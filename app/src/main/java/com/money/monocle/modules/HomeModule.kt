package com.money.monocle.modules

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.money.monocle.domain.home.HomeRepository
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ViewModelScoped


@Module
@InstallIn(ActivityRetainedComponent::class)
object HomeModule {
    @Provides
    fun provideHomeRepository(): HomeRepository =
        HomeRepository(
            Firebase.auth,
            Firebase.firestore.collection("users"))

    @Provides
    fun provideCoroutineScopeProvider(): CoroutineScopeProvider =
        CoroutineScopeProvider()
}