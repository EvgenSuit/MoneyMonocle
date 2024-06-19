package com.money.monocle.settings

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.money.monocle.BaseTestClass
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.data.ExchangeCurrency
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.network.FrankfurterApi
import com.money.monocle.domain.settings.SettingsRepository
import com.money.monocle.mockAuth
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.StringValue
import com.money.monocle.ui.presentation.settings.SettingsViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsUnitTests: BaseTestClass() {
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
        assertTrue(viewModel.uiState.value.currencyChangeResult is CustomResult.Success)
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
        assertEquals(viewModel.uiState.value.currencyChangeResult.error, StringValue.DynamicString("error"))
    }
}