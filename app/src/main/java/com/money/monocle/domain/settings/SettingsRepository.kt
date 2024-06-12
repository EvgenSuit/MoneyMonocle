package com.money.monocle.domain.settings

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.domain.Result
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.network.FrankfurterApi
import com.money.monocle.ui.presentation.toStringIfMessageIsNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class SettingsRepository(
    private val auth: FirebaseAuth,
    private val firestore: CollectionReference,
    private val frankfurterApi: FrankfurterApi,
    private val dataStoreManager: DataStoreManager
) {
    suspend fun changeTheme(isDark: Boolean) = dataStoreManager.changeTheme(isDark)
    fun balanceFlow() = dataStoreManager.balanceFlow()
    suspend fun changeCurrency(currentBalance: Balance, newCurrencyEnum: CurrencyEnum) = flow {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                emit(Result.InProgress)
                val convertedMainBalance = frankfurterApi.convert(currentBalance.balance,
                    CurrencyEnum.entries[currentBalance.currency].name, newCurrencyEnum.name)
                firestore.document(uid).collection("balance").document("balance")
                    .set(Balance(newCurrencyEnum.ordinal, convertedMainBalance.rates[newCurrencyEnum.name]!!)).await()
                emit(Result.Success(""))
            } catch (e: Exception) {
                emit(Result.Error(e.toStringIfMessageIsNull()))
            }
        }
    }
    fun themeFlow(): Flow<Boolean> = dataStoreManager.themeFlow()
    fun signOut() = auth.signOut()
}