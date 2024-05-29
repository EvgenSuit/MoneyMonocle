/*
package com.money.monocle.home

import androidx.compose.ui.test.assertIsDisplayed
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
import com.google.firebase.firestore.getField
import com.money.monocle.R
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.domain.home.HomeRepository
import com.money.monocle.domain.home.WelcomeRepository
import com.money.monocle.getString
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.HomeViewModel
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
    private val listenerSlot = slot<EventListener<DocumentSnapshot>>()

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
            every { collection("users").document(userId)
                .addSnapshotListener(capture(listenerSlot)) } returns mockk<ListenerRegistration>()
        }
    }

    @Test
    fun testAccountState_accountDeleted_signOut() = runTest {
        val homeRepository = HomeRepository(auth, firestore)
        val mockedDocument = mockk<DocumentSnapshot> {
            every { exists() } returns false
        }
        val viewModel = HomeViewModel(homeRepository, mockk<WelcomeRepository>(), CoroutineScopeProvider(this))
        advanceUntilIdle()
        composeRule.apply {
            setContent {
                HomeScreen(onError = {},
                    viewModel = viewModel)
            }
            listenerSlot.captured.onEvent(mockedDocument, null)
            waitForIdle()
            verify { auth.signOut() }
        }
    }
    @Test
    fun testAccountState_newAccount_showWelcomeScreen() = runTest {
        val homeRepository = HomeRepository(auth, firestore.collection("users"))
        val mockedDocument = mockk<DocumentSnapshot> {
            every { exists() } returns true
            every { getField<Int>("currency") } returns -1
        }
        val viewModel = HomeViewModel(homeRepository, mockk<WelcomeRepository>(), CoroutineScopeProvider(this))
        advanceUntilIdle()
        composeRule.apply {
            setContent {
                HomeScreen(onError = {}, viewModel = viewModel)
            }
            listenerSlot.captured.onEvent(mockedDocument, null)
            waitForIdle()
            onNodeWithText(getString(R.string.welcome)).assertIsDisplayed()
        }
    }

    @Test
    fun testAccountState_newAccountOnSubmit_showMainContent() = runTest {
        val currentBalance = 233.4f
        val homeRepository = HomeRepository(auth, firestore.collection("users"))
        every { firestore.collection("users")
            .document(userId).update("currency", CurrencyEnum.EUR) }
        every { firestore.collection("users")
            .document(userId).update("currentBalance", currentBalance) }
        val welcomeRepository = WelcomeRepository(firestore.collection("users").document(userId))
        val mockedDocument = mockk<DocumentSnapshot> {
            every { exists() } returns true
            every { getField<Int>("currency") } returns -1
        }
        val mockedDocument2 = mockk<DocumentSnapshot> {
            every { exists() } returns true
            every { getField<Int>("currency") } returns CurrencyEnum.EUR.ordinal
        }
        val viewModel = HomeViewModel(homeRepository, welcomeRepository, CoroutineScopeProvider(this))
        advanceUntilIdle()
        composeRule.apply {
            setContent {
                HomeScreen(onError = {}, viewModel = viewModel)
            }
            listenerSlot.captured.onEvent(mockedDocument, null)
            waitForIdle()
            onNodeWithText(getString(R.string.welcome)).assertIsDisplayed()
            onNodeWithTag("Welcome screen text field").performTextInput(currentBalance.toString())
            onNodeWithTag("Welcome screen submit button").performClick()
            listenerSlot.captured.onEvent(mockedDocument2, null)
            // wait for LaunchedEffect to finish executing
            waitForIdle()
            onNodeWithText(getString(R.string.hello) + ", $username").assertIsDisplayed()
        }
    }

}*/
