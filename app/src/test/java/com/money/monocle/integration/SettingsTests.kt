package com.money.monocle.integration

import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.BalanceListener
import com.money.monocle.MainActivity
import com.money.monocle.MoneyMonocleNavHost
import com.money.monocle.R
import com.money.monocle.Screen
import com.money.monocle.StatsListener
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.getString
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.username
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import javax.inject.Named

@OptIn(ExperimentalTestApi::class, ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
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
    @Named("PieChartListener")
    lateinit var statsListenerSlot: StatsListener
    private lateinit var navController: NavHostController

    @Before
    fun setup() {
        hiltRule.inject()
        composeRule.activity.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            MoneyMonocleNavHost(onError = {}, navController = navController)
        }
        composeRule.waitForIdle()
    }
    @After
    fun clean() = unmockkAll()

    @Test
    fun isAccountUsed_onSignOut_navigatedToAuth() {
        val currentBalance = 233.4f
        val currency = CurrencyEnum.EUR
        composeRule.apply {
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
            waitUntilExactlyOneExists(hasText("${getString(R.string.hello)}, $username"))
            waitUntilExactlyOneExists(hasTestTag(Screen.Settings.route))
            onNodeWithTag(Screen.Settings.route).performClick()
            onNodeWithTag(getString(R.string.sign_out)).performClick()
            waitForIdle()
            assertEquals(Screen.Auth.route, navController.currentBackStackEntry?.destination?.route)
        }
    }
    @Test
    fun hasCurrencyChanged_isCorrectlyDisplayed() {
        val currentBalance = 233.4f
        val currency = CurrencyEnum.EUR
        val newCurrency = CurrencyEnum.USD
        composeRule.apply {
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
            waitUntilExactlyOneExists(hasText("${getString(R.string.hello)}, $username"))
            waitUntilExactlyOneExists(hasTestTag(Screen.Settings.route))
            onNodeWithTag(Screen.Settings.route).performClick()

            waitForIdle()
            assertEquals(Screen.Auth.route, navController.currentBackStackEntry?.destination?.route)
        }
    }
}