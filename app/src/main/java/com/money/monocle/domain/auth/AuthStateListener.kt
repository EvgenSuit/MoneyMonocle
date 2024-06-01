package com.money.monocle.domain.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow


class CustomAuthStateListener(private val auth: FirebaseAuth) {
    val userRef = auth.currentUser
    fun isUserNullFlow() = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener {
            trySend(it.currentUser == null)
        }
        auth.addAuthStateListener(listener)
        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }
}