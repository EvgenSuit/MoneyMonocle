package com.money.monocle.settings

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.data.ExchangeCurrency
import com.money.monocle.domain.Result
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.network.FrankfurterApi
import com.money.monocle.domain.settings.SettingsRepository
import com.money.monocle.mockAuth
import com.money.monocle.mockTask
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.settings.SettingsViewModel
import com.money.monocle.userId
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsUnitTests {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val listener = slot<EventListener<DocumentSnapshot>>()

    @Before
    fun init() {
        auth = mockAuth()
        firestore = mockFirestore(listener)
    }

    @Test
    fun requestConversion_success() = runTest {
        val currentBalance = 234f
        val from = CurrencyEnum.EUR
        val to = CurrencyEnum.USD
        val api = mockk<FrankfurterApi> {
            coEvery { convert(currentBalance, from.name, to.name) } returns ExchangeCurrency(amount = currentBalance,
                base = from.name, rates = mapOf(to.name to 23f)
            )
        }
        val datastoreManager = mockk<DataStoreManager> {
            every { balanceFlow() } returns flowOf(Balance(from.ordinal, currentBalance))
            every { themeFlow() } returns flowOf()
        }
        val repository = SettingsRepository(auth, firestore.collection("data"), api, datastoreManager)
        val viewModel = SettingsViewModel(repository, CoroutineScopeProvider(this))
        viewModel.checkLastTimeUpdated()
        advanceUntilIdle()
        viewModel.changeCurrency(to)
        advanceUntilIdle()
        println(viewModel.uiState.value.currencyChangeResult.error)
        assertTrue(viewModel.uiState.value.currencyChangeResult is Result.Success)
    }
    @Test
    fun requestConversion_error() = runTest {
        val currentBalance = 234f
        val from = CurrencyEnum.EUR
        val to = CurrencyEnum.USD
        val api = mockk<FrankfurterApi> {
            coEvery { convert(currentBalance, from.name, to.name) } answers {
                throw RuntimeException("error")
            }
        }
        val datastoreManager = mockk<DataStoreManager> {
            every { balanceFlow() } returns flowOf(Balance(from.ordinal, currentBalance))
            every { themeFlow() } returns flowOf()
        }
        val repository = SettingsRepository(auth, firestore.collection("data"), api, datastoreManager)
        val viewModel = SettingsViewModel(repository, CoroutineScopeProvider(this))
        viewModel.checkLastTimeUpdated()
        advanceUntilIdle()
        viewModel.changeCurrency(to)
        advanceUntilIdle()
        assertEquals(viewModel.uiState.value.currencyChangeResult.error, "error")
    }
}