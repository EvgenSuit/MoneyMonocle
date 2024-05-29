package com.money.monocle.ui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.money.monocle.Record
import com.money.monocle.domain.Result
import com.money.monocle.domain.record.AddRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class AddRecordViewModel @Inject constructor(
    private val addRecordRepository: AddRecordRepository,
    coroutineScopeProvider: CoroutineScopeProvider
): ViewModel() {
    private val scope = coroutineScopeProvider.provide() ?: viewModelScope
    private val _recordState = MutableStateFlow(RecordState())
    val recordState = _recordState.asStateFlow()

    fun addRecord(isExpense: Boolean) = scope.launch {
        val currentState = _recordState.value
        val record = Record(isExpense = isExpense, category = currentState.selectedCategory,
            timestamp = currentState.selectedDate,
            amount = currentState.amount!!.toFloat())
        try {
            updateUploadResult(Result.InProgress)
            addRecordRepository.addRecord(record)
            updateUploadResult(Result.Success(""))
        } catch (e: Exception) {
            updateUploadResult(Result.Error(e.message ?: e.toString()))
        }
    }

    fun onAmountChange(amount: String) {
        _recordState.update { it.copy(amount = if (amount.length > 6) amount.substring(0, 6) else amount) }
    }
    fun onCategoryChange(category: Int) =
        _recordState.update { it.copy(selectedCategory = category) }
    fun onDateChange(timestamp: Long) {
        _recordState.update { it.copy(selectedDate = timestamp) }
    }
    private fun updateUploadResult(result: Result) =
        _recordState.update { it.copy(uploadResult = result) }

    data class RecordState(
        val selectedCategory: Int = -1,
        val selectedDate: Long = Instant.now().toEpochMilli(),
        val amount: String? = null,
        val uploadResult: Result = Result.Idle
    )
}