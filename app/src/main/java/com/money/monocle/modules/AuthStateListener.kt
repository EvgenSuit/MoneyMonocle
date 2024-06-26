package com.money.monocle.modules

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.money.monocle.domain.auth.CustomAuthStateListener
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthStateListener {
    @Provides
    @Singleton
    fun provideCustomAuthStateListener(): CustomAuthStateListener =
        CustomAuthStateListener(Firebase.auth)
}