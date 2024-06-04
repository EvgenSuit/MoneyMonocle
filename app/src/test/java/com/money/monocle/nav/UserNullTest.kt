package com.money.monocle.nav

import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.money.monocle.MainActivity
import com.money.monocle.MoneyMonocleNavHost
import com.money.monocle.Screen
import com.money.monocle.domain.auth.AuthRepository
import com.money.monocle.domain.auth.CustomAuthStateListener
import com.money.monocle.modules.AuthModule
import com.money.monocle.modules.AuthStateListener
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
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@UninstallModules(AuthStateListener::class, AuthModule::class)
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
    object AuthStateListener {
        @Provides
        fun provideCustomAuthStateListener(): CustomAuthStateListener {
            val mockFirebaseAuth = mockk<FirebaseAuth> {
                every { currentUser } returns null
                every { addAuthStateListener(any()) } just Runs
                every { removeAuthStateListener(any()) } just Runs
            }
            return CustomAuthStateListener(mockFirebaseAuth)
        }
    }
    @Module
    @InstallIn(SingletonComponent::class)
    object FakeAuthModule {
        @Provides
        fun provideAuthRepository(): AuthRepository =
            AuthRepository(mockk<FirebaseAuth> {
                every { currentUser } returns null
            }, mockk<FirebaseFirestore>(relaxed = true), mockk<SignInClient>(relaxed = true))
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