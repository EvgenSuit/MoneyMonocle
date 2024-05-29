package com.money.monocle.record

import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.money.monocle.MainActivity
import com.money.monocle.MoneyMonocleNavHost
import com.money.monocle.Record
import com.money.monocle.domain.auth.CustomAuthStateListener
import com.money.monocle.domain.record.AddRecordRepository
import com.money.monocle.mockTask
import com.money.monocle.modules.NavHostModule
import com.money.monocle.modules.RecordModule
import com.money.monocle.ui.presentation.MoneyMonocleNavHostViewModel
import com.money.monocle.ui.screens.home.AddRecordScreen
import com.money.monocle.userId
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@UninstallModules(RecordModule::class, NavHostModule::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AddRecordUiTests {
    private lateinit var navController: TestNavHostController

    @get: Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()
    @get: Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Module
    @InstallIn(SingletonComponent::class)
    object FakeRecordModule {
        @Provides
        fun provideAddRecordRepository(): AddRecordRepository {
            val auth = mockk<FirebaseAuth> {
                every { currentUser } returns mockk<FirebaseUser> {
                    every { uid } returns userId
                }
            }
            val firestore = mockk<FirebaseFirestore> {
                every { collection(userId).document("records").set(any<Record>()) } returns mockTask()
            }
            return AddRecordRepository(auth, firestore)
        }
    }

    @Module
    @InstallIn(SingletonComponent::class)
    class FakeNavHostModule {
        @Provides
        fun provideCustomAuthStateListener(): CustomAuthStateListener {
            val auth = mockk<FirebaseAuth> {
                every { currentUser } returns mockk<FirebaseUser> {
                    every { uid } returns userId
                }
            }
         return CustomAuthStateListener(auth)
        }
    }

    @Before
    fun setUpNavHost() {
        hiltRule.inject()
        composeRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            //AddRecordScreen(onNavigateBack = {  }, isExpense = true)
            MoneyMonocleNavHost(onError = {}, navController = navController)
        }
    }

    @Test
    fun test() {

    }
}