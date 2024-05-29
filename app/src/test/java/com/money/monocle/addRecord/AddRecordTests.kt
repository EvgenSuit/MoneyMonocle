package com.money.monocle.addRecord

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.ibm.icu.text.SimpleDateFormat
import com.ibm.icu.util.TimeZone
import com.money.monocle.Record
import com.money.monocle.domain.Result
import com.money.monocle.domain.record.AddRecordRepository
import com.money.monocle.mockTask
import com.money.monocle.ui.presentation.AddRecordViewModel
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.userId
import io.mockk.core.ValueClassSupport.boxedValue
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

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
            every { collection(userId).document("records").collection("records").add(any<Record>()) } returns mockTask(exception = exception)
            every { collection(userId).document("balance").update("balance", capture(balanceSlot)) } returns mockTask(exception = exception)
        }
    }

    @Test
    fun addRecord_success() = runTest {
        val record = Record(true, 2, Instant.now().toEpochMilli(), 999f)
        val scopeProvider = CoroutineScopeProvider(this)
        val repository = AddRecordRepository(auth, firestore)
        val viewModel = AddRecordViewModel(repository, scopeProvider)
        viewModel.apply {
            onAmountChange(record.amount.toString())
            onDateChange(record.timestamp)
            onCategoryChange(record.category)
        }
        viewModel.addRecord(true)
        advanceUntilIdle()

        verify { firestore.collection(userId).document("records").collection("records").add(record) }
        verify { firestore.collection(userId).document("balance").update("balance", balanceSlot.captured) }
        assertTrue(viewModel.recordState.value.uploadResult is Result.Success)
    }
    @Test
    fun addRecord_failure() = runTest {
        mockAuth()
        mockFirestore(Exception("exception"))
        val record = Record(true, 2, Instant.now().toEpochMilli(), 999f)
        val scopeProvider = CoroutineScopeProvider(this)
        val repository = AddRecordRepository(auth, firestore)
        val viewModel = AddRecordViewModel(repository, scopeProvider)
        viewModel.apply {
            onAmountChange(record.amount.toString())
            onDateChange(record.timestamp)
            onCategoryChange(record.category)
        }
        viewModel.addRecord(true)
        advanceUntilIdle()

        verify { firestore.collection(userId).document("records").collection("records").add(record) }
        assertEquals(viewModel.recordState.value.uploadResult.error, "exception")
    }
}