package com.money.monocle.record

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.money.monocle.data.Record
import com.money.monocle.domain.Result
import com.money.monocle.domain.record.AddRecordRepository
import com.money.monocle.mockTask
import com.money.monocle.ui.presentation.record.AddRecordViewModel
import com.money.monocle.ui.presentation.CoroutineScopeProvider
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
class AddRecordTests {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val balanceSlot = slot<FieldValue>()

    @Before
    fun init() {
        mockAuth()
        mockFirestore()
    }
    private fun mockAuth() {
        auth = mockk {
            every { currentUser?.uid } returns userId
        }
    }

    private fun mockFirestore(exception: Exception? = null) {
        firestore = mockk {
            every { collection("data").document(userId).collection("records")
                .document(any<String>()).set(any<Record>()) } returns mockTask(exception = exception)
            every { collection("data").document(userId).collection("balance")
                .document("balance").update("balance", capture(balanceSlot)) } returns mockTask(exception = exception)
        }
    }

    @Test
    fun addRecord_success() = runTest {
        val timestamp = Instant.now().toEpochMilli()
        val record = Record(true, 2, timestamp, timestamp, 999f)
        val scopeProvider = CoroutineScopeProvider(this)
        val repository = AddRecordRepository(auth, firestore)
        val viewModel = AddRecordViewModel(repository, scopeProvider)
        viewModel.apply {
            onAmountChange(record.amount.toString())
            onDateChange(record.date)
            onCategoryChange(record.category)
        }
        viewModel.addRecord(true, timestamp)
        advanceUntilIdle()
        verify { firestore.collection("data").document(userId).collection("records").document(any())
            .set(record)}
        verify { firestore.collection("data").document(userId).collection("balance")
            .document("balance").update("balance", balanceSlot.captured) }
        assertTrue(viewModel.recordState.value.uploadResult is Result.Success)
    }
    @Test
    fun addRecord_failure() = runTest {
        mockAuth()
        mockFirestore(Exception("exception"))
        val timestamp = Instant.now().toEpochMilli()
        val record = Record(
            true, 2, timestamp, timestamp,
            999f)
        val scopeProvider = CoroutineScopeProvider(this)
        val repository = AddRecordRepository(auth, firestore)
        val viewModel = AddRecordViewModel(repository, scopeProvider)
        viewModel.apply {
            onAmountChange(record.amount.toString())
            onDateChange(record.date)
            onCategoryChange(record.category)
        }
        viewModel.addRecord(true)
        advanceUntilIdle()
        assertEquals(viewModel.recordState.value.uploadResult.error, "exception")
    }
}