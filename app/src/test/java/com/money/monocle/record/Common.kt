package com.money.monocle.record

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.money.monocle.R
import com.money.monocle.data.Record
import com.money.monocle.mockTask
import com.money.monocle.userId
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk

fun mockRecordFirestore(balanceSlot: CapturingSlot<FieldValue>? = null,
                        exception: Exception? = null): FirebaseFirestore = mockk {
    every { collection("data").document(userId).collection("records")
        .document(any<String>()).set(any<Record>()) } returns mockTask(
        exception = exception
    )
    every { collection("data").document(userId).collection("balance")
        .document("balance").update("balance", if (balanceSlot != null) capture(balanceSlot) else any()) } returns mockTask(
        exception = exception
    )
}

fun mockCategoryFirestore(exception: Exception? = null): FirebaseFirestore = mockk {
    every { collection("data").document(userId).collection(any())
        .document(any()).set(any()) } returns mockTask(exception = exception)
}

fun mockRecordSavedStateHandle(currency: String = "$", isExpense: Boolean = Record().isExpense): SavedStateHandle = mockk{
    every { get<String>("currency") } returns currency
    every { get<Boolean>("isExpense")} returns isExpense
}


fun mockCategorySavedStateHandle(isExpense: Boolean = Record().isExpense): SavedStateHandle = mockk {
    every { get<Boolean>("isExpense") } returns isExpense
}
