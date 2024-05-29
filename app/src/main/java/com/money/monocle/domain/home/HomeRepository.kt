package com.money.monocle.domain.home

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.getField
import com.money.monocle.data.Balance
import com.money.monocle.data.Currency

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
    fun listenForBalance(
        onAccountState: (AccountState) -> Unit,
        onCurrentBalance: (CurrentBalance, CurrencyFirebase) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        firestore.collection(authRef.currentUser!!.uid).document("balance")
            .addSnapshotListener { snapshot, e ->
            //try {
                if (e != null && auth.currentUser != null) onError(e)
                else onAccountState(
                    if (auth.currentUser == null) AccountState.SIGNED_OUT
                    else if (snapshot == null || !snapshot.exists()) AccountState.DELETED
                    else if (snapshot.toObject(Currency::class.java)?.currency == -1) AccountState.NEW
                    else AccountState.USED
                )
                if (e == null && snapshot != null && snapshot.exists()) {
                    onCurrentBalance(snapshot.toObject(Balance::class.java)?.balance ?: 0f,
                        snapshot.toObject(Currency::class.java)?.currency ?: 0)
                }
            /*} catch (e: Exception) {
                onError(e)
            }*/
        }
    }
    fun signOut() = auth.signOut()
}