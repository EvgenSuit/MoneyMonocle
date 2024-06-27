package com.money.monocle.record

import com.google.firebase.firestore.FieldValue
import com.money.monocle.BaseTestClass
import com.money.monocle.data.Category
import com.money.monocle.data.Record
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
    @Before
    fun init() {
        auth = mockAuth()
        firestore = mockRecordFirestore(balanceSlot)
    }

    @Test
    fun addRecord_success() = runTest {
        val timestamp = Instant.now().toEpochMilli()
        val categoryId = UUID.randomUUID().toString()
        val record = Record(
            isExpense = true,
            id = UUID.randomUUID().toString(),
            categoryId = categoryId,
            date = timestamp,
            timestamp = timestamp,
            amount = 999f)
        val scopeProvider = CoroutineScopeProvider(this)
        val repository = AddRecordRepository(auth, firestore)
        val viewModel = AddRecordViewModel(repository, currencyFormatValidator, scopeProvider,
            mockRecordSavedStateHandle(isExpense = record.isExpense))
        viewModel.apply {
            onAmountChange(record.amount.toString())
            onDateChange(record.date)
            onCategoryChange(Category(categoryId = categoryId))
        }
        viewModel.addRecord(timestamp, record.id)
        advanceUntilIdle()
        verify { firestore.collection("data").document(userId).collection("records").document(any())
            .set(record)}
        verify { firestore.collection("data").document(userId).collection("balance")
            .document("balance").update("balance", balanceSlot.captured) }
        assertTrue(viewModel.recordState.value.uploadResult is CustomResult.Success)
    }
    @Test
    fun addRecord_failure() = runTest {
        auth = mockAuth()
        firestore = mockRecordFirestore(balanceSlot, Exception("exception"))
        val timestamp = Instant.now().toEpochMilli()
        val categoryId = UUID.randomUUID().toString()
        val record = Record(
            isExpense = true,
            id = UUID.randomUUID().toString(),
            categoryId = categoryId,
            timestamp = timestamp,
            date = timestamp,
            amount = 999f)
        val scopeProvider = CoroutineScopeProvider(this)
        val repository = AddRecordRepository(auth, firestore)
        val viewModel = AddRecordViewModel(repository, currencyFormatValidator, scopeProvider,
            mockRecordSavedStateHandle(isExpense = record.isExpense))
        viewModel.apply {
            onAmountChange(record.amount.toString())
            onDateChange(record.date)
            onCategoryChange(Category(categoryId = categoryId))
        }
        viewModel.addRecord(timestamp, record.id)
        advanceUntilIdle()
        assertEquals(viewModel.recordState.value.uploadResult.error, StringValue.DynamicString("exception"))
    }
}