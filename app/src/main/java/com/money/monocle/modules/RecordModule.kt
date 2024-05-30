package com.money.monocle.modules

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.money.monocle.domain.DateFormatter
import com.money.monocle.domain.history.TransactionHistoryRepository
import com.money.monocle.domain.home.HomeRepository
import com.money.monocle.domain.home.WelcomeRepository
import com.money.monocle.domain.record.AddRecordRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecordModule {
    @Provides
    fun provideAddRecordRepository(): AddRecordRepository =
        AddRecordRepository(Firebase.auth, Firebase.firestore)


}
