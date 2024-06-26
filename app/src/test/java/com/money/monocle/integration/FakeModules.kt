package com.money.monocle.integration

import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.money.monocle.BalanceListener
import com.money.monocle.CorrectAuthData
import com.money.monocle.LastTimeCurrencyUpdatedListener
import com.money.monocle.StatsListener
import com.money.monocle.data.Balance
import com.money.monocle.domain.auth.AuthRepository
import com.money.monocle.domain.auth.CustomAuthStateListener
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.home.HomeRepository
import com.money.monocle.domain.home.WelcomeRepository
import com.money.monocle.mockTask
import com.money.monocle.modules.AuthModule
import com.money.monocle.modules.AuthStateListener
import com.money.monocle.modules.HomeModule
import com.money.monocle.userId

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import javax.inject.Named
import javax.inject.Singleton


@Module
@TestInstallIn(replaces = [AuthModule::class],
    components = [SingletonComponent::class])
object FakeAuthModule {
    @Provides
    fun provideAuthRepository(auth: FirebaseAuth): AuthRepository =
        AuthRepository(auth, mockk {every { collection("data").document(userId).collection("balance")
            .document("balance").set(Balance())  } returns mockTask()
        }, mockk<SignInClient>(relaxed = true))
}

@Module
@InstallIn(SingletonComponent::class)
object FirestoreListenersModule {
    @Provides
    @Singleton
    @Named("BalanceListener")
    fun provideFirestoreListener(): BalanceListener = slot()
    @Provides
    @Singleton
    @Named("PieChartListener")
    fun providePieChartListener(): StatsListener = slot()
    @Provides
    @Singleton
     @Named("LastTimeCurrencyUpdatedListener")
    fun provideLastTimeCurrencyUpdatedListener(): LastTimeCurrencyUpdatedListener = slot()
}

@Module
@TestInstallIn(replaces = [HomeModule::class],
    components = [SingletonComponent::class])
object FakeHomeModule {
    @Provides
    fun provideFakeHomeRepository(
        auth: FirebaseAuth,
        dataStoreManager: DataStoreManager,
        @Named("BalanceListener") balanceListener: BalanceListener,
        @Named("PieChartListener") statsListener: StatsListener
    ): HomeRepository {
        val firestore = mockk<FirebaseFirestore> {
            every { collection("data").document(userId).collection("balance")
                .addSnapshotListener(capture(balanceListener))} returns mockk<ListenerRegistration>()
            every { collection("data").document(userId).collection("balance")
                .addSnapshotListener(capture(balanceListener)).remove() } returns Unit
            every { collection("data").document(userId).collection("records").whereGreaterThan("timestamp", any())
                .addSnapshotListener(capture(statsListener))} returns mockk<ListenerRegistration>()
            every { collection("data").document(userId).collection("records").whereGreaterThan("timestamp", any())
                .addSnapshotListener(capture(statsListener)).remove() } returns Unit
        }
        return HomeRepository(auth, firestore.collection("data"), dataStoreManager)
    }
    @Provides
    fun provideFakeWelcomeRepository(): WelcomeRepository {
        return mockk(relaxed = true)
    }
}
