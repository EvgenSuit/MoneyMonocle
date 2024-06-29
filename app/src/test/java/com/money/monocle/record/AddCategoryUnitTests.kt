package com.money.monocle.record

import com.money.monocle.BaseTestClass
import com.money.monocle.data.Category
import com.money.monocle.data.CustomIncomeCategoriesIds
import com.money.monocle.data.Record
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.record.AddCategoryRepository
import com.money.monocle.mockAuth
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.record.AddCategoryViewModel
import com.money.monocle.userId
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddCategoryUnitTests: BaseTestClass() {
    private lateinit var viewModel: AddCategoryViewModel
    @Before
    fun setup() {
        auth = mockAuth()
        firestore = mockCategoryFirestore()
        createViewModel()

    }

    private fun createViewModel(isExpense: Boolean = Record().expense) {
        val repository = AddCategoryRepository(auth, firestore.collection("data"))
        viewModel = AddCategoryViewModel(repository,
            CoroutineScopeProvider(testScope),
            mockCategorySavedStateHandle(isExpense), 30)
    }

    @Test
    fun addIncomeCategory_success() = testScope.runTest {
        val name = "my category"
        viewModel.apply {
            onCategoryChange(Category(category = CustomIncomeCategoriesIds.TIP.name))
            onNameChange(name)
        }
        viewModel.onCategoryAdd()
        advanceUntilIdle()
        verify { firestore.collection("data").document(userId).collection("customIncomeCategories") }
        assertEquals(CustomResult.Success, viewModel.uiState.value.uploadResult)
    }
    @Test
    fun addExpenseCategory_success() = testScope.runTest {
        createViewModel(true)
        val name = "my category"
        viewModel.apply {
            onCategoryChange(Category(category = CustomIncomeCategoriesIds.TIP.name))
            onNameChange(name)
        }
        viewModel.onCategoryAdd()
        advanceUntilIdle()
        verify { firestore.collection("data").document(userId).collection("customExpenseCategories") }
        assertEquals(CustomResult.Success, viewModel.uiState.value.uploadResult)
    }
}