package com.money.monocle.domain.home

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.money.monocle.data.Balance
import com.money.monocle.domain.datastore.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Instant

enum class AccountState {
    SIGNED_OUT,
    DELETED,
    NEW,
    USED,
    NONE
}

typealias CurrentBalance = Float
typealias CurrencyFirebase = Int
typealias TotalSpent = Float
typealias TotalEarned = Float

class HomeRepository(
    authRef: FirebaseAuth,
    firestore: CollectionReference,
    private val dataStoreManager: DataStoreManager
) {
    val auth = authRef
    private lateinit var balanceListener: ListenerRegistration
    private lateinit var pieChartListener: ListenerRegistration
    private val userRef = firestore.document(authRef.currentUser!!.uid)
    fun listenForBalance(
        scope: CoroutineScope,
        onAccountState: (AccountState) -> Unit,
        onCurrentBalance: (CurrentBalance, CurrencyFirebase) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        balanceListener = userRef.collection("balance").addSnapshotListener { snapshot, e ->
            try {
                if (e != null && auth.currentUser != null) onError(e)
                val balance = if (snapshot?.documents?.isEmpty() == true) null
                else snapshot?.documents?.map { it.toObject(Balance::class.java) }?.first()
                val state =  if (auth.currentUser == null) AccountState.SIGNED_OUT
                else if (snapshot == null || snapshot.isEmpty) AccountState.DELETED
                else if (balance!!.currency == -1) AccountState.NEW
                else AccountState.USED
                scope.launch {
                    if (e == null && snapshot != null && !snapshot.isEmpty && balance != null) {
                        dataStoreManager.setBalance(balance)
                        onCurrentBalance(balance.balance, balance.currency)
                        dataStoreManager.changeAccountState(true)
                    }
                    if (e == null) {
                        // use runBlocking in order for nav bar to show correctly
                        // and in order to block user interaction on sign out or account deletion
                        runBlocking {
                            if (state == AccountState.SIGNED_OUT || state == AccountState.DELETED) {
                                removeListeners()
                                dataStoreManager.changeAccountState(false)
                                if (state == AccountState.DELETED) {
                                    auth.signOut()
                                }
                            } else dataStoreManager.isWelcomeScreenShown(state == AccountState.NEW)
                        }
                    }
                    onAccountState(state)
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
    fun listenForStats(
        onError: (Exception) -> Unit,
        onPieChartData: (TotalSpent, TotalEarned) -> Unit
    ) {
        val fiveDaysAgo = Instant.now().toEpochMilli() - (5*24*60*60*1000)
        pieChartListener = userRef.collection("records").whereGreaterThan("timestamp", fiveDaysAgo).addSnapshotListener { snapshot, e ->
            try {
                if (e != null && auth.currentUser != null) onError(e)
                if (snapshot?.isEmpty == false && snapshot.documents.isNotEmpty()) {
                    val totalSpent = snapshot.documents.filter { it.getBoolean("expense") == true }
                        .sumOf { it.getDouble("amount") ?: 0.0 }.toFloat()
                    val totalEarned = snapshot.documents.filter { it.getBoolean("expense") == false }
                        .sumOf { it.getDouble("amount") ?: 0.0 }.toFloat()
                    onPieChartData(totalSpent, totalEarned)
                } else onPieChartData(0f, 0f)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
    private fun removeListeners() {
        balanceListener.remove()
        pieChartListener.remove()
    }
}