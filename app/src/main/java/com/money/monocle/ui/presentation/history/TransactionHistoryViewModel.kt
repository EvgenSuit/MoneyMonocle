package com.money.monocle.ui.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.money.monocle.data.Record
import com.money.monocle.domain.DateFormatter
import com.money.monocle.domain.Result
import com.money.monocle.domain.history.TransactionHistoryRepository
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val repository: TransactionHistoryRepository,
    private val dateFormatter: DateFormatter,
    scopeProvider: CoroutineScopeProvider
): ViewModel() {
    private val scope = scopeProvider.provide() ?: viewModelScope
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    val fetchResultFlow = _uiState.map { it.fetchResult }
    val deleteResultFlow = _uiState.map { it.deleteResult }

    fun fetchRecords(startAt: Int) = scope.launch {
        val records = _uiState.value.records
        // fetch only if the end was reached or if the previous result of fetching records is not empty
        // (to basically avoid making queries on an empty collection)
        if (!_uiState.value.isEndReached && _uiState.value.fetchResult !is Result.Empty) {
            repository.fetchRecords(
                startAt = startAt,
                lastRecord = records.getOrNull(startAt),
                onRecords = { newRecords ->
                    if (_uiState.value.records.any { newRecords.contains(it) }) {
                        _uiState.update { it.copy(isEndReached = true) }
                    } else {
                        _uiState.update { it.copy(records = it.records + newRecords) }
                    }
                }
            ).collectLatest { res ->
                updateFetchResult(if (res is Result.Success && _uiState.value.records.isEmpty()) Result.Empty else res)
            }
        }
    }
    fun deleteRecord(timestamp: Long) = scope.launch {
        repository.deleteRecord(_uiState.value.records.first { it.timestamp == timestamp }).collectLatest {res ->
            if (res is Result.Success) {
                _uiState.update { it.copy(records = it.records.filter { it.timestamp != timestamp }) }
            }
            updateDeleteResult(res)
            if (_uiState.value.records.isEmpty()) updateFetchResult(Result.Empty)
        }
    }
    fun onDispose() = repository.onDispose()
    fun formatDate(timestamp: Long): String = dateFormatter(timestamp)
    private fun updateDeleteResult(result: Result) =
        _uiState.update { it.copy(deleteResult = result) }
    private fun updateFetchResult(result: Result) =
        _uiState.update { it.copy(fetchResult = result) }
    data class UiState(
        val records: List<Record> = listOf(),
        val isEndReached: Boolean = false,
        val deleteResult: Result = Result.Idle,
        val fetchResult: Result = Result.Idle
    )
}