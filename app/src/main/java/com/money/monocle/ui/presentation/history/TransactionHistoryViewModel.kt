package com.money.monocle.ui.presentation.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.money.monocle.data.AccountName
import com.money.monocle.data.Category
import com.money.monocle.data.Record
import com.money.monocle.data.simpleCurrencyMapper
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.history.TransactionHistoryRepository
import com.money.monocle.domain.isEmpty
import com.money.monocle.domain.isSuccess
import com.money.monocle.domain.useCases.DateFormatter
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val repository: TransactionHistoryRepository,
    private val dateFormatter: DateFormatter,
    private val dataStoreManager: DataStoreManager,
    scopeProvider: CoroutineScopeProvider
): ViewModel() {
    private val scope = scopeProvider.provide() ?: viewModelScope
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
       scope.launch {
            dataStoreManager.accountFlow().collectLatest { accountId ->
                repository.onDispose()
                repository.currentAccountId.value = accountId

                withContext(Dispatchers.Main) {
                    _uiState.update { UiState(currency = simpleCurrencyMapper(dataStoreManager.balanceFlow().first().currency)) }
                    fetchRecords(0)
                }
            }
        }
    }

    suspend fun fetchRecords(startAt: Int) {
        val records = _uiState.value.records
        // fetch only if the end was reached or if the previous result of fetching records is not empty
        // (to basically avoid making queries on an empty collection)
        // and if the end was reached, since making limit queries that go beyond collection size makes firebase
        // return values from the very beginning of the collection
        if (!_uiState.value.isEndReached && !_uiState.value.fetchResult.isEmpty()) {
            repository.fetchRecords(
                startAt = startAt,
                customCategories = _uiState.value.customCategories,
                lastRecord = records.getOrNull(startAt),
                onCustomCategories = { newCategories ->
                    _uiState.update { it.copy(customCategories = it.customCategories + newCategories) }
                },
                onRecords = { newRecords ->
                    if (_uiState.value.records.any { newRecords.contains(it) }) {
                        _uiState.update { it.copy(isEndReached = true) }
                    } else {
                        _uiState.update { it.copy(records = it.records + newRecords) }
                    }
                }
            ).collectLatest { res ->
                updateFetchResult(if (res.isSuccess() && _uiState.value.records.isEmpty()) CustomResult.Empty else res)
            }
        }
    }
    fun deleteRecord(id: String) {
        updateDeleteResult(CustomResult.InProgress)
        scope.launch {
            repository.deleteRecord(_uiState.value.records.first { it.id == id }).collectLatest { res ->
                updateDeleteResult(res)
                if (res.isSuccess()) _uiState.update { it.copy(records = it.records.filter { it.id != id }) }
                if (_uiState.value.records.isEmpty()) updateFetchResult(CustomResult.Empty)
            }
        }
    }
    fun onDispose() = repository.onDispose()
    fun formatDate(timestamp: Long): String = dateFormatter(timestamp)
    fun updateDeleteResult(result: CustomResult) =
        _uiState.update { it.copy(deleteResult = result) }
    private fun updateFetchResult(result: CustomResult) =
        _uiState.update { it.copy(fetchResult = result) }
    data class UiState(
        val records: List<Record> = listOf(),
        val customCategories: List<Category> = listOf(),
        val isEndReached: Boolean = false,
        val currency: String = "",
        val deleteResult: CustomResult = CustomResult.Idle,
        val fetchResult: CustomResult = CustomResult.Idle
    )
}