package com.money.monocle.modules

import android.content.Context
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.money.monocle.domain.auth.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext


@Module
@InstallIn(ActivityRetainedComponent::class)
object AuthModule {
    @Provides
    fun provideAuthRepository(@ApplicationContext context: Context): AuthRepository =
        AuthRepository(Firebase.auth,
            Firebase.firestore,
            Identity.getSignInClient(context))
}