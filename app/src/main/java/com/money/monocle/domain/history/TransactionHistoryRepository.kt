package com.money.monocle.domain.history

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.money.monocle.data.Record
import com.money.monocle.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * @param firestore specifies a firestore.collection("data") path
 */
class TransactionHistoryRepository(
    private val limit: Int = 10,
    auth: FirebaseAuth,
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
                val batch = if (startAt == 0) query.limit(limit.toLong())
                else query.startAfter(lastRecord?.timestamp).limit(limit.toLong())
                emit(Result.InProgress)
                val records = batch.get().await().documents.map { it.toObject(Record::class.java)!! }
                emit(Result.Success(""))
                onRecords(records)
                // increment last item index by batch size
                nextStartAt += limit-1
            } catch (e: Exception) {
                emit(Result.Error(e.message ?: e.toString()))
            }
        }
    }
    fun onDispose() {
        nextStartAt = 0
    }
}