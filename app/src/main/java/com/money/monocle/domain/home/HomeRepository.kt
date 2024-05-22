package com.money.monocle.domain.home

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestoreException

class HomeRepository(
    private val authRef: FirebaseAuth,
    private val usersRef: CollectionReference
) {
    fun listen(
        onDoesUserExist: (Boolean) -> Unit,
        onError: (FirebaseFirestoreException) -> Unit,
    ) {
        usersRef.document(authRef.currentUser!!.uid).addSnapshotListener {snapshot, e ->
            onDoesUserExist(e != null)
            if (e != null && snapshot != null && snapshot.exists()) {

            }
        }
    }
}