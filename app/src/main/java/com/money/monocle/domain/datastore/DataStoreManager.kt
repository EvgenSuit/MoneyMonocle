package com.money.monocle.domain.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.money.monocle.data.Balance
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

val Context.accountDataStore: DataStore<Preferences> by preferencesDataStore("accountDataStore")
private val accountState = booleanPreferencesKey("accountState")
private val isWelcomeScreenShown = booleanPreferencesKey("isWelcomeScreenShown")
private val currency = intPreferencesKey("currency")
private val balance = floatPreferencesKey("currentBalance")
val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore("themeDataStore")
private val isThemeDark = booleanPreferencesKey("isThemeDark")

class DataStoreManager(
    private val accountDataStore: DataStore<Preferences>,
    private val themeDataStore: DataStore<Preferences>
) {
    suspend fun setBalance(newBalance: Balance) {
        accountDataStore.edit {
            it[currency] = newBalance.currency
            it[balance] = newBalance.balance
        }
    }

    fun balanceFlow() = combine(
        accountDataStore.data.map { it[currency] ?: 0 },
        accountDataStore.data.map { it[balance] ?: 0f }) {currentCurrency, currentBalance ->
        Balance(currentCurrency, currentBalance)
    }
    suspend fun changeTheme(isDark: Boolean) {
        themeDataStore.edit {
            it[isThemeDark] = isDark
        }
    }
    fun themeFlow() = themeDataStore.data.map { it[isThemeDark] ?: true }
    suspend fun changeAccountState(isLoaded: Boolean) {
        accountDataStore.edit {
            it[accountState] = isLoaded
        }
    }
    fun accountStateFlow() = accountDataStore.data.map {
            it[accountState] ?: false
    }
    suspend fun isWelcomeScreenShown(shown: Boolean) {
        accountDataStore.edit {
            it[isWelcomeScreenShown] = shown
        }
    }
    fun isWelcomeScreenShownFlow() = accountDataStore.data.map {
            it[isWelcomeScreenShown] ?: false
        }
}