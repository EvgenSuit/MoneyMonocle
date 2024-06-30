package com.money.monocle.record

import com.google.firebase.firestore.FieldValue
import com.money.monocle.BaseTestClass
import com.money.monocle.data.Category
import com.money.monocle.data.CustomRawExpenseCategories
import com.money.monocle.data.DefaultExpenseCategoriesIds
import com.money.monocle.data.DefaultIncomeCategoriesIds
import com.money.monocle.data.Record
import com.money.monocle.data.defaultRawExpenseCategories
import com.money.monocle.data.defaultRawIncomeCategories
import com.money.monocle.data.firestoreExpenseCategories
import com.money.monocle.data.firestoreIncomeCategories
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.record.AddRecordRepository
import com.money.monocle.domain.useCases.CurrencyFormatValidator
import com.money.monocle.mockAuth
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.StringValue
import com.money.monocle.ui.presentation.record.AddRecordViewModel
import com.money.monocle.userId
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class AddRecordTests: BaseTestClass() {
    private val balanceSlot = slot<FieldValue>()
    private val currencyFormatValidator = CurrencyFormatValidator(6)
    private lateinit var viewModel: AddRecordViewModel
    private val limit = 3
    @Before
    fun init() {
        auth = mockAuth()
        firestore = mockRecordFirestore(balanceSlot, limit)
        createViewModel()
    }

    private fun createViewModel(isExpense: Boolean = Record().expense) {
        val scopeProvider = CoroutineScopeProvider(testScope)
        val repository = AddRecordRepository(limit, auth, firestore.collection("data"))
        viewModel = AddRecordViewModel(repository, currencyFormatValidator, scopeProvider,
            mockRecordSavedStateHandle(isExpense = isExpense))
    }

    @Test
    fun fetchCustomIncomeCategories_success() = testScope.runTest {
        viewModel.onCustomCategoriesFetch(id = defaultRawIncomeCategories.last().id)
        advanceUntilIdle()
        assertEquals(
            customIncomeCategories.slice(0 until limit),
            viewModel.recordState.value.customCategories)
        for (i in 0..1) {
            viewModel.onCustomCategoriesFetch(id = viewModel.recordState.value.customCategories.last().id)
            advanceUntilIdle()
        }
        assertEquals(CustomResult.Success, viewModel.recordState.value.customCategoriesFetchResult)
        assertEquals(9, viewModel.recordState.value.customCategories.size)
    }
    @Test
    fun fetchCustomExpenseCategories_success() = testScope.runTest {
        createViewModel(true)
        viewModel.onCustomCategoriesFetch(id = defaultRawExpenseCategories.last().id)
        advanceUntilIdle()
        assertEquals(
            customExpenseCategories.slice(0 until limit),
            viewModel.recordState.value.customCategories)
        for (i in 0..1) {
            viewModel.onCustomCategoriesFetch(id = viewModel.recordState.value.customCategories.last().id)
            advanceUntilIdle()
        }

        assertEquals(CustomResult.Success, viewModel.recordState.value.customCategoriesFetchResult)
        assertEquals(9, viewModel.recordState.value.customCategories.size)
    }
    @Test
    fun addRecord_success() = testScope.runTest {
        val timestamp = Instant.now().toEpochMilli()
        val categoryId = UUID.randomUUID().toString()
        val category = DefaultExpenseCategoriesIds.INSURANCE.name
        val record = Record(
            expense = true,
            id = UUID.randomUUID().toString(),
            category = category,
            categoryId = categoryId,
            date = timestamp,
            timestamp = timestamp,
            amount = 999f)
        createViewModel(record.expense)
        viewModel.apply {
            onAmountChange(record.amount.toString())
            onDateChange(record.date)
            onCategoryChange(Category(id = categoryId, category = category))
            addRecord(timestamp, record.id)
        }
        advanceUntilIdle()
        assertTrue(viewModel.recordState.value.uploadResult is CustomResult.Success)
        // no idea why verify doesn't without ref1
        val ref1 = firestore.collection("data").document(userId).collection("records")
            .document(record.timestamp.toString())
        verify { ref1.set(record)}
        verify { firestore.collection("data").document(userId).collection("balance")
            .document("balance").update("balance", balanceSlot.captured) }
    }

    @Test
    fun addRecord_failure() = testScope.runTest {
        firestore = mockRecordFirestore(balanceSlot, limit, Exception("exception"))
        createViewModel(true)
        val timestamp = Instant.now().toEpochMilli()
        val categoryId = UUID.randomUUID().toString()
        val category = DefaultIncomeCategoriesIds.WAGE.name
        val record = Record(
            expense = true,
            id = UUID.randomUUID().toString(),
            category = category,
            categoryId = categoryId,
            timestamp = timestamp,
            date = timestamp,
            amount = 999f)
        createViewModel(record.expense)
        viewModel.apply {
            onAmountChange(record.amount.toString())
            onDateChange(record.date)
            onCategoryChange(Category(category = category, id = categoryId))
            addRecord(timestamp, record.id)
        }
        advanceUntilIdle()
        assertEquals(viewModel.recordState.value.uploadResult.error, StringValue.DynamicString("exception"))
    }

}