package com.money.monocle.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.money.monocle.domain.settings.SettingsRepository
import com.money.monocle.getString
import com.money.monocle.mockAuth
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.settings.SettingsViewModel
import com.money.monocle.ui.screens.settings.SettingsScreen
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.money.monocle.R
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.data.ExchangeCurrency
import com.money.monocle.data.LastTimeUpdated
import com.money.monocle.domain.Result
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.network.FrankfurterApi
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsUITests {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    @get: Rule
    val composeRule = createComposeRule()
    private val lastTimeUpdatedListener = slot<EventListener<DocumentSnapshot>>()

    private fun mockLastTimeUpdated(timestamp: Long? = null): DocumentSnapshot =
        mockk {
            every { toObject(LastTimeUpdated::class.java) } returns LastTimeUpdated(timestamp)
        }
    @Before
    fun init() {
        auth = mockAuth()
        firestore = mockFirestore(lastTimeUpdatedListener)
    }

    @After
    fun clean() = unmockkAll()

    @Test
    fun onChangeCurrencyTap_isLastTimeUpdatedNull_isInfoSheetDisplayed() = runTest {
        val datastoreManager = mockk<DataStoreManager> {
            every { balanceFlow() } returns flowOf(Balance(CurrencyEnum.USD.ordinal, 23f))
            every { themeFlow() } returns flowOf(true)
        }
        val repository = SettingsRepository(auth, firestore.collection("data"), mockk(relaxed = true), datastoreManager)
        val scopeProvider = CoroutineScopeProvider(this)
        val viewModel = SettingsViewModel(repository, scopeProvider)
        composeRule.apply {
            composeRule.setContent {
                SettingsScreen(viewModel = viewModel) {}
            }
            advanceUntilIdle()
            onNodeWithTag(getString(R.string.change_currency)).performClick()
            // call advanceUntilIdle in order for checkLastTimeUpdated to collect a result
            advanceUntilIdle()
            onNodeWithTag(getString(R.string.change_currency)).assertIsNotEnabled()
            viewModel.updateLastTimeUpdatedResult(Result.Success(""))
            onNodeWithTag(getString(R.string.change_currency)).assertIsEnabled()
            onNodeWithText(getString(R.string.be_advised)).assertIsDisplayed()
        }
    }
    @Test
    fun onChangeCurrencyTap_isLastTimeUpdatedEligible_isChangeCurrencySheetDisplayed() = runTest {
        val datastoreManager = mockk<DataStoreManager> {
            every { balanceFlow() } returns flowOf(Balance(CurrencyEnum.USD.ordinal, 23f))
            every { themeFlow() } returns flowOf(true)
        }
        val repository = SettingsRepository(auth, firestore.collection("data"), mockk(relaxed = true), datastoreManager)
        val scopeProvider = CoroutineScopeProvider(this)
        val viewModel = SettingsViewModel(repository, scopeProvider)
        var errorCounter = 0
        composeRule.apply {
            composeRule.setContent {
                SettingsScreen(
                    viewModel = viewModel,
                ) {errorCounter++}
            }
            advanceUntilIdle()
            onNodeWithTag(getString(R.string.change_currency)).performClick()
            advanceUntilIdle()
            lastTimeUpdatedListener.captured.onEvent(mockLastTimeUpdated(Instant.now().toEpochMilli()-24*60*60*1000), null)

            onNodeWithText(getString(R.string.change_currency_from)).assertIsDisplayed()
        }
    }
    @Test
    fun onChangeCurrencyTap_isLastTimeUpdatedNotEligible_isErrorDisplayed() = runTest {
        val datastoreManager = mockk<DataStoreManager> {
            every { balanceFlow() } returns flowOf(Balance(CurrencyEnum.USD.ordinal, 23f))
            every { themeFlow() } returns flowOf(true)
        }
        val repository = SettingsRepository(auth, firestore.collection("data"), mockk(relaxed = true), datastoreManager)
        val scopeProvider = CoroutineScopeProvider(this)
        var errorCount = 0
        composeRule.apply {
            composeRule.setContent {
                SettingsScreen(
                    viewModel = SettingsViewModel(repository, scopeProvider),
                ) {errorCount++}
            }
            advanceUntilIdle()
            onNodeWithTag(getString(R.string.change_currency)).performClick()
            advanceUntilIdle()
            lastTimeUpdatedListener.captured.onEvent(mockk {
                every { toObject(LastTimeUpdated::class.java) } returns LastTimeUpdated(Instant.now().toEpochMilli()-1)
            }, null)
            onNodeWithText(getString(R.string.change_currency_from)).assertIsNotDisplayed()
            assertEquals(1, errorCount)
        }
    }
    @Test
    fun onChangeCurrencyTap_change_success() = runTest {
        val from = CurrencyEnum.USD
        val to = CurrencyEnum.EUR
        val currentBalance = 23f
        val api = mockk<FrankfurterApi> {
            coEvery { convert(currentBalance, from.name, to.name) } returns ExchangeCurrency(amount = currentBalance,
                base = from.name, rates = mapOf(to.name to 23f)
            )
        }
        val datastoreManager = mockk<DataStoreManager> {
            every { balanceFlow() } returns flowOf(Balance(from.ordinal, currentBalance))
            every { themeFlow() } returns flowOf(true)
        }
        val repository = SettingsRepository(auth, firestore.collection("data"), api, datastoreManager)
        val scopeProvider = CoroutineScopeProvider(this)
        var errorCount = 0
        val viewModel = SettingsViewModel(repository, scopeProvider)
        composeRule.apply {
            composeRule.setContent {
                SettingsScreen(
                    viewModel = viewModel,
                ) {errorCount++}
            }
            advanceUntilIdle()
            onNodeWithTag(getString(R.string.change_currency)).performClick()
            advanceUntilIdle()
            onNodeWithTag(getString(R.string.change_currency)).assertIsNotEnabled()
            lastTimeUpdatedListener.captured.onEvent(null, null)
            onNodeWithTag(getString(R.string.change_currency)).assertIsEnabled()
            onNodeWithText(getString(R.string.be_advised)).assertIsDisplayed()
            onNodeWithText(getString(R.string.ok)).performClick()
            advanceUntilIdle()


            onNodeWithText(getString(R.string.change_currency_from)).assertIsDisplayed()

            onNodeWithTag(from.name).performClick()
            onNodeWithTag(to.name).performClick()
            onNodeWithText(getString(R.string.confirm)).performClick()
            advanceUntilIdle()
            waitForIdle()
            onNodeWithText(getString(R.string.change_currency_from)).assertIsNotDisplayed()
        }
    }
}