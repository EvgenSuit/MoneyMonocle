package com.money.monocle.record

import androidx.lifecycle.SavedStateHandle
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.data.Category
import com.money.monocle.data.Record
import com.money.monocle.mockTask
import com.money.monocle.userId
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

val customExpenseCategories = List(10) {
    Category()
}
val customIncomeCategories = List(10) {
    Category()
}

fun mockRecordFirestore(balanceSlot: CapturingSlot<FieldValue>? = null,
                        limit: Int,
                        exception: Exception? = null): FirebaseFirestore = mockk {
    every { collection("data").document(userId).collection("records")
        .document(any<String>()).set(any<Record>()) } returns mockTask(
        exception = exception
    )
    every { collection("data").document(userId).collection("balance")
        .document("balance").update("balance", if (balanceSlot != null) capture(balanceSlot) else any()) } returns mockTask(
        exception = exception
    )
    for (type in listOf("customExpenseCategories", "customIncomeCategories")) {
        val categories = if (type == "customExpenseCategories") customExpenseCategories else customIncomeCategories
        val idSlot = slot<String>()
        every { collection("data").document(userId).collection(type).orderBy("timestamp")
            .limit(limit.toLong()).get() } returns mockTask(mockk<QuerySnapshot> {
            every { documents } returns categories.slice(0 until limit).map {
                mockk<DocumentSnapshot> { every { toObject(Category::class.java) } returns it }
            }
        }, exception = exception)
        every { collection("data").document(userId).collection(type).orderBy("timestamp")
            .startAfter(capture(idSlot))
            .limit(limit.toLong()).get() } answers {
                val id = idSlot.captured
            val startIndex = categories.indexOfFirst { it.id == id } + 1
            val endIndex = minOf(startIndex + limit, categories.size)
            mockTask(mockk<QuerySnapshot> {
                every { documents } returns categories.slice(startIndex until endIndex).map {
                    mockk<DocumentSnapshot> { every { toObject(Category::class.java) } returns it }
                }
            }, exception = exception)
        }
    }
}

fun mockCategoryFirestore(exception: Exception? = null): FirebaseFirestore = mockk {
    every { collection("data").document(userId).collection(any())
        .document(any()).set(any()) } returns mockTask(exception = exception)
}

fun mockRecordSavedStateHandle(currency: String = "$", isExpense: Boolean = Record().expense): SavedStateHandle = mockk{
    every { get<String>("currency") } returns currency
    every { get<Boolean>("isExpense")} returns isExpense
}


fun mockCategorySavedStateHandle(isExpense: Boolean = Record().expense): SavedStateHandle = mockk {
    every { get<Boolean>("isExpense") } returns isExpense
}
