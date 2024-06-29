package com.money.monocle.domain.record

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.money.monocle.R
import com.money.monocle.data.Category
import com.money.monocle.data.Record
import com.money.monocle.data.defaultRawExpenseCategories
import com.money.monocle.data.defaultRawIncomeCategories
import com.money.monocle.domain.CustomResult
import com.money.monocle.ui.presentation.toStringIfMessageIsNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class AddRecordRepository(
    private val limit: Int,
    private val auth: FirebaseAuth,
    private val firestore: CollectionReference) {
    private var nextStartAt = 0
    suspend fun fetchCustomCategories(
        startAt: Int,
        isExpense: Boolean,
        lastCategory: Category?,
        onCategories: (List<Category>) -> Unit) = flow {
        val query = firestore.document(auth.currentUser!!.uid).collection(if (isExpense) "customExpenseCategories"
        else "customIncomeCategories").orderBy("timestamp")
        if (startAt >= nextStartAt) {
            emit(CustomResult.InProgress)
            nextStartAt += limit-1
            try {
                val batch = if (lastCategory == null) query.limit(limit.toLong())
                else query.startAfter(lastCategory.timestamp).limit(limit.toLong())
                val categories = batch.get().await().documents.map { it.toObject(Category::class.java)!! }
                onCategories(categories)
                emit(CustomResult.Success)
            } catch (e: Exception) {
                nextStartAt -= limit-1
                emit(CustomResult.DynamicError(e.toStringIfMessageIsNull()))
            }
        }
    }

    suspend fun addRecord(record: Record,
                          selectedCategoryId: String) = flow {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                val defaultRawCategories = if (record.expense) defaultRawExpenseCategories else defaultRawIncomeCategories
                val query = firestore.document(auth.currentUser!!.uid).collection(if (record.expense) "customExpenseCategories"
                else "customIncomeCategories")
                // check if current category is not default. if it is, return null
                val categoryId = if (!defaultRawCategories.map { it.categoryId }.contains(record.category)) selectedCategoryId else null
                if (categoryId != null && query.whereEqualTo("id", selectedCategoryId).get().await().isEmpty) {
                    emit(CustomResult.ResourceError(R.string.category_doesnt_exist))
                    // return if category doesn't exist
                    return@flow
                }
                val userRef = firestore.document(uid)
                userRef.collection("records").document(record.timestamp.toString())
                    .set(record.copy(categoryId = selectedCategoryId)).await()
                userRef.collection("balance").document("balance").update("balance",
                    FieldValue.increment((if (record.expense) -record.amount else record.amount).toDouble())).await()
                emit(CustomResult.Success)
            } catch (e: Exception) {
                emit(CustomResult.DynamicError(e.toStringIfMessageIsNull()))
            }
        }
    }
    fun onDispose() {
        nextStartAt = 0
    }
}