package com.money.monocle.modules

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.money.monocle.domain.accounts.AccountsRepository
import com.money.monocle.domain.datastore.DataStoreManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AccountsModule {
    @Provides
    @Singleton
    fun provideAccountsRepository(dataStoreManager: DataStoreManager): AccountsRepository =
        AccountsRepository(Firebase.auth, Firebase.firestore, dataStoreManager)
}