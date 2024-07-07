package com.money.monocle.history

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.data.Category
import com.money.monocle.data.CustomExpenseCategoriesIds
import com.money.monocle.data.CustomIncomeCategoriesIds
import com.money.monocle.data.Record
import com.money.monocle.mockAuth
import com.money.monocle.mockTask
import com.money.monocle.userId
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.UUID

var records = List(17) {
    val isExpense = it % 2 == 0
    val categoriesIds = if (isExpense) CustomExpenseCategoriesIds.entries else CustomIncomeCategoriesIds.entries
    Record(
        id = UUID.randomUUID().toString(),
        expense = isExpense,
        categoryId = UUID.randomUUID().toString(),
        category = categoriesIds.random().name,
        timestamp = it.toLong()+1,
        amount = it.toFloat()+1)
}

fun mockAuthForAuthentication(userProfileChangeRequest: CapturingSlot<UserProfileChangeRequest>? = null): FirebaseAuth {
    val auth = mockAuth()
    val user = auth.currentUser
    every { user?.updateProfile(if (userProfileChangeRequest != null) capture(userProfileChangeRequest) else any()) } returns mockTask()
    return auth
}



fun mockFirestore(limit: Int,
                  inputRecords: List<Record>,
                  empty: Boolean = false,
                  exception: Exception? = null): FirebaseFirestore {
    val timestampSlot = slot<Long>()

    return mockk {
        every {
            collection("data").document(userId).collection("records")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong()).get()
        } returns mockTask(mockk<QuerySnapshot> {
            every { documents } returns if (!empty) inputRecords.slice(0 until limit).map {
                mockk<DocumentSnapshot> { every { toObject(Record::class.java) } returns it }
            } else listOf()
        }, exception)

        every {
            collection("data").document(userId).collection("records")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(capture(timestampSlot))
                .limit(limit.toLong()).get()
        } answers {
            val startAfterTimestamp = timestampSlot.captured
            val startIndex = inputRecords.indexOfFirst { it.timestamp == startAfterTimestamp } + 1
            val endIndex = minOf(startIndex + limit, inputRecords.size)
            val returnedRecords = inputRecords.slice(startIndex until endIndex).map { record ->
                val newId = UUID.randomUUID().toString()
                records = inputRecords.map { if (it.id == record.id) it.copy(id = newId) else it }
                mockk<DocumentSnapshot> {
                    every { toObject(Record::class.java) } returns record.copy(id = newId)
                }
            }
            mockTask(mockk<QuerySnapshot> {
                every { documents } returns if (!empty) returnedRecords else listOf()
            }, exception)
        }
        every { collection("data").document(userId).collection("records")
            .document(any<String>()).delete() } returns mockTask(exception = exception)
        every { collection("data").document(userId).collection("balance")
            .document("balance").update("balance", any()) } returns mockTask(
            exception = exception
        )
        for (record in inputRecords) {
            every { collection("data").document(userId).collection(if (record.expense) "customExpenseCategories"
            else "customIncomeCategories").orderBy("id").whereEqualTo("id", record.categoryId)
                .get()} returns mockTask(mockk<QuerySnapshot> {
                every { documents } returns if (!empty) listOf(
                    mockk<DocumentSnapshot> {
                        every { toObject(Category::class.java) } returns Category(id = record.categoryId!!,
                            category = record.category, name = record.category.lowercase())
                    }
                ) else listOf()
            }, exception = exception)
        }
    }
}
