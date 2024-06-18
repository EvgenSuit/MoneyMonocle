package com.money.monocle

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.money.monocle.data.Balance
import com.money.monocle.data.ExchangeCurrency
import com.money.monocle.data.LastTimeUpdated
import com.money.monocle.domain.useCases.DateFormatter
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.datastore.accountDataStore
import com.money.monocle.domain.datastore.themeDataStore
import com.money.monocle.domain.network.FrankfurterApi
import com.money.monocle.domain.settings.SettingsRepository
import com.money.monocle.modules.NetworkModule
import com.money.monocle.modules.SettingsModule
import com.money.monocle.modules.UtilsModule
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import dagger.Module
import dagger.Provides
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
        dataStoreManager: DataStoreManager): SettingsRepository {
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
            frankfurterApi, dataStoreManager)
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