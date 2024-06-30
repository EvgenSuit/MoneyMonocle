package com.money.monocle.record

import androidx.lifecycle.SavedStateHandle
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.data.Category
import com.money.monocle.data.Record
import com.money.monocle.data.defaultRawExpenseCategories
import com.money.monocle.data.defaultRawIncomeCategories
import com.money.monocle.mockTask
import com.money.monocle.userId
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant

val customExpenseCategories = List(10) {
    Category(category = defaultRawExpenseCategories.map { it.category }.random() ,timestamp = Instant.now().toEpochMilli()+it)
}
val customIncomeCategories = List(10) {
    Category(category = defaultRawIncomeCategories.map { it.category }.random(), timestamp = Instant.now().toEpochMilli()+it)
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
        val timestampSlot = slot<Long>()
        every { collection("data").document(userId).collection(type).orderBy("timestamp")
            .limit(limit.toLong()).get() } returns mockTask(mockk<QuerySnapshot> {
            every { documents } returns categories.slice(0 until limit).map {
                mockk<DocumentSnapshot> { every { toObject(Category::class.java) } returns it }
            }
        }, exception = exception)
        every { collection("data").document(userId).collection(type).orderBy("timestamp")
            .startAfter(capture(timestampSlot))
            .limit(limit.toLong()).get() } answers {
                val timestamp = timestampSlot.captured
            val startIndex = categories.indexOfFirst { it.timestamp == timestamp } + 1
            val endIndex = minOf(startIndex + limit, categories.size)
            mockTask(mockk<QuerySnapshot> {
                every { documents } returns categories.slice(startIndex until endIndex).map {
                    mockk<DocumentSnapshot> { every { toObject(Category::class.java) } returns it }
                }
            }, exception = exception)
        }
        every { collection("data").document(userId).collection(type)
            .whereEqualTo("id", any()).get() } returns mockTask(
                mockk<QuerySnapshot> {
                    every { isEmpty } returns false
                }
            )
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
