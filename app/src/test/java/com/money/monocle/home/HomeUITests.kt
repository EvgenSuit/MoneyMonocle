
package com.money.monocle.home

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.BalanceListener
import com.money.monocle.StatsListener
import com.money.monocle.R
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.data.simpleCurrencyMapper
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.home.HomeRepository
import com.money.monocle.domain.home.WelcomeRepository
import com.money.monocle.getInt
import com.money.monocle.getString
import com.money.monocle.mockAuth
import com.money.monocle.printToLog
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
import org.junit.After
import org.junit.Rule
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class HomeUITests {
    @get:Rule
    val composeRule = createComposeRule()
    private val isAccountLoadedSlot = slot<Boolean>()
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private val balanceListener: BalanceListener = slot()
    private val statsListener: StatsListener = slot()

    @Before
    fun init() {
        auth = mockAuth()
        mockFirestore()
        dataStoreManager = mockDataStoreManager(isAccountLoadedSlot)
    }
    @After
    fun clean() = unmockkAll()
    private fun mockFirestore() {
        firestore = mockk {
            every { collection("data").document(userId).collection("balance")
                .addSnapshotListener(capture(balanceListener))} returns mockk<ListenerRegistration>()
            every { collection("data").document(userId).collection("balance")
                .addSnapshotListener(capture(balanceListener)).remove() } returns Unit
            every { collection("data").document(userId).collection("records").whereGreaterThan("timestamp", any())
                .addSnapshotListener(capture(statsListener))} returns mockk<ListenerRegistration>()
            every { collection("data").document(userId).collection("records").whereGreaterThan("timestamp", any())
                .addSnapshotListener(capture(statsListener)).remove() } returns Unit
        }
    }

    @Test
    fun testAccountState_accountDeleted_signOut() = runTest {
        val homeRepository = HomeRepository(auth, firestore.collection("data"), dataStoreManager)
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
            balanceListener.captured.onEvent(mockedSnapshot, null)
            onNodeWithText(getString(R.string.welcome)).assertIsNotDisplayed()
            onNodeWithText(getString(R.string.hello) + ", $username").assertIsNotDisplayed()
        }
    }
    @Test
    fun testAccountState_newAccount_showWelcomeScreen() = runTest {
        val homeRepository = HomeRepository(auth, firestore.collection("data"), dataStoreManager)
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
            balanceListener.captured.onEvent(mockedSnapshot, null)
            waitForIdle()
            onNodeWithText(getString(R.string.welcome)).assertIsDisplayed()
        }
    }

    @Test
    fun welcomeScreen_testTextField() = runTest {
        val homeRepository = HomeRepository(auth, firestore.collection("data"), dataStoreManager)
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
            balanceListener.captured.onEvent(mockedSnapshot, null)
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
        val homeRepository = HomeRepository(auth, firestore.collection("data"), dataStoreManager)
        every { firestore.collection("data").document(userId)
            .collection("balance").document("balance")
            .set(Balance(CurrencyEnum.EUR.ordinal, currentBalance)) }
        val welcomeRepository = WelcomeRepository(auth, firestore.collection("data"))
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
            balanceListener.captured.onEvent(mockedSnapshot, null)
            waitForIdle()
            onNodeWithText(getString(R.string.welcome)).assertIsDisplayed()
            onNodeWithTag("Welcome screen text field").performTextInput(currentBalance.toString())
            onNodeWithTag("Welcome screen text field").assertTextEquals(currentBalance.toString())
            onNodeWithTag("Welcome screen submit button").performClick()
            balanceListener.captured.onEvent(mockedSnapshot2, null)
            // wait for LaunchedEffect to finish executing
            waitForIdle()
            onNodeWithText(getString(R.string.hello) + ", $username").assertIsDisplayed()
        }
    }

    @Test
    fun testPieChart_notEmpty() = runTest {
        val homeRepository = HomeRepository(auth, firestore.collection("data"), dataStoreManager)
        val currencyString = simpleCurrencyMapper(CurrencyEnum.EUR.ordinal)
        val mockedDocs = listOf(mockk<DocumentSnapshot> {
            every { exists() } returns true
            every { toObject(Balance::class.java) } returns Balance(CurrencyEnum.EUR.ordinal)
        })
        val mockedSnapshot = mockk<QuerySnapshot> {
            every { isEmpty } returns false
            every { documents } returns mockedDocs
        }
        val viewModel = HomeViewModel(homeRepository, mockk<WelcomeRepository>(), CoroutineScopeProvider(this))
        composeRule.apply {
            setContent {
                HomeScreen(onNavigateToAddRecord = {_, _ -> },
                    onNavigateToHistory = {},
                    onError = {}, viewModel = viewModel)
            }
            balanceListener.captured.onEvent(mockedSnapshot, null)
            waitForIdle()
            val records = List(10) {
                mockk<DocumentSnapshot> {
                    every { getDouble("amount") } returns it.toDouble()
                    every { getBoolean("expense") } returns (it % 2 == 0)
                }
            }
            val statsQuery = mockk<QuerySnapshot> {
                every { isEmpty } returns false
                every { documents } returns records
            }
            val totalSpent = statsQuery.documents.filter { it.getBoolean("expense") == true }
                .sumOf { it.getDouble("amount") ?: 0.0 }.toFloat()
            val totalEarned = statsQuery.documents.filter { it.getBoolean("expense") == false }
                .sumOf { it.getDouble("amount") ?: 0.0 }.toFloat()
            statsListener.captured.onEvent(statsQuery, null)
            advanceUntilIdle()
            waitUntilExactlyOneExists(hasTestTag("PieChart"))
            onNodeWithTag("${getString(R.string.earned)}: $totalEarned$currencyString").performScrollTo().assertIsDisplayed()
            onNodeWithTag("${getString(R.string.spent)}: $totalSpent$currencyString").performScrollTo().assertIsDisplayed()
        }
    }
    @Test
    fun testPieChart_isEmpty() = runTest {
        val homeRepository = HomeRepository(auth, firestore.collection("data"), dataStoreManager)
        val mockedDocs = listOf(mockk<DocumentSnapshot> {
            every { exists() } returns true
            every { toObject(Balance::class.java) } returns Balance(CurrencyEnum.EUR.ordinal)
        })
        val mockedSnapshot = mockk<QuerySnapshot> {
            every { isEmpty } returns false
            every { documents } returns mockedDocs
        }
        val viewModel = HomeViewModel(homeRepository, mockk<WelcomeRepository>(), CoroutineScopeProvider(this))
        composeRule.apply {
            setContent {
                HomeScreen(onNavigateToAddRecord = {_, _ -> },
                    onNavigateToHistory = {},
                    onError = {}, viewModel = viewModel)
            }
            balanceListener.captured.onEvent(mockedSnapshot, null)
            waitForIdle()
            val statsQuery = mockk<QuerySnapshot> {
                every { isEmpty } returns false
                every { documents } returns listOf()
            }
            statsListener.captured.onEvent(statsQuery, null)
            advanceUntilIdle()
            waitUntilExactlyOneExists(hasTestTag("PieChart"))
            onNodeWithText(getString(R.string.nothing_to_show)).isDisplayed()
        }
    }
}
