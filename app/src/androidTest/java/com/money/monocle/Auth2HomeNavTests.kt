package com.money.monocle

import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.money.monocle.domain.auth.CustomAuthStateListener
import com.money.monocle.modules.HomeModule
import com.money.monocle.modules.NavHostModule
import com.money.monocle.modules.UtilsModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test



//@UninstallModules(UtilsModule::class)
@HiltAndroidTest
class Auth2HomeNavTests {
    @get: Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)
    @get: Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()
    private lateinit var navController: TestNavHostController

    @Module
    @InstallIn(ActivityComponent::class)
    object NavHostModule {
        @Provides
        fun provideCustomAuthStateListener(): CustomAuthStateListener {
            val mockFirebaseAuth = mockk<FirebaseAuth> {
                every { currentUser } returns mockk<FirebaseUser> {
                    every { uid } returns "test_uid"
                    every { displayName } returns "Test User"
                }
            }
            return CustomAuthStateListener(mockFirebaseAuth)
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
    fun testHomeToAuthNav_userIsNull_navigateToAuth() = runTest {
        /*val viewModel = mockk<MoneyMonocleNavHostViewModel>(relaxed = true) {
            every { isUserNullFlow } returns flow { emit(false) }
        }
        advanceUntilIdle()
        composeRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            MoneyMonocleNavHost(onError = {}, viewModel = viewModel, navController = navController)
        }
        composeRule.waitForIdle()
        assertEquals(navController.currentDestination?.route, Screens.Home.route)*/
    }

}