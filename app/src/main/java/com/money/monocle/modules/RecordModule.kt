package com.money.monocle.modules

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.money.monocle.domain.record.AddCategoryRepository
import com.money.monocle.domain.record.AddRecordRepository
import com.money.monocle.domain.record.CustomCategoriesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecordModule {
    @Provides
    @Singleton
    fun provideAddRecordRepository(): AddRecordRepository =
        AddRecordRepository(5, Firebase.auth, Firebase.firestore)

    @Provides
    @Singleton
    fun provideAddCategoryRepository(): AddCategoryRepository =
        AddCategoryRepository(Firebase.auth, Firebase.firestore.collection("data"))

    @Provides
    @Singleton
    fun provideCustomCategoriesRepository(): CustomCategoriesRepository =
        CustomCategoriesRepository(10, Firebase.auth, Firebase.firestore.collection("data"))
}
