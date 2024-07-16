package com.money.monocle.modules

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.money.monocle.domain.history.TransactionHistoryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TransactionHistoryModule {
    @Provides
    @Singleton
    fun provideTransactionHistoryRepository(): TransactionHistoryRepository =
        TransactionHistoryRepository(limit = 10, auth = Firebase.auth,
            firestore = Firebase.firestore)
}