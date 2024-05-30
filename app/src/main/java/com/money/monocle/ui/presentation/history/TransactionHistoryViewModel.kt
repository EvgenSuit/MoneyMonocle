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
import kotlinx.coroutines.flow.update
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

    init {
        repository.listen(
            onError = { updateResult(Result.Error(it)) },
            onRecords = {records ->
                _uiState.update { it.copy(records = records) }
                updateResult(Result.Success(""))
            }
        )
    }
    fun removeListener() = repository.removeListener()
    fun formatDate(timestamp: Long): String = dateFormatter(timestamp)
    private fun updateResult(result: Result) =
        _uiState.update { it.copy(result = result) }
    data class UiState(
        val records: List<Record> = listOf(),
        val result: Result = Result.InProgress
    )
}