package com.money.monocle.integration

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.money.monocle.BalanceListener
import com.money.monocle.BaseIntegrationTestClass
import com.money.monocle.LastTimeCurrencyUpdatedListener
import com.money.monocle.MainActivity
import com.money.monocle.MoneyMonocleNavHost
import com.money.monocle.R
import com.money.monocle.Screen
import com.money.monocle.assertSnackbarIsNotDisplayed
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.data.LastTimeUpdated
import com.money.monocle.data.simpleCurrencyMapper
import com.money.monocle.getString
import com.money.monocle.setContentWithSnackbar
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class SettingsTests: BaseIntegrationTestClass() {
    @get: Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    @get: Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()
    @Inject
    @Named("BalanceListener")
    lateinit var balanceListener: BalanceListener
    @Inject
    @Named("LastTimeCurrencyUpdatedListener")
    lateinit var lastTimeCurrencyUpdatedListener: LastTimeCurrencyUpdatedListener
    @Inject
    override lateinit var auth: FirebaseAuth
    private lateinit var navController: NavHostController
    
    private fun mockLastTimeUpdated(timestamp: Long? = null): DocumentSnapshot =
        mockk {
            every { toObject(LastTimeUpdated::class.java) } returns LastTimeUpdated(timestamp)
        }
    @Before
    fun setup() {
        hiltRule.inject()
        composeRule.apply {
            activity.setContentWithSnackbar(snackbarScope) {
                navController = TestNavHostController(LocalContext.current)
                navController.navigatorProvider.addNavigator(ComposeNavigator())
                MoneyMonocleNavHost(navController = navController)
            }
            waitForIdle()
            showHomeScreen(balanceListener)
            waitForIdle()
            waitUntilExactlyOneExists(hasTestTag(Screen.Settings.route))
            onNodeWithTag(Screen.Settings.route).performClick()
        }
    }

    @Test
    fun isAccountUsed_onSignOut_navigatedToAuth() {
        composeRule.apply {
            onNodeWithTag(getString(R.string.sign_out)).performClick()
            waitForIdle()
            assertEquals(Screen.Auth.route, navController.currentBackStackEntry?.destination?.route)
            assertSnackbarIsNotDisplayed(snackbarScope)
        }
    }

    @Test
    fun isSheetOpen_onSignOut_noSheetShown() {
        composeRule.apply {
            onNodeWithTag(getString(R.string.change_currency)).performClick()
            waitForIdle()
            waitUntil { lastTimeCurrencyUpdatedListener.isCaptured }
            lastTimeCurrencyUpdatedListener.captured.onEvent(mockLastTimeUpdated(Instant.now().toEpochMilli()-24*60*60*1000), null)
            onNodeWithText(getString(R.string.change_currency_from)).assertIsDisplayed()
            auth.signOut()
            waitForIdle()
            onNodeWithText(getString(R.string.be_advised)).assertIsNotDisplayed()
            onNodeWithText(getString(R.string.change_currency_from)).assertIsNotDisplayed()
            onNodeWithText(getString(R.string.already_changed_currency)).assertIsNotDisplayed()
            assertSnackbarIsNotDisplayed(snackbarScope)
            assertEquals(Screen.Auth.route, navController.currentBackStackEntry?.destination?.route)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun updateCurrency_onSuccess_isUpdatedCorrectly() {
        val to = CurrencyEnum.USD
        composeRule.apply {
            onNodeWithTag(getString(R.string.change_currency)).performClick()
            waitForIdle()
            waitUntil { lastTimeCurrencyUpdatedListener.isCaptured }
            lastTimeCurrencyUpdatedListener.captured.onEvent(mockLastTimeUpdated(Instant.now().toEpochMilli()-24*60*60*1000), null)
            onNodeWithTag(currency.name).performClick()
            onNodeWithTag(to.name).performClick()
            onNodeWithText(getString(R.string.confirm)).performClick()
            waitForIdle()
            waitUntilDoesNotExist(hasText(getString(R.string.change_currency_from)))
            snackbarScope.advanceUntilIdle()
            onNodeWithTag(getString(R.string.error_snackbar)).assertIsNotDisplayed()
            onNodeWithTag(Screen.Home.route).performClick()
            waitForIdle()
            // 22 is defined at FakeNetworkModule
            onNodeWithText("22.0${simpleCurrencyMapper(to.ordinal)}").assertIsDisplayed()
        }
    }
}