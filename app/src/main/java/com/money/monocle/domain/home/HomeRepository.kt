package com.money.monocle.domain.home

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.money.monocle.data.Balance

enum class AccountState {
    SIGNED_OUT,
    DELETED,
    NEW,
    USED,
    NONE
}

typealias CurrentBalance = Float
typealias CurrencyFirebase = Int

class HomeRepository(
    private val authRef: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val auth = authRef
    private lateinit var listenerRegistration: ListenerRegistration

    fun listenForBalance(
        onAccountState: (AccountState) -> Unit,
        onCurrentBalance: (CurrentBalance, CurrencyFirebase) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        listenerRegistration = firestore.collection("data").document(authRef.currentUser!!.uid).collection("balance")
            .addSnapshotListener { snapshot, e ->
            try {
                if (e != null && auth.currentUser != null) onError(e)
                val balance = if (snapshot?.documents?.isEmpty() == true) Balance()
                else snapshot?.documents?.map { it.toObject(Balance::class.java) }?.first()


                if (e == null) onAccountState(
                    if (auth.currentUser == null) AccountState.SIGNED_OUT
                    else if (snapshot == null || snapshot.isEmpty) AccountState.DELETED
                    else if (balance!!.currency == -1) AccountState.NEW
                    else AccountState.USED
                )
                if (e == null && snapshot != null && !snapshot.isEmpty) {
                    onCurrentBalance(balance!!.balance, balance.currency)
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
    fun removeListener() = listenerRegistration.remove()
    fun signOut() = auth.signOut()
}