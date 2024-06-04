package com.money.monocle.nav

import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.MainActivity
import com.money.monocle.MoneyMonocleNavHost
import com.money.monocle.R
import com.money.monocle.Screen
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.domain.auth.CustomAuthStateListener
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.home.HomeRepository
import com.money.monocle.domain.home.WelcomeRepository
import com.money.monocle.domain.settings.SettingsRepository
import com.money.monocle.getString
import com.money.monocle.modules.AuthStateListener
import com.money.monocle.modules.HomeModule
import com.money.monocle.modules.SettingsModule
import com.money.monocle.userId
import com.money.monocle.username
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import javax.inject.Singleton

@UninstallModules(AuthStateListener::class, SettingsModule::class, HomeModule::class)
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class SettingsTests {
    @get: Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    @get: Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()
    @Inject
    lateinit var firestoreListener: CapturingSlot<EventListener<QuerySnapshot>>
    private lateinit var navController: NavHostController
    @Module
    @InstallIn(SingletonComponent::class)
    object FakeModule {
        @Provides
        @Singleton
        fun provideFirestoreEventListener(): CapturingSlot<EventListener<QuerySnapshot>> = slot<EventListener<QuerySnapshot>>()
        @Provides
        fun provideFakeSettingsRepository(dataStoreManager: DataStoreManager): SettingsRepository
                = SettingsRepository(mockk{
            every { signOut() } just Runs
        }, dataStoreManager)

        private val auth = mockk<FirebaseAuth> {
            every { currentUser } returns mockk<FirebaseUser>{
                every { uid } returns userId
                every { displayName } returns username
            }
        }
        @Provides
        fun provideCustomAuthStateListener(): CustomAuthStateListener {
            every { auth.addAuthStateListener(any()) } just Runs
            every { auth.removeAuthStateListener(any()) } just Runs
            return CustomAuthStateListener(auth)
        }
        @Provides
        fun provideFakeHomeRepository(listenerSlot: CapturingSlot<EventListener<QuerySnapshot>>): HomeRepository {
            val firestore = mockk<FirebaseFirestore> {
                every { collection("data").document(userId).collection("balance")
                    .addSnapshotListener(capture(listenerSlot))} returns mockk<ListenerRegistration>()
                every { collection("data").document(userId).collection("balance")
                    .addSnapshotListener(capture(listenerSlot)).remove() } returns Unit
            }
            return HomeRepository(auth, firestore)
        }
        @Provides
        fun provideFakeWelcomeRepository(): WelcomeRepository {
            return mockk(relaxed = true)
        }
    }
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
    @OptIn(ExperimentalTestApi::class)
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
            firestoreListener.captured.onEvent(mockedSnapshot, null)
            waitForIdle()
            waitUntilAtLeastOneExists(hasText("${getString(R.string.hello)}, $username"))
            onNodeWithTag(Screen.Settings.route).performClick()

        }
    }
}