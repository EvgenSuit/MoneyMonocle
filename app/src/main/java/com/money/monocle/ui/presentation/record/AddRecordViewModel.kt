package com.money.monocle.ui.presentation.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.money.monocle.data.Category
import com.money.monocle.data.RawCategory
import com.money.monocle.data.Record
import com.money.monocle.data.defaultRawExpenseCategories
import com.money.monocle.data.defaultRawIncomeCategories
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.record.AddRecordRepository
import com.money.monocle.domain.useCases.CurrencyFormatValidator
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddRecordViewModel @Inject constructor(
    private val addRecordRepository: AddRecordRepository,
    private val currencyFormatValidator: CurrencyFormatValidator,
    coroutineScopeProvider: CoroutineScopeProvider,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val scope = coroutineScopeProvider.provide() ?: viewModelScope
    private val _recordState = MutableStateFlow(RecordState())
    private val currency = checkNotNull(savedStateHandle["currency"]) as String
    private val isExpense = checkNotNull(savedStateHandle["isExpense"]) as Boolean
    private val defaultCategories = if (isExpense) defaultRawExpenseCategories else defaultRawIncomeCategories
    val recordState = _recordState.asStateFlow()

    init {
        _recordState.update { it.copy(isExpense = isExpense, currency = currency) }
    }
    fun onDispose() = addRecordRepository.onDispose()

    fun onCustomCategoriesFetch(id: String) = scope.launch {
        val customCategories = _recordState.value.customCategories
        val category = defaultCategories.getOrNull(defaultCategories.indexOfFirst { it.id == id }) ?:
        customCategories.firstOrNull { it.id == id }
        if (category == null) return@launch
        if (category is RawCategory && defaultCategories.last().category != category.category
            && customCategories.isEmpty()) return@launch
        val startAt = if (category is RawCategory && defaultCategories.last().category == category.category) 0
        else customCategories.indexOf(category)
        addRecordRepository.fetchCustomCategories(
            startAt = startAt,
            isExpense = isExpense,
            lastCategory = customCategories.getOrNull(startAt),
            onCategories = {newCategories ->
                if (!_recordState.value.customCategories.any { newCategories.contains(it) }) {
                    _recordState.update { it.copy(customCategories = it.customCategories + newCategories) }
                }
            }
        ).collectLatest { updateCustomCategoriesFetchResult(it) }
    }
    fun addRecord(timestamp: Long = Instant.now().toEpochMilli(),
                  recordId: String = UUID.randomUUID().toString()) {
        updateUploadResult(CustomResult.InProgress)
        scope.launch {
            val currentState = _recordState.value
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
            _recordState.update { it.copy(amount = validatedAmount) }
        }
    }
    fun onCategoryChange(category: Category) =
        _recordState.update { it.copy(selectedCategory = category) }
    fun onDateChange(timestamp: Long) {
        _recordState.update { it.copy(selectedDate = timestamp) }
    }
    private fun updateUploadResult(result: CustomResult) =
        _recordState.update { it.copy(uploadResult = result) }
    private fun updateCustomCategoriesFetchResult(result: CustomResult) =
        _recordState.update { it.copy(customCategoriesFetchResult = result) }

    data class RecordState(
        val selectedCategory: Category = Category(),
        val isExpense: Boolean = false,
        val currency: String = "",
        val selectedDate: Long = Instant.now().toEpochMilli(),
        val amount: String? = null,
        val customCategories: List<Category> = listOf(),
        val customCategoriesFetchResult: CustomResult = CustomResult.Idle,
        val uploadResult: CustomResult = CustomResult.Idle
    )
}