package com.money.monocle.domain.record

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.money.monocle.data.Category
import com.money.monocle.data.FIRESTORE_EXPENSE_CATEGORIES
import com.money.monocle.data.FIRESTORE_INCOME_CATEGORIES
import com.money.monocle.domain.CustomResult
import com.money.monocle.ui.presentation.record.CategoryType
import com.money.monocle.ui.presentation.record.isIncome
import com.money.monocle.ui.presentation.toStringIfMessageIsNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class CustomCategoriesRepository(
    private val limit: Int = 3,
    auth: FirebaseAuth,
    firestore: CollectionReference
) {
    private val userRef = firestore.document(auth.currentUser!!.uid)
    private var incomeCategoriesNextStartAt = 0
    private var expenseCategoriesNextStartAt = 0

    fun fetch(
        startAt: Int,
        type: CategoryType,
        lastCategory: Category?,
        onCategories: (List<Category>) -> Unit
    ) = flow {
        if (startAt >= if (type.isIncome()) incomeCategoriesNextStartAt else expenseCategoriesNextStartAt) {
            emit(CustomResult.InProgress)
            if (type.isIncome()) incomeCategoriesNextStartAt += limit-1
            else expenseCategoriesNextStartAt += limit-1
            try {
                val ref = userRef.collection(if (type.isIncome()) FIRESTORE_INCOME_CATEGORIES
                else FIRESTORE_EXPENSE_CATEGORIES).orderBy("timestamp")

                val batch = if (lastCategory == null) ref.limit(limit.toLong())
                else ref.startAfter(lastCategory.timestamp).limit(limit.toLong())
                val categories =
                    batch.get().await().documents.mapNotNull { it.toObject(Category::class.java) }
                onCategories(categories)
                emit(CustomResult.Success)
            } catch (e: Exception) {
                if (type.isIncome()) incomeCategoriesNextStartAt -= limit-1
                else expenseCategoriesNextStartAt -= limit-1
                emit(CustomResult.DynamicError(e.toStringIfMessageIsNull()))
            }
        }
    }

    suspend fun deleteCategory(
        isIncome: Boolean,
        id: String) {
        userRef.collection(if (isIncome) FIRESTORE_INCOME_CATEGORIES
        else FIRESTORE_EXPENSE_CATEGORIES).document(id).delete().await()
        if (isIncome) incomeCategoriesNextStartAt -= 1
        else expenseCategoriesNextStartAt -= 1
    }

    suspend fun changeCategoryName(isIncome: Boolean, category: Category) {
        userRef.collection(if (isIncome) FIRESTORE_INCOME_CATEGORIES
        else FIRESTORE_EXPENSE_CATEGORIES).document(category.id).update("name", category.name).await()
    }
    fun onDispose() {
        incomeCategoriesNextStartAt = 0
        expenseCategoriesNextStartAt = 0
    }
}