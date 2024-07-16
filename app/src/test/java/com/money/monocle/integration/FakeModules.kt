package com.money.monocle.integration

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.money.monocle.BalanceListener
import com.money.monocle.CorrectAuthData
import com.money.monocle.LastTimeCurrencyUpdatedListener
import com.money.monocle.StatsListener
import com.money.monocle.accounts.accounts
import com.money.monocle.accounts.balances
import com.money.monocle.accounts.mockAccountsFirestore
import com.money.monocle.data.Account
import com.money.monocle.data.AccountName
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.domain.accounts.AccountsRepository
import com.money.monocle.domain.auth.AuthRepository
import com.money.monocle.domain.auth.CustomAuthStateListener
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.home.HomeRepository
import com.money.monocle.domain.home.WelcomeRepository
import com.money.monocle.mockTask
import com.money.monocle.modules.AccountsModule
import com.money.monocle.modules.AuthModule
import com.money.monocle.modules.AuthStateListener
import com.money.monocle.modules.HomeModule
import com.money.monocle.userId

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import javax.inject.Named
import javax.inject.Singleton


@Module
@TestInstallIn(replaces = [AuthModule::class],
    components = [SingletonComponent::class])
object FakeAuthModule {
    @Provides
    fun provideAuthRepository(auth: FirebaseAuth): AuthRepository =
        AuthRepository(auth, mockk {every { collection(userId).document(any<String>()).collection("balance")
            .document("balance").set(Balance()) } returns mockTask()
        },  mockk<SignInClient>(relaxed = true), ApplicationProvider.getApplicationContext<Context>().resources)
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
    @Singleton
    fun provideFakeHomeRepository(
        auth: FirebaseAuth,
        dataStoreManager: DataStoreManager,
        @Named("BalanceListener") balanceListener: BalanceListener,
        @Named("PieChartListener") statsListener: StatsListener
    ): HomeRepository {
        val currentAccountIdSlot = slot<String>()
        val balanceListenerRegistration = mockk<ListenerRegistration>()
        val statsListenerRegistration = mockk<ListenerRegistration>()
        val firestore = mockk<FirebaseFirestore> {
            every { collection(userId).document(capture(currentAccountIdSlot)).collection("balance")
                .addSnapshotListener(capture(balanceListener))} answers {
                    val id = currentAccountIdSlot.captured
                //if (id != AccountName.MAIN.name) {
                    val mockedDocs = listOf(mockk<DocumentSnapshot> {
                        every { exists() } returns true
                        every { toObject(Balance::class.java) } returns balances[accounts.map { it.id }.indexOf(id)]
                    })
                    val mockedSnapshot = mockk<QuerySnapshot> {
                        every { isEmpty } returns false
                        every { documents } returns mockedDocs
                    }
                    balanceListener.captured.onEvent(mockedSnapshot, null)
                //}
                balanceListenerRegistration
            }
            every { balanceListenerRegistration.remove() } returns Unit
            every { collection(userId).document(capture(currentAccountIdSlot)).collection("records").whereGreaterThan("timestamp", any())
                .addSnapshotListener(capture(statsListener))} returns statsListenerRegistration
            every { statsListenerRegistration.remove() } returns Unit
        }
        return HomeRepository(auth, firestore, dataStoreManager)
    }
    @Provides
    fun provideFakeWelcomeRepository(): WelcomeRepository {
        return mockk(relaxed = true)
    }
}

@Module
@TestInstallIn(replaces = [AccountsModule::class],
    components = [SingletonComponent::class])
object FakeAccountsModule {
    @Provides
    @Singleton
    fun provideAccountsRepository(
        auth: FirebaseAuth,
        dataStoreManager: DataStoreManager): AccountsRepository {
        return AccountsRepository(auth, mockAccountsFirestore(), dataStoreManager)
    }
}