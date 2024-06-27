package com.money.monocle.modules

import android.content.Context
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.money.monocle.domain.record.AddCategoryRepository
import com.money.monocle.domain.record.AddRecordRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import com.money.monocle.R

@Module
@InstallIn(SingletonComponent::class)
object RecordModule {
    @Provides
    @Singleton
    fun provideAddRecordRepository(): AddRecordRepository =
        AddRecordRepository(Firebase.auth, Firebase.firestore)

    @Provides
    @Singleton
    fun provideAddCategoryRepository(): AddCategoryRepository =
        AddCategoryRepository(Firebase.auth, Firebase.firestore.collection("data"))

    @Provides
    @Named("maxCustomCategoryNameLength")
    fun provideMaxCustomCategoryNameLength(@ApplicationContext context: Context): Int =
        context.resources.getInteger(R.integer.max_custom_category_name_length)
}
