package com.money.monocle.ui.presentation.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.money.monocle.data.Category
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.record.AddCategoryRepository
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.toStringIfMessageIsNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class AddCategoryViewModel @Inject constructor(
    private val addCategoryRepository: AddCategoryRepository,
    scopeProvider: CoroutineScopeProvider,
    savedStateHandle: SavedStateHandle,
    @Named("maxCustomCategoryNameLength")
    private val maxCustomCategoryNameLength: Int
): ViewModel() {
    private val scope = scopeProvider.provide() ?: viewModelScope
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    private val isExpense = checkNotNull(savedStateHandle["isExpense"]) as Boolean

    init {
        _uiState.update { it.copy(isExpense = isExpense) }
    }

    fun onCategoryAdd() {
        updateUploadResult(CustomResult.InProgress)
        scope.launch {
            try {
                addCategoryRepository.addCategory(
                    _uiState.value.selectedCategory,
                    _uiState.value.isExpense)
                updateUploadResult(CustomResult.Success)
            } catch (e: Exception) {
                updateUploadResult(CustomResult.DynamicError(e.toStringIfMessageIsNull()))
            }
        }
    }
    fun onCategoryChange(category: Category) =
        _uiState.update { it.copy(selectedCategory = category) }
    fun onNameChange(name: String) {
        if (name.length < maxCustomCategoryNameLength)
            _uiState.update { it.copy(selectedCategory = it.selectedCategory.copy(name = name)) }
    }
    private fun updateUploadResult(result: CustomResult) =
        _uiState.update { it.copy(uploadResult = result) }
    data class UiState(
        val isExpense: Boolean = false,
        val selectedCategory: Category = Category(),
        val uploadResult: CustomResult = CustomResult.Idle
    )
}