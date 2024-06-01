package com.money.monocle.domain.history

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.money.monocle.data.Record
import com.money.monocle.domain.Result
import kotlinx.coroutines.flow.Flow
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
        .orderBy("timestamp")
    private var nextStartAt = 0

    suspend fun fetchRecords(startAt: Int,
                             lastRecord: Record?,
                             onRecords: (List<Record>) -> Unit): Flow<Result> = flow {
        // if last visible item index is bigger than the previously saved max index, load an additional batch of records
        if (startAt >= nextStartAt) {
            try {
                emit(Result.InProgress)
                val batch = if (lastRecord == null) query.limit(limit.toLong())
                else query.startAfter(lastRecord.timestamp).limit(limit.toLong())
                val records = batch.get().await().documents.map { it.toObject(Record::class.java)!! }
                onRecords(records)
                emit(Result.Success(""))
                // increment last item index by batch size
                nextStartAt += limit-1
            } catch (e: Exception) {
                emit(Result.Error(e.message ?: e.toString()))
            }
        }
    }
    suspend fun deleteRecord(record: Record): Flow<Result> = flow {
        try {
            emit(Result.InProgress)
            firestore.document(auth.currentUser!!.uid).collection("records").document(record.id).delete().await()
            firestore.document(auth.currentUser!!.uid).collection("balance").document("balance")
                .update("balance", FieldValue.increment((if (record.expense) +record.amount else -record.amount).toDouble())).await()
            emit(Result.Success(""))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: e.toString()))
        }
    }
    fun onDispose() {
        nextStartAt = 0
    }
}