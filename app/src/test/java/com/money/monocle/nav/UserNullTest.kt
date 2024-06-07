package com.money.monocle.nav

import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.money.monocle.MainActivity
import com.money.monocle.MoneyMonocleNavHost
import com.money.monocle.Screen
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@UninstallModules(FakeNotNullUserModule::class)
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class UserNullTest {
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
        }
    }
    @Before
    fun init() {
        composeRule.activity.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            MoneyMonocleNavHost(onError = {}, navController = navController)
        }
    }

    @Test
    fun isUserNull_authScreenShown() {
        composeRule.runOnIdle {
            assertEquals(navController.currentBackStackEntry?.destination?.route, Screen.Auth.route)
        }
    }
}