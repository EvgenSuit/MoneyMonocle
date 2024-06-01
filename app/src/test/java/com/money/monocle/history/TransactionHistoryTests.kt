package com.money.monocle.history

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.money.monocle.domain.DateFormatter
import com.money.monocle.domain.Result
import com.money.monocle.domain.history.TransactionHistoryRepository
import com.money.monocle.mockAuth
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.history.TransactionHistoryViewModel
import com.money.monocle.userId
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionHistoryTests {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val limit = 3

    @Before
    fun init() {
        auth = mockAuth()
        firestore = mockFirestore(limit, records)
    }

    @Test
    fun fetchRecords_success() = runTest {
        val repository = TransactionHistoryRepository(limit = limit, auth = auth, firestore = firestore.collection("data"))
        val viewModel = TransactionHistoryViewModel(repository, DateFormatter(),
            CoroutineScopeProvider(this))
        var startAt = 0
        while (startAt < records.size) {
            viewModel.fetchRecords(startAt)
            advanceUntilIdle()
            // without this correction the last record of the second new batch is equal to
            // to the first record of the 3rd batch, although in production there's no overlapping, this sucks
            startAt += if (startAt == 0) limit-1 else limit
        }
        assertTrue(viewModel.uiState.value.fetchResult is Result.Success)
    }
    @Test
    fun fetchRecords_successNoRecords() = runTest {
        firestore = mockFirestore(limit, records, empty = true)
        val repository = TransactionHistoryRepository(limit = limit, auth = auth, firestore = firestore.collection("data"))
        val viewModel = TransactionHistoryViewModel(repository, DateFormatter(),
            CoroutineScopeProvider(this))
        var startAt = 0
        while (startAt < records.size) {
            viewModel.fetchRecords(startAt)
            advanceUntilIdle()
            // without this correction the last record of the second new batch is equal to
            // to the first record of the 3rd batch, although in production there's no overlapping, this sucks
            startAt += if (startAt == 0) limit-1 else limit
        }
        assertTrue(viewModel.uiState.value.fetchResult is Result.Empty)
    }

    @Test
    fun deleteRecord_success() = runTest {
        val repository = TransactionHistoryRepository(limit = limit, auth = auth, firestore = firestore.collection("data"))
        val viewModel = TransactionHistoryViewModel(repository, DateFormatter(),
            CoroutineScopeProvider(this))
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
    fun deleteRecord_successNewQueryNotCalled() = runTest {
        val repository = TransactionHistoryRepository(limit = limit, auth = auth, firestore = firestore.collection("data"))
        val viewModel = TransactionHistoryViewModel(repository, DateFormatter(),
            CoroutineScopeProvider(this))
        val ref = firestore.collection("data").document(userId).collection("records").orderBy("timestamp")
            .limit(limit.toLong())
        viewModel.fetchRecords(0)
        advanceUntilIdle()
        viewModel.deleteRecord(records[0].id)
        advanceUntilIdle()
        assertTrue(!viewModel.uiState.value.records.contains(records[0]))
        verify(exactly = 1) { ref.get() }
    }
}