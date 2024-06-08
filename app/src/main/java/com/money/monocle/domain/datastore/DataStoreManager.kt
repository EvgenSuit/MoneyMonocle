package com.money.monocle.domain.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.accountDataStore: DataStore<Preferences> by preferencesDataStore("accountDataStore")
private val accountState = booleanPreferencesKey("accountState")
private val isWelcomeScreenShown = booleanPreferencesKey("isWelcomeScreenShown")
val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore("themeDataStore")
private val isThemeDark = booleanPreferencesKey("isThemeDark")
class DataStoreManager(
    private val accountDataStore: DataStore<Preferences>,
    private val themeDataStore: DataStore<Preferences>
) {
    suspend fun changeTheme(isDark: Boolean) {
        themeDataStore.edit {
            it[isThemeDark] = isDark
        }
    }
    fun themeFlow(): Flow<Boolean> = themeDataStore.data.map { it[isThemeDark] ?: true }
    suspend fun changeAccountState(isLoaded: Boolean) {
        accountDataStore.edit {
            it[accountState] = isLoaded
        }
    }
    fun accountStateFlow(): Flow<Boolean> =
        accountDataStore.data.map {
            it[accountState] ?: false
        }
    suspend fun isWelcomeScreenShown(shown: Boolean) {
        accountDataStore.edit {
            it[isWelcomeScreenShown] = shown
        }
    }
    fun isWelcomeScreenShownFlow(): Flow<Boolean> =
        accountDataStore.data.map {
            it[isWelcomeScreenShown] ?: false
        }
}