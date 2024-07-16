
package com.money.monocle.home

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.BalanceListener
import com.money.monocle.BaseTestClass
import com.money.monocle.CorrectAuthData
import com.money.monocle.R
import com.money.monocle.StatsListener
import com.money.monocle.data.AccountName
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.data.simpleCurrencyMapper
import com.money.monocle.domain.home.HomeRepository
import com.money.monocle.domain.home.WelcomeRepository
import com.money.monocle.getInt
import com.money.monocle.getString
import com.money.monocle.mockAuth
import com.money.monocle.setContentWithSnackbar
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.home.HomeViewModel
import com.money.monocle.ui.screens.home.HomeScreen
import com.money.monocle.ui.screens.home.WelcomeScreen
import com.money.monocle.userId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class HomeUITests: BaseTestClass() {
    @get:Rule
    val composeRule = createComposeRule()
    private val isAccountLoadedSlot = slot<Boolean>()
    private val isWelcomeScreenShownSlot = slot<Boolean>()
    private val balanceSlot = slot<Balance>()
    private lateinit var viewModel: HomeViewModel

    private val balanceListener: BalanceListener = slot()
    private val statsListener: StatsListener = slot()

    @Before
    fun init() {
        auth = mockAuth()
        firestore = mockHomeFirestore(balanceListener, statsListener)
        dataStoreManager = mockDataStoreManager(
            isAccountLoadedSlot = isAccountLoadedSlot,
            isWelcomeScreenShownSlot = isWelcomeScreenShownSlot,
            balanceSlot = balanceSlot)
        mockViewModel()
    }

    private fun mockViewModel() {
        val homeRepository = HomeRepository(auth, firestore, dataStoreManager)
        viewModel = HomeViewModel(homeRepository, WelcomeRepository(auth, firestore),
            dataStoreManager, CoroutineScopeProvider(testScope))
    }

    @Test
    fun testAccountState_newAccount_showWelcomeScreen() = testScope.runTest {
        val mockedDocs = listOf(mockk<DocumentSnapshot> {
            every { toObject(Balance::class.java) } returns Balance(currency = -1)
        })

        val mockedSnapshot = mockk<QuerySnapshot> {
            every { isEmpty } returns false
            every { documents } returns mockedDocs
        }
        advanceUntilIdle()
        composeRule.apply {
            setContentWithSnackbar(snackbarScope) {
                HomeScreen(
                    onNavigateToAddRecord = {_ -> },
                    onNavigateToHistory = {},
                      viewModel = viewModel)
            }
            balanceListener.captured.onEvent(mockedSnapshot, null)
            advanceUntilIdle()
            waitForIdle()
            onNodeWithText(getString(R.string.welcome)).assertIsDisplayed()
        }
    }

    @Test
    fun welcomeScreen_testTextField() = testScope.runTest {
        val errorCases = listOf("-1", "4..", "..", "df", "000", "0")
        val longTestValue = "10".repeat(getInt(R.integer.max_amount_length) *2)
        composeRule.apply {
            setContentWithSnackbar(snackbarScope) {
                WelcomeScreen(isSubmitEnabled = true) { _ -> }
            }
            waitForIdle()
            onNodeWithText(getString(R.string.welcome)).assertIsDisplayed()
            val textField = onNodeWithTag(getString(R.string.text_field))
            for(case in errorCases) {
                textField.performTextReplacement(case)
            }
            textField.performTextReplacement(longTestValue)
            textField.assertTextEquals(longTestValue.substring(0, getInt(R.integer.max_amount_length)))
        }
    }

    @Test
    fun testAccountState_newAccountOnSubmit_showMainContent() = testScope.runTest {
        val currentBalance = 233f

        every { firestore.collection(userId).document(AccountName.MAIN.name)
            .collection("balance").document("balance")
            .set(Balance(CurrencyEnum.EUR.ordinal, currentBalance)) }
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
        mockViewModel()
        advanceUntilIdle()
        composeRule.apply {
            setContentWithSnackbar(snackbarScope) {
                HomeScreen(onNavigateToAddRecord = {_ -> },
                    onNavigateToHistory = {},
                      viewModel = viewModel)
            }
            balanceListener.captured.onEvent(mockedSnapshot, null)
            advanceUntilIdle()
            waitForIdle()
            onNodeWithText(getString(R.string.welcome)).assertIsDisplayed()
            onNodeWithTag(getString(R.string.text_field)).performTextInput(currentBalance.toString())
            onNodeWithTag(getString(R.string.text_field)).assertTextEquals(currentBalance.toString())
            onNodeWithText(getString(R.string.submit)).performClick()
            balanceListener.captured.onEvent(mockedSnapshot2, null)
            advanceUntilIdle()
            // wait for LaunchedEffect to finish executing
            waitForIdle()
            onNodeWithText(getString(R.string.hello) + ", ${CorrectAuthData.USERNAME}").assertIsDisplayed()
        }
    }

    @Test
    fun testPieChart_notEmpty() = testScope.runTest {
        val currencyString = simpleCurrencyMapper(CurrencyEnum.EUR.ordinal)
        val mockedDocs = listOf(mockk<DocumentSnapshot> {
            every { exists() } returns true
            every { toObject(Balance::class.java) } returns Balance(CurrencyEnum.EUR.ordinal)
        })
        val mockedSnapshot = mockk<QuerySnapshot> {
            every { isEmpty } returns false
            every { documents } returns mockedDocs
        }
        composeRule.apply {
            setContentWithSnackbar(snackbarScope) {
                HomeScreen(onNavigateToAddRecord = {_ -> },
                    onNavigateToHistory = {},
                      viewModel = viewModel)
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
    fun testPieChart_isEmpty() = testScope.runTest {
        val mockedDocs = listOf(mockk<DocumentSnapshot> {
            every { exists() } returns true
            every { toObject(Balance::class.java) } returns Balance(CurrencyEnum.EUR.ordinal)
        })
        val mockedSnapshot = mockk<QuerySnapshot> {
            every { isEmpty } returns false
            every { documents } returns mockedDocs
        }
        composeRule.apply {
            setContentWithSnackbar(snackbarScope) {
                HomeScreen(onNavigateToAddRecord = {_ -> },
                    onNavigateToHistory = {},
                      viewModel = viewModel)
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
