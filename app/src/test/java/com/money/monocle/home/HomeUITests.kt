
package com.money.monocle.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.R
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.domain.home.HomeRepository
import com.money.monocle.domain.home.WelcomeRepository
import com.money.monocle.getInt
import com.money.monocle.getString
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.home.HomeViewModel
import com.money.monocle.ui.screens.home.HomeScreen
import com.money.monocle.userId
import com.money.monocle.username
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Rule
import org.junit.runner.RunWith


@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class HomeUITests {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val listenerSlot = slot<EventListener<QuerySnapshot>>()

    @Before
    fun init() {
        mockAuth()
        mockFirestore()
    }
    private fun mockAuth() {
        auth = mockk {
            every { currentUser?.uid } returns userId
            every { currentUser } returns mockk<FirebaseUser>{
                every { uid } returns userId
                every { displayName } returns username
                every { signOut() } returns Unit
            }
        }
    }
    private fun mockFirestore() {
        firestore = mockk {
            every { collection("data").document(userId).collection("balance")
                .addSnapshotListener(capture(listenerSlot))} returns mockk<ListenerRegistration>()
            every { collection("data").document(userId).collection("balance")
                .addSnapshotListener(capture(listenerSlot)).remove() } returns Unit
        }
    }

    @Test
    fun testAccountState_accountDeleted_signOut() = runTest {
        val homeRepository = HomeRepository(auth, firestore)
        val mockedSnapshot = mockk<QuerySnapshot> {
            every { isEmpty } returns true
            every { documents } returns listOf()
        }
        val viewModel = HomeViewModel(homeRepository, mockk<WelcomeRepository>(), CoroutineScopeProvider(this))
        advanceUntilIdle()
        composeRule.apply {
            setContent {
                HomeScreen(
                    onNavigateToAddRecord = {_, _ -> },
                    onNavigateToHistory = {},
                    onError = {},
                    viewModel = viewModel)
            }
            listenerSlot.captured.onEvent(mockedSnapshot, null)
            onNodeWithText(getString(R.string.welcome)).assertIsNotDisplayed()
            onNodeWithText(getString(R.string.hello) + ", $username").assertIsNotDisplayed()
        }
    }
    @Test
    fun testAccountState_newAccount_showWelcomeScreen() = runTest {
        val homeRepository = HomeRepository(auth, firestore)
        val mockedDocs = listOf(mockk<DocumentSnapshot> {
            every { toObject(Balance::class.java) } returns Balance(currency = -1)
        })

        val mockedSnapshot = mockk<QuerySnapshot> {
            every { isEmpty } returns false
            every { documents } returns mockedDocs
        }

        val viewModel = HomeViewModel(homeRepository, mockk<WelcomeRepository>(), CoroutineScopeProvider(this))
        advanceUntilIdle()
        composeRule.apply {
            setContent {
                HomeScreen(
                    onNavigateToAddRecord = {_, _ -> },
                    onNavigateToHistory = {},
                    onError = {}, viewModel = viewModel)
            }
            listenerSlot.captured.onEvent(mockedSnapshot, null)
            waitForIdle()
            onNodeWithText(getString(R.string.welcome)).assertIsDisplayed()
        }
    }

    @Test
    fun welcomeScreen_testTextField() = runTest {
        val homeRepository = HomeRepository(auth, firestore)
        val mockedDocs = listOf(mockk<DocumentSnapshot> {
            every { toObject(Balance::class.java) } returns Balance(currency = -1)
        })

        val mockedSnapshot = mockk<QuerySnapshot> {
            every { isEmpty } returns false
            every { documents } returns mockedDocs
        }
        val testValue ="1".repeat(getInt(R.integer.max_init_balance_length)*2)
        val viewModel = HomeViewModel(homeRepository, mockk<WelcomeRepository>(), CoroutineScopeProvider(this))
        advanceUntilIdle()
        composeRule.apply {
            setContent {
                HomeScreen(
                    onNavigateToAddRecord = { _, _ -> },
                    onNavigateToHistory = {},
                    onError = {}, viewModel = viewModel
                )
            }
            listenerSlot.captured.onEvent(mockedSnapshot, null)
            waitForIdle()
            onNodeWithText(getString(R.string.welcome)).assertIsDisplayed()
            for (s in testValue) {
                onNodeWithTag("Welcome screen text field").performTextInput(s.toString())
            }
            onNodeWithTag("Welcome screen text field").assertTextEquals(testValue.substring(0, getInt(R.integer.max_init_balance_length)))
        }
    }

    @Test
    fun testAccountState_newAccountOnSubmit_showMainContent() = runTest {
        val currentBalance = 233f
        val homeRepository = HomeRepository(auth, firestore)
        every { firestore.collection("data").document(userId)
            .collection("balance").document("balance")
            .set(Balance(CurrencyEnum.EUR.ordinal, currentBalance)) }
        val welcomeRepository = WelcomeRepository(auth, firestore)
        val mockedDocs = listOf(mockk<DocumentSnapshot> {
            every { exists() } returns true
            every { toObject(Balance::class.java) } returns Balance(currency = -1)
        })
        val mockedDocs2 = listOf(mockk<DocumentSnapshot> {
            every { exists() } returns true
            every { toObject(Balance::class.java) } returns Balance(currency = CurrencyEnum.EUR.ordinal)
        })
        val mockedSnapshot = mockk<QuerySnapshot> {
            every { isEmpty } returns false
            every { documents } returns mockedDocs
        }
        val mockedSnapshot2 = mockk<QuerySnapshot> {
            every { isEmpty } returns false
            every { documents } returns mockedDocs2
        }
        val viewModel = HomeViewModel(homeRepository, welcomeRepository, CoroutineScopeProvider(this))
        advanceUntilIdle()
        composeRule.apply {
            setContent {
                HomeScreen(onNavigateToAddRecord = {_, _ -> },
                    onNavigateToHistory = {},
                    onError = {}, viewModel = viewModel)
            }
            listenerSlot.captured.onEvent(mockedSnapshot, null)
            waitForIdle()
            onNodeWithText(getString(R.string.welcome)).assertIsDisplayed()
            onNodeWithTag("Welcome screen text field").performTextInput(currentBalance.toString())
            onNodeWithTag("Welcome screen text field").assertTextEquals(currentBalance.toString())
            onNodeWithTag("Welcome screen submit button").performClick()
            listenerSlot.captured.onEvent(mockedSnapshot2, null)
            // wait for LaunchedEffect to finish executing
            waitForIdle()
            onNodeWithText(getString(R.string.hello) + ", $username").assertIsDisplayed()
        }
    }

}
