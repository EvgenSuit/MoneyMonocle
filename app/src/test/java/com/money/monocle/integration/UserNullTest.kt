package com.money.monocle.integration

import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.BalanceListener
import com.money.monocle.BaseIntegrationTestClass
import com.money.monocle.CorrectAuthData
import com.money.monocle.MainActivity
import com.money.monocle.MoneyMonocleNavHost
import com.money.monocle.R
import com.money.monocle.Screen
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.home.HomeRepository
import com.money.monocle.domain.home.WelcomeRepository
import com.money.monocle.getString
import com.money.monocle.setContentWithSnackbar
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.home.HomeViewModel
import com.money.monocle.ui.screens.home.HomeScreen
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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
class UserNullTest: BaseIntegrationTestClass() {
    @get: Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)
    @get: Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()
    private lateinit var navController: TestNavHostController

    @Module
    @InstallIn(SingletonComponent::class)
    object FakeAuth {
        @Provides
        fun provideAuth(): FirebaseAuth = mockk<FirebaseAuth> {
            every { currentUser } returns null
            every { addAuthStateListener(any()) } returns Unit
            every { removeAuthStateListener(any()) } returns Unit
        }
    }
    @Before
    fun init() {
        composeRule.activity.setContentWithSnackbar(snackbarScope) {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            MoneyMonocleNavHost(navController = navController)
        }
    }
    @Test
    fun isUserNull_authScreenShown() {
        composeRule.runOnIdle {
            assertEquals(navController.currentBackStackEntry?.destination?.route, Screen.Auth.route)
        }
    }
}