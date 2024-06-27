package com.money.monocle

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.money.monocle.data.Balance
import com.money.monocle.data.ExchangeCurrency
import com.money.monocle.data.LastTimeUpdated
import com.money.monocle.domain.auth.CustomAuthStateListener
import com.money.monocle.domain.useCases.DateFormatter
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.datastore.accountDataStore
import com.money.monocle.domain.datastore.themeDataStore
import com.money.monocle.domain.network.FrankfurterApi
import com.money.monocle.domain.settings.SettingsRepository
import com.money.monocle.domain.useCases.CurrencyFormatValidator
import com.money.monocle.modules.AuthStateListener
import com.money.monocle.modules.NetworkModule
import com.money.monocle.modules.SettingsModule
import com.money.monocle.modules.UtilsModule
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Named
import javax.inject.Singleton

@Module
@TestInstallIn(replaces = [UtilsModule::class],
    components = [SingletonComponent::class])
object FakeUtilsModule {
    @Provides
    fun provideCoroutineScopeProvider(): CoroutineScopeProvider =
        CoroutineScopeProvider(CoroutineScope(Dispatchers.Default + SupervisorJob()))
    @Provides
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager =
        DataStoreManager(context.accountDataStore, context.themeDataStore)
    @Provides
    fun provideDateFormatter(): DateFormatter = DateFormatter()
    @Provides
    @Singleton
    fun provideCurrencyFormatValidator(@ApplicationContext context: Context): CurrencyFormatValidator =
        CurrencyFormatValidator(context.resources.getInteger(R.integer.max_amount_length))
}
@Module
@InstallIn(SingletonComponent::class)
object FakeNotNullUserModule {
    private val authStateListener = mutableStateOf<FirebaseAuth.AuthStateListener?>(null)
    @Provides
    @Singleton
    fun provideAuth(): FirebaseAuth = mockk<FirebaseAuth> {
        every { currentUser } returns mockk<FirebaseUser>{
            every { uid } returns userId
            every { displayName } returns CorrectAuthData.USERNAME
        }
        every { signOut() } answers {
            every { currentUser } returns null
            authStateListener.value?.onAuthStateChanged(mockk<FirebaseAuth>{ every { currentUser } returns null})
        }
        every { addAuthStateListener(any()) } answers { authStateListener.value = firstArg() }
        every { removeAuthStateListener(any()) } answers { authStateListener.value = null }
    }
}
@Module
@TestInstallIn(replaces = [AuthStateListener::class],
    components = [SingletonComponent::class])
object FakeAuthStateListenerModule {
    @Provides
    @Singleton
    fun provideCustomAuthStateListener(auth: FirebaseAuth): CustomAuthStateListener {
        return CustomAuthStateListener(auth)
    }
}

@Module
@TestInstallIn(replaces = [SettingsModule::class],
    components = [SingletonComponent::class])
object FakeSettingsModule {
    @Provides
    @Singleton
    fun provideFakeSettingsRepository(
        @Named("LastTimeCurrencyUpdatedListener") listener: LastTimeCurrencyUpdatedListener,
        @Named("BalanceListener") balanceListener: BalanceListener,
        auth: FirebaseAuth,
        frankfurterApi: FrankfurterApi,
        dataStoreManager: DataStoreManager,
        customAuthStateListener: CustomAuthStateListener): SettingsRepository {
        val firestore = mockk<FirebaseFirestore> {
            every { collection("data").document(userId).collection("balance")
                .document("lastTimeUpdated").addSnapshotListener(capture(listener))} returns mockk<ListenerRegistration>()
            every { collection("data").document(userId).collection("balance")
                .document("lastTimeUpdated").addSnapshotListener(capture(listener)).remove()} returns Unit
            every { collection("data").document(userId).collection("balance")
                .document("lastTimeUpdated").set(any()) } answers {
                listener.captured.onEvent(mockk<DocumentSnapshot> {
                    every { toObject(LastTimeUpdated::class.java) } returns firstArg<LastTimeUpdated>()
                }, null)
                mockTask()
            }
            every { collection("data").document(userId).collection("balance")
                .document("balance").set(any())} answers {
                    balanceListener.captured.onEvent(mockk {
                        every { isEmpty } returns false
                        every { documents } returns listOf(
                            mockk<DocumentSnapshot> {
                                every { toObject(Balance::class.java) } returns firstArg()
                            }
                        )
                    }, null)
                mockTask()
            }
        }
        return SettingsRepository(auth, firestore.collection("data"),
            frankfurterApi, dataStoreManager
        )
    }
}

@Module
@TestInstallIn(replaces = [NetworkModule::class],
    components = [SingletonComponent::class])
class FakeNetworkModule {
    @Provides
    @Singleton
    fun provideFrankfurterApi(): FrankfurterApi =
        mockk {
            coEvery { convert(any(), any(), any()) } answers {
                val args = it.invocation.args
                ExchangeCurrency(args[0] as Float,
                    base = args[1] as String,
                    rates = mapOf(args[2] as String to 22f)
                )
            }
        }
}