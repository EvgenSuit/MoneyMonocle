package com.money.monocle.domain.history

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import com.money.monocle.data.Record

/**
 * @param firestore specifies a firestore.collection("data") path
 */
class TransactionHistoryRepository(
    private val auth: FirebaseAuth,
    private val firestore: CollectionReference
) {
    private lateinit var listener: ListenerRegistration
    fun listen(
        onError: (String) -> Unit,
        onRecords: (List<Record>) -> Unit,
    ) {
        listener = firestore.document(auth.currentUser!!.uid).collection("records")
            .addSnapshotListener { snapshot, e ->
            if (e != null && auth.currentUser != null) {
                onError(e.message!!)
                return@addSnapshotListener
            }
            if (snapshot?.isEmpty == false && snapshot.documents.isNotEmpty()) {
                val records = snapshot.documents.map { it.toObject(Record::class.java)!! }
                onRecords(records)
            }
        }
    }
    fun removeListener() = listener.remove()
}