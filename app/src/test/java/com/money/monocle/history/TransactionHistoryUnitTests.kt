package com.money.monocle.history

import com.google.firebase.firestore.Query
import com.money.monocle.BaseTestClass
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
        firestore = mockFirestore(limit, records)
    }
    private fun createViewModel() {
        val repository = TransactionHistoryRepository(
            limit = limit,
            auth = auth, firestore = firestore.collection("data"))
        viewModel = TransactionHistoryViewModel(repository, DateFormatter(),
            CoroutineScopeProvider(testScope),
            mockk { every { get<String>("currency") } returns "$"})
    }
    @Test
    fun fetchRecords_success() = testScope.runTest {
        createViewModel()
        var startAt = 0
        while (startAt < records.size) {
            viewModel.fetchRecords(startAt)
            advanceUntilIdle()
            // without this correction the last record of the second new batch is equal to
            // to the first record of the 3rd batch, although in production there's no overlapping, this sucks
            startAt += if (startAt == 0) limit-1 else limit
        }
        assertTrue(viewModel.uiState.value.fetchResult is CustomResult.Success)
    }
    @Test
    fun fetchRecords_successNoRecords() = testScope.runTest {
        firestore = mockFirestore(limit, records, empty = true)
        createViewModel()
        var startAt = 0
        while (startAt < records.size) {
            viewModel.fetchRecords(startAt)
            advanceUntilIdle()
            // without this correction the last record of the second new batch is equal to
            // to the first record of the 3rd batch, although in production there's no overlapping, this sucks
            startAt += if (startAt == 0) limit-1 else limit
        }
        println(viewModel.uiState.value.fetchResult)
        assertTrue(viewModel.uiState.value.fetchResult is CustomResult.Empty)
    }

    @Test
    fun deleteRecord_success() = testScope.runTest {
        createViewModel()
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
        createViewModel()
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