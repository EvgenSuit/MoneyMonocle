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
class DataStoreManager(
    private val accountDataStore: DataStore<Preferences>
) {
    suspend fun changeAccountState(isLoaded: Boolean) {
        accountDataStore.edit {dataStore ->
            dataStore[accountState] = isLoaded
        }
    }
    fun accountStateFlow(): Flow<Boolean> =
        accountDataStore.data.map { dataStore ->
            dataStore[accountState] ?: false
        }
}