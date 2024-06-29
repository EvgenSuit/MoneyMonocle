package com.money.monocle.domain.record

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.money.monocle.data.Category
import com.money.monocle.data.FirestoreCategory
import kotlinx.coroutines.tasks.await
import java.time.Instant

class AddCategoryRepository(
    auth: FirebaseAuth,
    firestore: CollectionReference
) {
    private val userRef = firestore.document(auth.currentUser!!.uid)
    suspend fun addCategory(category: Category, isExpense: Boolean) {
        userRef.collection(if (isExpense) "customExpenseCategories" else "customIncomeCategories")
            .document(category.id).set(FirestoreCategory(
            id = category.id,
            category = category.category,
            name = category.name,
            timestamp = Instant.now().toEpochMilli()
        )).await()
    }
}