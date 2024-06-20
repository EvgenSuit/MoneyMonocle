package com.money.monocle.domain.settings

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.data.LastTimeUpdated
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.auth.CustomAuthStateListener
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.network.FrankfurterApi
import com.money.monocle.ui.presentation.toStringIfMessageIsNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.time.Instant

class SettingsRepository(
    private val auth: FirebaseAuth,
    private val firestore: CollectionReference,
    private val frankfurterApi: FrankfurterApi,
    private val dataStoreManager: DataStoreManager
) {
    private var lastTimeUpdatedListener: ListenerRegistration? = null
    fun listenForLastTimeUpdated(
        onData: (Long?) -> Unit,
        onError: (String) -> Unit,
    ) {
        val uid = auth.currentUser?.uid ?: return
        lastTimeUpdatedListener?.remove()
        lastTimeUpdatedListener = firestore.document(uid).collection("balance")
            .document("lastTimeUpdated").addSnapshotListener {snapshot, e ->
                if (auth.currentUser != null) {
                    if (e != null) onError(e.toStringIfMessageIsNull())
                    else {
                        try {
                            onData(snapshot?.toObject(LastTimeUpdated::class.java)?.lastTimeUpdated)
                        } catch (e: Exception) {
                            onError(e.toStringIfMessageIsNull())
                        }
                    }
                }
            }
    }
    suspend fun changeLastTimeUpdated(lastTimeUpdated: Long?)  {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            firestore.document(uid).collection("balance")
                .document("lastTimeUpdated").set(LastTimeUpdated(lastTimeUpdated)).await()
        }
    }
    suspend fun changeTheme(isDark: Boolean) = dataStoreManager.changeTheme(isDark)
    fun balanceFlow() = dataStoreManager.balanceFlow()
    suspend fun changeCurrency(currentBalance: Balance, newCurrencyEnum: CurrencyEnum) = flow {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            emit(CustomResult.InProgress)
            val convertedMainBalance = frankfurterApi.convert(
                    amount = currentBalance.balance,
                    from = CurrencyEnum.entries[currentBalance.currency].name,
                    to = newCurrencyEnum.name)
            firestore.document(uid).collection("balance").document("balance")
                .set(Balance(newCurrencyEnum.ordinal, convertedMainBalance.rates[newCurrencyEnum.name]!!)).await()
            changeLastTimeUpdated(Instant.now().toEpochMilli())
            emit(CustomResult.Success)
        }
    }
    fun themeFlow(): Flow<Boolean> = dataStoreManager.themeFlow()
    fun signOut() = auth.signOut()
}