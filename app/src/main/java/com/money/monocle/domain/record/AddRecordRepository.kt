package com.money.monocle.domain.record

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.money.monocle.data.Record
import kotlinx.coroutines.tasks.await

class AddRecordRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore) {
    suspend fun addRecord(record: Record) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            val userRef = firestore.collection("data").document(uid)
            userRef.collection("records").add(record).await()
            userRef.collection("balance").document("balance").update("balance",
                FieldValue.increment((if (record.isExpense) -record.amount else record.amount).toDouble())).await()
        }
    }
}