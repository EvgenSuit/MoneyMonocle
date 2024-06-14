package com.money.monocle.integration

import androidx.activity.compose.setContent
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
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.BalanceListener
import com.money.monocle.LastTimeUpdatedListener
import com.money.monocle.MainActivity
import com.money.monocle.MoneyMonocleNavHost
import com.money.monocle.R
import com.money.monocle.Screen
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.data.LastTimeUpdated
import com.money.monocle.data.simpleCurrencyMapper
import com.money.monocle.getString
import com.money.monocle.username
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
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
class SettingsTests {
    @get: Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    @get: Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()
    @Inject
    @Named("BalanceListener")
    lateinit var balanceListener: BalanceListener
    @Inject
    @Named("LastTimeUpdatedListener")
    lateinit var lastTimeCurrencyUpdatedListener: LastTimeUpdatedListener
    @Inject
    lateinit var auth: FirebaseAuth
    private lateinit var navController: NavHostController

    val currentBalance = 233.4f
    val currency = CurrencyEnum.EUR
    private fun mockLastTimeUpdated(timestamp: Long? = null): DocumentSnapshot =
        mockk {
            every { toObject(LastTimeUpdated::class.java) } returns LastTimeUpdated(timestamp)
        }
    @Before
    fun setup() {
        hiltRule.inject()
        composeRule.apply {
            activity.setContent {
                navController = TestNavHostController(LocalContext.current)
                navController.navigatorProvider.addNavigator(ComposeNavigator())
                MoneyMonocleNavHost(onError = {}, navController = navController)
            }
            waitForIdle()
            val mockedDocs = listOf(mockk<DocumentSnapshot> {
                every { exists() } returns true
                every { toObject(Balance::class.java) } returns Balance(currency.ordinal, currentBalance)
            })
            val mockedSnapshot = mockk<QuerySnapshot> {
                every { isEmpty } returns false
                every { documents } returns mockedDocs
            }
            balanceListener.captured.onEvent(mockedSnapshot, null)
            waitForIdle()
            waitUntilExactlyOneExists(hasTestTag(Screen.Settings.route))
            onNodeWithTag(Screen.Settings.route).performClick()
        }
    }
    @After
    fun clean() = unmockkAll()

    @Test
    fun isAccountUsed_onSignOut_navigatedToAuth() {
        composeRule.apply {
            onNodeWithTag(getString(R.string.sign_out)).performClick()
            waitForIdle()
            assertEquals(Screen.Auth.route, navController.currentBackStackEntry?.destination?.route)
        }
    }

    @Test
    fun isSheetOpen_onSignOut_noSheetShownShown() {
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
            assertEquals(Screen.Auth.route, navController.currentBackStackEntry?.destination?.route)
        }
    }
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
            onNodeWithTag("errorSnackbar").assertIsNotDisplayed()
            onNodeWithTag(Screen.Home.route).performClick()
            waitForIdle()
            // 22 is defined at FakeNetworkModule
            onNodeWithText("22.0${simpleCurrencyMapper(to.ordinal)}").assertIsDisplayed()
        }
    }
}