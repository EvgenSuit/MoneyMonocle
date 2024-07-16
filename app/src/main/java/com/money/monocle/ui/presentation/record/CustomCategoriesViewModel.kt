package com.money.monocle.ui.presentation.record

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.money.monocle.data.Category
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.isEmpty
import com.money.monocle.domain.record.CustomCategoriesRepository
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.toStringIfMessageIsNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomCategoriesViewModel @Inject constructor(
    private val customCategoryRepository: CustomCategoriesRepository,
    scopeProvider: CoroutineScopeProvider,
): ViewModel() {
    private val scope = scopeProvider.provide() ?: viewModelScope
    private val _uiState = MutableStateFlow(CustomCategoriesUiState())
    val uiState = _uiState.asStateFlow()

    fun onCategoriesFetch(
        startAt: Int
    ) = scope.launch {
        if (!_uiState.value.isEndReached() && !_uiState.value.currentState().fetchResult.isEmpty()) {
            customCategoryRepository.fetch(
                startAt = startAt,
                type = _uiState.value.selectedType,
                lastCategory = _uiState.value.currentCategories().getOrNull(startAt),
                onCategories = {categories ->
                    if (_uiState.value.currentCategories().any { categories.contains(it) }) {
                        _uiState.updateIsEndReached()
                    } else {
                        updateCategories(categories, true)
                    }
                }
            ).collectLatest {res ->
                updateFetchResult(if (res is CustomResult.Success && _uiState.value.currentCategories().isEmpty()) CustomResult.Empty else res) }
        }
    }

    fun onCategoryDelete(category: Category) {
        updateDeletionResult(CustomResult.InProgress)
        scope.launch {
            try {
                customCategoryRepository.deleteCategory(_uiState.value.isIncome(), category.id)
                updateCategories(_uiState.value.currentCategories().filter { it.id != category.id }, false)
                updateDeletionResult(CustomResult.Success)

                if (_uiState.value.currentCategories().isEmpty()) updateFetchResult(CustomResult.Empty)
            } catch (e: Exception) {
                updateDeletionResult(CustomResult.DynamicError(e.toStringIfMessageIsNull()))
            }
        }
    }

    fun onCategoryNameChange(category: Category) {
        updateNameChangeResult(CustomResult.InProgress)
        scope.launch {
            try {
                customCategoryRepository.changeCategoryName(_uiState.value.isIncome(), category)
                updateCategories(_uiState.value.currentCategories().map { if (it.id == category.id) category else it }, false)
                updateNameChangeResult(CustomResult.Success)
            } catch (e: Exception) {
                updateNameChangeResult(CustomResult.DynamicError(e.toStringIfMessageIsNull()))
            }
        }
    }

    private fun updateCategories(categories: List<Category>, add: Boolean) =
        _uiState.update { it.copy(
            incomeCategoriesState = if (it.isIncome()) it.incomeCategoriesState.copy(categories = if (add) it.incomeCategoriesState.categories + categories
            else categories)
            else it.incomeCategoriesState,
            expenseCategoriesState = if (!it.isIncome()) it.expenseCategoriesState.copy(categories = if (add) it.expenseCategoriesState.categories + categories
            else categories)
            else it.expenseCategoriesState
        ) }

    fun onTypeChange() = _uiState.update { it.copy(selectedType = CategoryType.entries[1 xor it.selectedType.ordinal]) }

    private fun updateFetchResult(result: CustomResult) =
        _uiState.update { it.copy(
            incomeCategoriesState = if (it.isIncome()) it.incomeCategoriesState.copy(fetchResult = result)
            else it.incomeCategoriesState,
            expenseCategoriesState = if (!it.isIncome()) it.expenseCategoriesState.copy(fetchResult = result)
            else it.expenseCategoriesState
        ) }
    fun updateDeletionResult(result: CustomResult) =
        _uiState.update { it.copy(deletionResult = result) }
    fun updateNameChangeResult(result: CustomResult) =
        _uiState.update { it.copy(nameChangeResult = result) }
    fun onDispose() = customCategoryRepository.onDispose()
}

data class CustomCategoriesUiState(
    val selectedType: CategoryType = CategoryType.INCOME,
    val expenseCategoriesState: ExpenseCategoriesState = ExpenseCategoriesState(),
    val incomeCategoriesState: IncomeCategoriesState = IncomeCategoriesState(),
    val deletionResult: CustomResult = CustomResult.Idle,
    val nameChangeResult: CustomResult = CustomResult.Idle
)

fun CustomCategoriesUiState.currentCategories(): List<Category> =
    if (this.selectedType.isIncome()) this.incomeCategoriesState.categories
    else this.expenseCategoriesState.categories

fun CustomCategoriesUiState.currentState(): CategoryState =
    if (this.selectedType.isIncome()) this.incomeCategoriesState else this.expenseCategoriesState

fun CustomCategoriesUiState.isEndReached(): Boolean =
    if (this.selectedType.isIncome()) this.incomeCategoriesState.isEndReached
    else this.expenseCategoriesState.isEndReached

fun MutableStateFlow<CustomCategoriesUiState>.updateIsEndReached() {
    this.update { it.copy(
        incomeCategoriesState = if (it.isIncome()) it.incomeCategoriesState.copy(isEndReached = true)
        else it.incomeCategoriesState,
        expenseCategoriesState = if (!it.isIncome()) it.expenseCategoriesState.copy(isEndReached = true)
        else it.expenseCategoriesState
    ) }
}
fun CustomCategoriesUiState.isIncome(): Boolean =
    this.selectedType == CategoryType.INCOME

data class ExpenseCategoriesState(
    override val categories: List<Category> = listOf(),
    override val isEndReached: Boolean = false,
    override val fetchResult: CustomResult = CustomResult.Idle
): CategoryState()
data class IncomeCategoriesState(
    override val categories: List<Category> = listOf(),
    override val isEndReached: Boolean = false,
    override val fetchResult: CustomResult = CustomResult.Idle
): CategoryState()
open class CategoryState(
    open val categories: List<Category> = listOf(),
    open val isEndReached: Boolean = false,
    open val fetchResult: CustomResult = CustomResult.Idle
)
enum class CategoryType {
    INCOME,
    EXPENSE
}
fun CategoryType.isIncome(): Boolean = this == CategoryType.INCOME