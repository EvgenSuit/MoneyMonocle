package com.money.monocle.modules

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.home.HomeRepository
import com.money.monocle.domain.home.WelcomeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


@Module
@InstallIn(SingletonComponent::class)
object HomeModule {
    @Provides
    fun provideHomeRepository(dataStoreManager: DataStoreManager): HomeRepository =
        HomeRepository(
            Firebase.auth,
            Firebase.firestore.collection("data"),
            dataStoreManager)

    @Provides
    fun provideWelcomeRepository(): WelcomeRepository =
        WelcomeRepository(
            Firebase.auth,
            Firebase.firestore.collection("data"))
}

