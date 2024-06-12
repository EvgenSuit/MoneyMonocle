package com.money.monocle.integration

import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.BalanceListener
import com.money.monocle.MainActivity
import com.money.monocle.MoneyMonocleNavHost
import com.money.monocle.StatsListener
import com.money.monocle.R
import com.money.monocle.Screen
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.getString
import com.money.monocle.username
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import javax.inject.Named

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class UserNotNullTests {
    @get: Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    @get: Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()
    private lateinit var navController: NavHostController

    @Inject
    @Named("BalanceListener")
    lateinit var balanceListener: BalanceListener
    @Inject
    @Named("PieChartListener")
    lateinit var statsListener: StatsListener


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
    fun isUserNew_welcomeScreenDisplayed() {
        composeRule.apply {
            assertEquals(navController.currentBackStackEntry?.destination?.route, Screen.Home.route)
            val mockedDocs = listOf(mockk<DocumentSnapshot> {
                every { exists() } returns true
                every { toObject(Balance::class.java) } returns Balance()
            })
            val mockedSnapshot = mockk<QuerySnapshot> {
                every { isEmpty } returns false
                every { documents } returns mockedDocs
            }
            balanceListener.captured.onEvent(mockedSnapshot, null)
            waitForIdle()
            waitUntilAtLeastOneExists(hasText(getString(R.string.welcome)))
            onNodeWithTag("BottomNavBar").assertIsNotDisplayed()
        }
    }
    @Test
    fun isUserUsed_mainContentDisplayed() {
        val currentBalance = 233.4f
        val currency = CurrencyEnum.EUR
        composeRule.apply {
            assertEquals(navController.currentBackStackEntry?.destination?.route, Screen.Home.route)
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
            waitUntilAtLeastOneExists(hasText("${getString(R.string.hello)}, $username"))
            onNodeWithTag("BottomNavBar").assertIsDisplayed()
        }
    }
    @Test
    fun isUserDeleted_authScreenDisplayed() {
        composeRule.apply {
            assertEquals(navController.currentBackStackEntry?.destination?.route, Screen.Home.route)
            val mockedSnapshot = mockk<QuerySnapshot> {
                every { isEmpty } returns true
                every { documents } returns listOf()
            }
            balanceListener.captured.onEvent(mockedSnapshot, null)
            waitForIdle()
            assertEquals(Screen.Auth.route, navController.currentDestination?.route)
            onNodeWithTag("BottomNavBar").assertIsNotDisplayed()
        }
    }
    @Test
    fun isUserNew_onWelcomeScreenAccountCreated_showMainContent() {
        composeRule.apply {
            assertEquals(navController.currentBackStackEntry?.destination?.route, Screen.Home.route)
            val mockedDocs = listOf(mockk<DocumentSnapshot> {
                every { exists() } returns true
                every { toObject(Balance::class.java) } returns Balance()
            })
            val mockedSnapshot = mockk<QuerySnapshot> {
                every { isEmpty } returns false
                every { documents } returns mockedDocs
            }
            balanceListener.captured.onEvent(mockedSnapshot, null)
            waitForIdle()
            waitUntilAtLeastOneExists(hasText(getString(R.string.welcome)))
            onNodeWithTag("BottomNavBar").assertIsNotDisplayed()
            // trigger the show of main content
            val currentBalance = 233.4f
            val currency = CurrencyEnum.EUR
            val mockedDocs2 = listOf(mockk<DocumentSnapshot> {
                every { exists() } returns true
                every { toObject(Balance::class.java) } returns Balance(currency.ordinal, currentBalance)
            })
            val mockedSnapshot2 = mockk<QuerySnapshot> {
                every { isEmpty } returns false
                every { documents } returns mockedDocs2
            }
            balanceListener.captured.onEvent(mockedSnapshot2, null)
            waitForIdle()
            waitUntilAtLeastOneExists(hasText("${getString(R.string.hello)}, $username"))
            onNodeWithTag("BottomNavBar").assertIsDisplayed()
        }
    }
}