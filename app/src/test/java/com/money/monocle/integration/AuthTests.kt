package com.money.monocle.integration

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.money.monocle.BalanceListener
import com.money.monocle.BaseIntegrationTestClass
import com.money.monocle.CorrectAuthData
import com.money.monocle.FakeNotNullUserModule
import com.money.monocle.MainActivity
import com.money.monocle.MoneyMonocleNavHost
import com.money.monocle.R
import com.money.monocle.Screen
import com.money.monocle.assertSnackbarIsNotDisplayed
import com.money.monocle.getString
import com.money.monocle.history.mockAuthForAuthentication
import com.money.monocle.setContentWithSnackbar
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import javax.inject.Named

@UninstallModules(FakeNotNullUserModule::class)
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class AuthTests: BaseIntegrationTestClass() {
    @get: Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get: Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    @Named("BalanceListener")
    lateinit var balanceListener: BalanceListener
    private lateinit var navController: NavHostController

    @Module
    @InstallIn(SingletonComponent::class)
    object FakeAuth {
        @Provides
        fun provideAuth(): FirebaseAuth {
            val auth = mockAuthForAuthentication()
            every { auth.currentUser } returns null
            every { auth.addAuthStateListener(any()) } just Runs
            every { auth.removeAuthStateListener(any()) } just Runs
            return auth
        }
    }

    @Before
    fun setup() {
        composeRule.activity.apply {
            setContentWithSnackbar(snackbarScope) {
                navController = TestNavHostController(LocalContext.current)
                navController.navigatorProvider.addNavigator(ComposeNavigator())
                MoneyMonocleNavHost(navController = navController)
            }
        }
    }

    @Test
    fun successSignIn_navigatedHome() {
        composeRule.apply {
            onNodeWithTag(getString(R.string.email)).performTextReplacement(
                CorrectAuthData.EMAIL)
            onNodeWithTag(getString(R.string.password)).performTextReplacement(
                CorrectAuthData.PASSWORD)
            onNodeWithText(getString(R.string.sign_in)).performClick()
            assertSnackbarIsNotDisplayed(snackbarScope)
            assertEquals(Screen.Home.route, navController.currentBackStackEntry?.destination?.route)
        }
    }
    @Test
    fun successSignUp_navigatedHome() {
        composeRule.apply {
            onNodeWithText(getString(R.string.go_to_signup)).performClick()
            onNodeWithTag(getString(R.string.username)).performTextReplacement(
                CorrectAuthData.USERNAME)
            onNodeWithTag(getString(R.string.email)).performTextReplacement(
                CorrectAuthData.EMAIL)
            onNodeWithTag(getString(R.string.password)).performTextReplacement(
                CorrectAuthData.PASSWORD)
            onNodeWithText(getString(R.string.sign_up)).performClick()
            assertSnackbarIsNotDisplayed(snackbarScope)
            assertEquals(Screen.Home.route, navController.currentBackStackEntry?.destination?.route)
        }
    }
}