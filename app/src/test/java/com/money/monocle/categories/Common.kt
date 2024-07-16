package com.money.monocle.categories

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.data.Category
import com.money.monocle.data.FIRESTORE_EXPENSE_CATEGORIES
import com.money.monocle.data.FIRESTORE_INCOME_CATEGORIES
import com.money.monocle.data.defaultRawExpenseCategories
import com.money.monocle.data.defaultRawIncomeCategories
import com.money.monocle.mockTask
import com.money.monocle.userId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import java.util.UUID


// make names unique for testing
var customExpenseCategories = List(40) {
    val categoryName = defaultRawExpenseCategories.map { it.category }.random()
    Category(category = categoryName, name = "0/${categoryName.lowercase()}/$it", timestamp = Instant.now().toEpochMilli()+it)
}
var customIncomeCategories = List(40) {
    val categoryName = defaultRawIncomeCategories.map { it.category }.random()
    Category(category = categoryName, name = "1/${categoryName.lowercase()}/$it", timestamp = Instant.now().toEpochMilli()+it)
}

fun mockCategoriesFirestore(limit: Int,
                            empty: Boolean = false,
                            fetchException: Exception? = null,
                            deletionException: Exception? = null,
                            changeException: Exception? = null): FirebaseFirestore = mockk {
    for (type in listOf(FIRESTORE_EXPENSE_CATEGORIES, FIRESTORE_INCOME_CATEGORIES)) {
        val categories = if (type == FIRESTORE_EXPENSE_CATEGORIES) customExpenseCategories else customIncomeCategories
        val timestampSlot = slot<Long>()
        every { collection("data").document(userId).collection(type).orderBy("timestamp")
            .limit(limit.toLong()).get() } returns mockTask(mockk<QuerySnapshot> {
            every { documents } returns if (!empty) categories.chunked(limit).first().map {
                mockk<DocumentSnapshot> { every { toObject(Category::class.java) } returns it }
            } else listOf()
        }, exception = fetchException)
        every { collection("data").document(userId).collection(type).orderBy("timestamp")
            .startAfter(capture(timestampSlot))
            .limit(limit.toLong()).get() } answers {
            val timestamp = timestampSlot.captured
            val startIndex = categories.indexOfFirst { it.timestamp == timestamp } + 1
            val endIndex = minOf(startIndex + limit, categories.size)
            val returnedCategories = categories.slice(startIndex until endIndex).map {category ->
                val newId = UUID.randomUUID().toString()
                if (type == FIRESTORE_EXPENSE_CATEGORIES) customExpenseCategories = customExpenseCategories
                    .map { if (it.id == category.id) it.copy(id = newId) else it }
                else customIncomeCategories = customIncomeCategories
                    .map { if (it.id == category.id) it.copy(id = newId) else it }

                mockk<DocumentSnapshot> { every { toObject(Category::class.java) } returns category.copy(id = newId) }
            }
            mockTask(mockk<QuerySnapshot> {
                every { documents } returns if (!empty) returnedCategories else listOf()
            }, exception = fetchException)
        }
        every { collection("data").document(userId).collection(type)
            .document(any<String>()).delete() } returns mockTask(exception = deletionException)
        every { collection("data").document(userId).collection(type)
            .document(any<String>()).update("name", any<String>()) } returns mockTask(exception = changeException)
    }
}