package com.money.monocle.domain.settings

import com.google.firebase.auth.FirebaseAuth
import com.money.monocle.domain.datastore.DataStoreManager
import kotlinx.coroutines.flow.Flow

class SettingsRepository(
    private val auth: FirebaseAuth,
    private val dataStoreManager: DataStoreManager
) {
    suspend fun changeTheme(isDark: Boolean) = dataStoreManager.changeTheme(isDark)
    fun themeFlow(): Flow<Boolean> = dataStoreManager.themeFlow()
    fun signOut() = auth.signOut()
}