package com.money.monocle.ui.presentation.history

import android.util.Log
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
    val resultFlow = _uiState.map { it.result }

    fun fetchRecords(startAt: Int) = scope.launch {
        val records = _uiState.value.records
        if (!_uiState.value.isEndReached) {
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
            ).collectLatest { updateResult(it) }
        }
    }
    fun onDispose() = repository.onDispose()
    fun formatDate(timestamp: Long): String = dateFormatter(timestamp)
    private fun updateResult(result: Result) =
        _uiState.update { it.copy(result = result) }
    data class UiState(
        val records: List<Record> = listOf(),
        val isEndReached: Boolean = false,
        val result: Result = Result.InProgress
    )
}