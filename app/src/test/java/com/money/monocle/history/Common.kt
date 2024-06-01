package com.money.monocle.history

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.data.Record
import com.money.monocle.mockTask
import com.money.monocle.userId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

val records = List(17) {
    Record(
        id = "$it",
        expense = true,
        category = 2,
        timestamp = it.toLong(),
        amount = it.toFloat())
}

fun mockFirestore(limit: Int,
                  records: List<Record>,
                  empty: Boolean = false,
                  exception: Exception? = null): FirebaseFirestore {
    val timestampSlot = slot<Long>()
    return mockk {
        every {
            collection("data").document(userId).collection("records")
                .orderBy("timestamp")
                .limit(limit.toLong()).get()
        } returns mockTask(mockk<QuerySnapshot> {
            every { documents } returns if (!empty) records.slice(0 until limit).map {
                mockk<DocumentSnapshot> { every { toObject(Record::class.java) } returns it }
            } else listOf()
        }, exception)

        every {
            collection("data").document(userId).collection("records")
                .orderBy("timestamp")
                .startAfter(capture(timestampSlot))
                .limit(limit.toLong()).get()
        } answers {
            val startAfterTimestamp = timestampSlot.captured
            val startIndex = records.indexOfFirst { it.timestamp == startAfterTimestamp } + 1
            val endIndex = minOf(startIndex + limit, records.size)

            mockTask(mockk<QuerySnapshot> {
                every { documents } returns if (!empty) records.slice(startIndex until endIndex).map {
                    mockk<DocumentSnapshot> { every { toObject(Record::class.java) } returns it }
                } else listOf()
            }, exception)
        }
        every { collection("data").document(userId).collection("records")
            .document(any<String>()).delete() } returns mockTask(exception = exception)
        every { collection("data").document(userId).collection("balance")
            .document("balance").update("balance", any()) } returns mockTask(exception = exception)
    }
}