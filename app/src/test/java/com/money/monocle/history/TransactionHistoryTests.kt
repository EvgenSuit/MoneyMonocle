package com.money.monocle.history

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.data.Record
import com.money.monocle.domain.DateFormatter
import com.money.monocle.domain.history.TransactionHistoryRepository
import com.money.monocle.mockAuth
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.history.TransactionHistoryViewModel
import com.money.monocle.userId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionHistoryTests {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val snapshotListener = slot<EventListener<QuerySnapshot>>()

    @Before
    fun init() {
        auth = mockAuth()
        mockFirestore()
    }

    private fun mockFirestore() {
        firestore = mockk {
            every { collection("data").document(userId).collection("records")
                .addSnapshotListener(capture(snapshotListener))} returns mockk<ListenerRegistration>()
        }
    }

    @Test
    fun listenForRecords_success() = runTest {
        val records = listOf(
            Record(expense = true,
                category = 2,
                timestamp = Instant.now().toEpochMilli(),
                amount = 12f)
        )
        val docs = records.map { mockk<DocumentSnapshot> {
            every { toObject(Record::class.java) } returns it
        } }
        val snapshot = mockk<QuerySnapshot> {
            every { isEmpty } returns false
            every { documents } returns docs
        }
        val repository = TransactionHistoryRepository(auth, firestore.collection("data"))
        val viewModel = TransactionHistoryViewModel(repository, DateFormatter(),
            CoroutineScopeProvider(this))
        snapshotListener.captured.onEvent(snapshot, null)
        advanceUntilIdle()
        assertEquals(viewModel.uiState.value.records, records)
    }

}