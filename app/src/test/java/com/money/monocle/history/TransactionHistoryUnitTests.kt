package com.money.monocle.history

import com.google.firebase.firestore.Query
import com.money.monocle.BaseTestClass
import com.money.monocle.data.DefaultExpenseCategoriesIds
import com.money.monocle.data.DefaultIncomeCategoriesIds
import com.money.monocle.data.Record
import com.money.monocle.data.firestoreExpenseCategories
import com.money.monocle.data.firestoreIncomeCategories
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.history.TransactionHistoryRepository
import com.money.monocle.domain.useCases.DateFormatter
import com.money.monocle.mockAuth
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.history.TransactionHistoryViewModel
import com.money.monocle.userId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionHistoryTests: BaseTestClass() {
    private val limit = 3
    private lateinit var viewModel: TransactionHistoryViewModel

    @Before
    fun init() {
        auth = mockAuth()
        createViewModel(records)
    }
    private fun createViewModel(records: List<Record>, empty: Boolean = false) {
        firestore = mockFirestore(limit, records, empty = empty)
        val repository = TransactionHistoryRepository(
            limit = limit,
            auth = auth, firestore = firestore.collection("data"))
        viewModel = TransactionHistoryViewModel(repository, DateFormatter(),
            CoroutineScopeProvider(testScope),
            mockk { every { get<String>("currency") } returns "$"})
    }
    @Test
    fun fetchRecords_success() = testScope.runTest {
        var startAt = 0
        while (startAt < records.size) {
            viewModel.fetchRecords(startAt)
            advanceUntilIdle()
            // without this correction the last record of the second new batch is equal to
            // to the first record of the 3rd batch, although in production there's no overlapping, this sucks
            startAt += if (startAt == 0) limit-1 else limit
        }
        for (type in listOf(firestoreExpenseCategories, firestoreIncomeCategories)) {
            val ref = firestore.collection("data").document(userId).collection(type)
            verify(atLeast = 2) { ref.orderBy("id") }
        }
        assertTrue(viewModel.uiState.value.fetchResult is CustomResult.Success)
    }

    @Test
    fun fetchRecords_assertOnlyOneCategoryFetch() = testScope.runTest {
        createViewModel(records.map { it.copy(categoryId = "same ${it.expense} id") })
        var startAt = 0
        while (startAt < records.size) {
            viewModel.fetchRecords(startAt)
            advanceUntilIdle()
            // without this correction the last record of the second new batch is equal to
            // to the first record of the 3rd batch, although in production there's no overlapping, this sucks
            startAt += if (startAt == 0) limit-1 else limit
        }
        for (type in listOf(firestoreExpenseCategories, firestoreIncomeCategories)) {
            val ref = firestore.collection("data").document(userId).collection(type)
            verify(exactly = 1) { ref.orderBy("id") }
        }
        assertTrue(viewModel.uiState.value.fetchResult is CustomResult.Success)
    }

    @Test
    fun fetchRecords_successNoRecords() = testScope.runTest {
        createViewModel(records, empty = true)
        var startAt = 0
        while (startAt < records.size) {
            viewModel.fetchRecords(startAt)
            advanceUntilIdle()
            // without this correction the last record of the second new batch is equal to
            // to the first record of the 3rd batch, although in production there's no overlapping, this sucks
            startAt += if (startAt == 0) limit-1 else limit
        }
        assertTrue(viewModel.uiState.value.fetchResult is CustomResult.Empty)
    }

    @Test
    fun deleteRecord_success() = testScope.runTest {
        viewModel.fetchRecords(0)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.records.contains(records[0]))
        viewModel.deleteRecord(records[0].id)
        advanceUntilIdle()
        assertTrue(!viewModel.uiState.value.records.contains(records[0]))
        verify { firestore.collection("data").document(userId).collection("balance")
            .document("balance").update("balance", any()) }
    }

    // test a case where the last record variable is null again after deleting all records
    @Test
    fun deleteRecord_successNewQueryNotCalled() = testScope.runTest {
        val ref = firestore.collection("data").document(userId).collection("records")
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(limit.toLong())
        viewModel.fetchRecords(0)
        advanceUntilIdle()
        viewModel.deleteRecord(records[0].id)
        advanceUntilIdle()
        assertTrue(!viewModel.uiState.value.records.contains(records[0]))
        verify(exactly = 1) { ref.get() }
    }
}