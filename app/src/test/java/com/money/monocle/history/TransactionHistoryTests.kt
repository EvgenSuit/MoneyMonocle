package com.money.monocle.history

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.data.Record
import com.money.monocle.domain.DateFormatter
import com.money.monocle.domain.Result
import com.money.monocle.domain.history.TransactionHistoryRepository
import com.money.monocle.mockAuth
import com.money.monocle.mockTask
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.history.TransactionHistoryViewModel
import com.money.monocle.userId
import io.mockk.every
import io.mockk.mockk
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
class TransactionHistoryTests {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val limit = 3

    @Before
    fun init() {
        auth = mockAuth()
        firestore = mockFirestore(limit)
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
        assertTrue(viewModel.uiState.value.result is Result.Success)
    }

}