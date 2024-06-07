package com.money.monocle.modules

import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.money.monocle.domain.auth.CustomAuthStateListener
import com.money.monocle.domain.history.TransactionHistoryRepository
import com.money.monocle.domain.home.HomeRepository
import com.money.monocle.domain.home.WelcomeRepository
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object HomeModule {
    @Provides
    fun provideHomeRepository(): HomeRepository =
        HomeRepository(
            Firebase.auth,
            Firebase.firestore.collection("data"))

    @Provides
    fun provideWelcomeRepository(): WelcomeRepository =
        WelcomeRepository(
            Firebase.auth,
            Firebase.firestore.collection("data"))
}

