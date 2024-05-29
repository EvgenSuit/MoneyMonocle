package com.money.monocle.modules

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.firestore
import com.money.monocle.domain.record.AddRecordRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


@Module
@InstallIn(SingletonComponent::class)
object RecordModule {
    @Provides
    fun provideAddRecordRepository(): AddRecordRepository =
        AddRecordRepository(Firebase.auth, Firebase.firestore)
}