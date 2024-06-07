package com.money.monocle.modules

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.settings.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    @Provides
    fun provideSettingsRepository(dataStoreManager: DataStoreManager): SettingsRepository =
        SettingsRepository(Firebase.auth, dataStoreManager)
}