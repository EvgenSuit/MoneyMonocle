package com.money.monocle.integration

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.BalanceListener
import com.money.monocle.BaseIntegrationTestClass
import com.money.monocle.CorrectAuthData
import com.money.monocle.MainActivity
import com.money.monocle.MoneyMonocleNavHost
import com.money.monocle.R
import com.money.monocle.Screen
import com.money.monocle.StatsListener
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.getString
import com.money.monocle.assertSnackbarIsDisplayed
import com.money.monocle.assertSnackbarIsNotDisplayed
import com.money.monocle.setContentWithSnackbar

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import javax.inject.Named

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class UserNotNullTests: BaseIntegrationTestClass() {
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
    @Inject
    override lateinit var auth: FirebaseAuth


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
        }
    }
    @Test
    fun isUserNew_welcomeScreenDisplayed() {
        composeRule.apply {
            assertEquals(navController.currentBackStackEntry?.destination?.route, Screen.Home.route)
            showWelcomeScreen(balanceListener)
            waitForIdle()
            waitUntilExactlyOneExists(hasText(getString(R.string.welcome)))
            onNodeWithTag("BottomNavBar").assertIsNotDisplayed()
            assertFalse(
                assertSnackbarIsDisplayed(
                    snackbarScope
                )
            )
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
            waitUntilAtLeastOneExists(hasText("${getString(R.string.hello)}, ${CorrectAuthData.USERNAME}"))
            onNodeWithTag("BottomNavBar").assertIsDisplayed()
            assertFalse(
                assertSnackbarIsDisplayed(
                    snackbarScope
                )
            )
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
            waitUntil { Screen.Auth.route == navController.currentDestination?.route }
            onNodeWithTag("BottomNavBar").assertIsNotDisplayed()
            assertSnackbarIsDisplayed(snackbarScope)
        }
    }

    @Test
    fun signOut_authScreenDisplayed() {
        composeRule.apply {
            auth.signOut()
            waitForIdle()
            waitUntil { Screen.Auth.route == navController.currentDestination?.route }
            onNodeWithTag("BottomNavBar").assertIsNotDisplayed()
            assertSnackbarIsNotDisplayed(snackbarScope)
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
            waitUntilAtLeastOneExists(hasText("${getString(R.string.hello)}, ${CorrectAuthData.USERNAME}"))
            onNodeWithTag("BottomNavBar").assertIsDisplayed()
            assertFalse(
                assertSnackbarIsDisplayed(
                    snackbarScope
                )
            )
        }
    }
}