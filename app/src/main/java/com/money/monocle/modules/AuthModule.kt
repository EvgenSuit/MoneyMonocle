package com.money.monocle.modules

import android.content.Context
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.money.monocle.domain.auth.AuthRepository
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.useCases.EmailValidator
import com.money.monocle.domain.useCases.PasswordValidator
import com.money.monocle.domain.useCases.UsernameValidator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun provideAuthRepository(@ApplicationContext context: Context): AuthRepository =
        AuthRepository(Firebase.auth,
            Firebase.firestore,
            Identity.getSignInClient(context))
}