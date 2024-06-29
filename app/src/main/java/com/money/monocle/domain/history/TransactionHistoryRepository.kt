package com.money.monocle.domain.history

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.money.monocle.data.Category
import com.money.monocle.data.DefaultExpenseCategoriesIds
import com.money.monocle.data.DefaultIncomeCategoriesIds
import com.money.monocle.data.Record
import com.money.monocle.data.firestoreExpenseCategories
import com.money.monocle.data.firestoreIncomeCategories
import com.money.monocle.domain.CustomResult
import com.money.monocle.ui.presentation.toStringIfMessageIsNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * @param firestore specifies a firestore.collection("data") path
 */
class TransactionHistoryRepository(
    private val limit: Int,
    private val auth: FirebaseAuth,
    private val firestore: CollectionReference) {
    private val query = firestore.document(auth.currentUser!!.uid).collection("records")
        .orderBy("timestamp", Query.Direction.DESCENDING)
    private var nextStartAt = 0

    suspend fun fetchRecords(startAt: Int,
                             lastRecord: Record?,
                             customCategories: List<Category>,
                             onCustomCategories: (List<Category>) -> Unit,
                             onRecords: (List<Record>) -> Unit) = flow {
        // if last visible item index is bigger than the previously saved max index, load an additional batch of records
        if (startAt >= nextStartAt) {
            try {
                emit(CustomResult.InProgress)
                // increment immediately to account for a case when a user
                // scrolls up and the last item becomes invisible before the fetch has been completed, and then scrolls to the bottom again.
                // this avoids making multiple requests when the fetch has not yet been completed
                nextStartAt += limit-1
                val batch = if (lastRecord == null) query.limit(limit.toLong())
                else query.startAfter(lastRecord.timestamp).limit(limit.toLong())
                val records = batch.get().await().documents.map { it.toObject(Record::class.java)!! }
                onCustomCategories(loadCategories(records, customCategories))
                onRecords(records)
                emit(CustomResult.Success)
            } catch (e: Exception) {
                nextStartAt -= limit-1
                emit(CustomResult.DynamicError(e.toStringIfMessageIsNull()))
            }
        }
    }
    private suspend fun loadCategories(records: List<Record>,
        customCategories: List<Category>): List<Category> {
        var categories by mutableStateOf(customCategories)
        for (record in records) {
            val isDefaultCategories = DefaultIncomeCategoriesIds.entries + DefaultExpenseCategoriesIds.entries
            if (!categories.any { it.id == record.categoryId } && !isDefaultCategories.any { it.name == record.category }) {
                val query = firestore.document(auth.currentUser!!.uid).collection(if (record.expense) firestoreExpenseCategories
                else firestoreIncomeCategories).orderBy("id").whereEqualTo("id", record.categoryId)
                val newCategories = query.get().await().documents.map { it.toObject(Category::class.java)!! }
                categories += newCategories
            }
        }
        return categories
    }
    suspend fun deleteRecord(record: Record)  {
        firestore.document(auth.currentUser!!.uid).collection("records").document(record.timestamp.toString()).delete().await()
        firestore.document(auth.currentUser!!.uid).collection("balance").document("balance")
            .update("balance", FieldValue.increment((if (record.expense) +record.amount else -record.amount).toDouble())).await()
    }
    fun onDispose() {
        nextStartAt = 0
    }
}