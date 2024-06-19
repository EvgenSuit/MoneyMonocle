package com.money.monocle.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.money.monocle.BaseTestClass
import com.money.monocle.R
import com.money.monocle.assertSnackbarTextEquals
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.data.ExchangeCurrency
import com.money.monocle.data.LastTimeUpdated
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.network.FrankfurterApi
import com.money.monocle.domain.settings.SettingsRepository
import com.money.monocle.getString
import com.money.monocle.mockAuth
import com.money.monocle.setContentWithSnackbar
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.settings.SettingsViewModel
import com.money.monocle.ui.screens.settings.SettingsScreen
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsUITests: BaseTestClass() {
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
            composeRule.setContentWithSnackbar(this@runTest) {
                SettingsScreen(viewModel = viewModel)
            }
            advanceUntilIdle()
            onNodeWithTag(getString(R.string.change_currency)).performClick()
            // call advanceUntilIdle in order for checkLastTimeUpdated to collect a result
            advanceUntilIdle()
            onNodeWithTag(getString(R.string.change_currency)).assertIsNotEnabled()
            viewModel.updateLastTimeUpdatedResult(CustomResult.Success)
            onNodeWithTag(getString(R.string.change_currency)).assertIsEnabled()
            onNodeWithText(getString(R.string.be_advised)).assertIsDisplayed()
            onNodeWithTag(getString(R.string.error_snackbar)).assertIsNotDisplayed()
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
        composeRule.apply {
            composeRule.setContentWithSnackbar(snackbarScope) {
                SettingsScreen(viewModel = viewModel)
            }
            advanceUntilIdle()
            onNodeWithTag(getString(R.string.change_currency)).performClick()
            advanceUntilIdle()
            lastTimeUpdatedListener.captured.onEvent(mockLastTimeUpdated(Instant.now().toEpochMilli()-24*60*60*1000), null)
            snackbarScope.advanceUntilIdle()
            onNodeWithText(getString(R.string.change_currency_from)).assertIsDisplayed()
            onNodeWithTag(getString(R.string.error_snackbar)).assertIsNotDisplayed()
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
        composeRule.apply {
            composeRule.setContentWithSnackbar(snackbarScope) {
                SettingsScreen(SettingsViewModel(repository, scopeProvider))
            }
            advanceUntilIdle()
            onNodeWithTag(getString(R.string.change_currency)).performClick()
            advanceUntilIdle()
            lastTimeUpdatedListener.captured.onEvent(mockk {
                every { toObject(LastTimeUpdated::class.java) } returns LastTimeUpdated(Instant.now().toEpochMilli()-1)
            }, null)
            onNodeWithText(getString(R.string.change_currency_from)).assertIsNotDisplayed()
            snackbarScope.advanceUntilIdle()
            assertSnackbarTextEquals(snackbarScope,
                getString(R.string.already_changed_currency)
            )
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
            val viewModel = SettingsViewModel(repository, scopeProvider)
            composeRule.apply {
                composeRule.setContentWithSnackbar(snackbarScope) {
                    SettingsScreen(viewModel)
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
                snackbarScope.advanceUntilIdle()
                onNodeWithTag(getString(R.string.error_snackbar)).assertIsNotDisplayed()
            }
        }
}