package com.money.monocle.ui.presentation.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.money.monocle.data.Record
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.record.AddRecordRepository
import com.money.monocle.domain.useCases.CurrencyFormatValidator
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.toStringIfMessageIsNull
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
    private val currencyFormatValidator: CurrencyFormatValidator,
    coroutineScopeProvider: CoroutineScopeProvider,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val scope = coroutineScopeProvider.provide() ?: viewModelScope
    private val _recordState = MutableStateFlow(RecordState())
    private val currency = checkNotNull(savedStateHandle["currency"]) as String
    private val isExpense = checkNotNull(savedStateHandle["isExpense"]) as Boolean
    val recordState = _recordState.asStateFlow()

    init {
        _recordState.update { it.copy(isExpense = isExpense, currency = currency) }
    }

    fun addRecord(timestamp: Long = Instant.now().toEpochMilli()) = scope.launch {
        val currentState = _recordState.value
        val record = Record(
            expense = isExpense,
            timestamp = timestamp,
            category = currentState.selectedCategory,
            date = currentState.selectedDate,
            amount = currentState.amount!!.toFloat())
        try {
            updateUploadResult(CustomResult.InProgress)
            addRecordRepository.addRecord(record)
            updateUploadResult(CustomResult.Success)
        } catch (e: Exception) {
            updateUploadResult(CustomResult.DynamicError(e.toStringIfMessageIsNull()))
        }
    }

    fun onAmountChange(amount: String) {
        currencyFormatValidator(amount) {validatedAmount ->
            _recordState.update { it.copy(amount = validatedAmount) }
        }
    }
    fun onCategoryChange(category: Int) =
        _recordState.update { it.copy(selectedCategory = category) }
    fun onDateChange(timestamp: Long) {
        _recordState.update { it.copy(selectedDate = timestamp) }
    }
    private fun updateUploadResult(result: CustomResult) =
        _recordState.update { it.copy(uploadResult = result) }

    data class RecordState(
        val selectedCategory: Int = -1,
        val isExpense: Boolean = false,
        val currency: String = "",
        val selectedDate: Long = Instant.now().toEpochMilli(),
        val amount: String? = null,
        val uploadResult: CustomResult = CustomResult.Idle
    )
}