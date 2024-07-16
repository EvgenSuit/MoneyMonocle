package com.money.monocle.ui.presentation.record

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.money.monocle.data.Category
import com.money.monocle.data.RawCategory
import com.money.monocle.data.Record
import com.money.monocle.data.defaultRawExpenseCategories
import com.money.monocle.data.defaultRawIncomeCategories
import com.money.monocle.data.simpleCurrencyMapper
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.isEmpty
import com.money.monocle.domain.record.AddRecordRepository
import com.money.monocle.domain.useCases.CurrencyFormatValidator
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddRecordViewModel @Inject constructor(
    private val addRecordRepository: AddRecordRepository,
    private val currencyFormatValidator: CurrencyFormatValidator,
    private val dataStoreManager: DataStoreManager,
    coroutineScopeProvider: CoroutineScopeProvider,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val scope = coroutineScopeProvider.provide() ?: viewModelScope
    private val _uiState = MutableStateFlow(RecordState())
    private val isExpense = checkNotNull(savedStateHandle["isExpense"]) as Boolean
    private val defaultCategories = if (isExpense) defaultRawExpenseCategories else defaultRawIncomeCategories
    val recordState = _uiState.asStateFlow()
    private var currencyListenerJob: Job? = null

    init {
        scope.launch {
            dataStoreManager.accountFlow().collectLatest { accountId ->
                currencyListenerJob?.cancel()
                currencyListenerJob?.join()
                onDispose()
                addRecordRepository.currentAccountId = accountId
                withContext(Dispatchers.Main) {
                    _uiState.update { RecordState(
                        currency = simpleCurrencyMapper(dataStoreManager.balanceFlow().first().currency),
                        isExpense = isExpense) }
                }
            }
        }
        _uiState.update { it.copy(isExpense = isExpense) }
    }
    fun onDispose() = addRecordRepository.onDispose()

    fun onCustomCategoriesFetch(id: String) {
        currencyListenerJob = scope.launch {
            // this collect latest job will emit state only if currency is not empty
            _uiState.filter { it.currency.isNotEmpty() }.collectLatest { state ->
                if (state.isEndReached || state.customCategoriesFetchResult.isEmpty()) return@collectLatest
                val customCategories = state.customCategories
                val category = defaultCategories.getOrNull(defaultCategories.indexOfFirst { it.id == id }) ?:
                customCategories.firstOrNull { it.id == id }
                if (category == null) return@collectLatest
                if (category is RawCategory && defaultCategories.last().category != category.category
                    && customCategories.isEmpty()) return@collectLatest
                val startAt = if (category is RawCategory && defaultCategories.last().category == category.category) 0
                else customCategories.indexOf(category)
                addRecordRepository.fetchCustomCategories(
                    startAt = startAt,
                    isExpense = isExpense,
                    lastCategory = customCategories.getOrNull(startAt),
                    onCategories = {newCategories ->
                        if (!state.customCategories.any { newCategories.contains(it) }) {
                            _uiState.update { it.copy(customCategories = it.customCategories + newCategories) }
                        } else {
                            _uiState.update { it.copy(isEndReached = true) }
                        }
                    }
                ).collectLatest { updateCustomCategoriesFetchResult(it) }
            }
        }
    }
    fun addRecord(timestamp: Long = Instant.now().toEpochMilli(),
                  recordId: String = UUID.randomUUID().toString()) {
        updateUploadResult(CustomResult.InProgress)
        scope.launch {
            val currentState = _uiState.value
            val record = Record(
                expense = isExpense,
                id = recordId,
                timestamp = timestamp,
                category = currentState.selectedCategory.category,
                date = currentState.selectedDate,
                amount = currentState.amount!!.toFloat())
            addRecordRepository.addRecord(record,
                selectedCategoryId = currentState.selectedCategory.id).collect {res ->
                updateUploadResult(res)
            }
        }
    }

    fun onAmountChange(amount: String) {
        currencyFormatValidator(amount) {validatedAmount ->
            _uiState.update { it.copy(amount = validatedAmount) }
        }
    }
    fun onCategoryChange(category: Category) =
        _uiState.update { it.copy(selectedCategory = category) }
    fun onDateChange(timestamp: Long) {
        _uiState.update { it.copy(selectedDate = timestamp) }
    }
    private fun updateUploadResult(result: CustomResult) =
        _uiState.update { it.copy(uploadResult = result) }
    private fun updateCustomCategoriesFetchResult(result: CustomResult) =
        _uiState.update { it.copy(customCategoriesFetchResult = result) }

    data class RecordState(
        val selectedCategory: Category = Category(),
        val isExpense: Boolean = false,
        val currency: String = "",
        val selectedDate: Long = Instant.now().toEpochMilli(),
        val amount: String? = null,
        val customCategories: List<Category> = listOf(),
        val isEndReached: Boolean = false,
        val customCategoriesFetchResult: CustomResult = CustomResult.Idle,
        val uploadResult: CustomResult = CustomResult.Idle
    )
}