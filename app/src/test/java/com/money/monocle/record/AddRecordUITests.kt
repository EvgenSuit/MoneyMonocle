package com.money.monocle.record

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ibm.icu.text.SimpleDateFormat
import com.money.monocle.BaseTestClass
import com.money.monocle.data.Record
import com.money.monocle.domain.record.AddRecordRepository
import com.money.monocle.getString
import com.money.monocle.mockTask
import com.money.monocle.setContentWithSnackbar
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.record.AddRecordViewModel
import com.money.monocle.ui.screens.home.AddRecordScreen
import com.money.monocle.ui.screens.home.expenseIcons
import com.money.monocle.userId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLog
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AddRecordUITests: BaseTestClass() {
    @get: Rule
    val composeRule = createComposeRule()
    private val formatter = SimpleDateFormat("EEEE, MMMM d, yyyy")
    private var scopeProvider: CoroutineScopeProvider = CoroutineScopeProvider()
    private lateinit var viewModel: AddRecordViewModel

    @Before
    fun init() {
        ShadowLog.stream = System.out
        mockAuth()
        mockFirestore()
        mockViewModel()
    }
    private fun mockAuth() {
        auth = mockk {
            every { currentUser?.uid } returns userId
        }
    }

    private fun mockFirestore(exception: Exception? = null) {
        firestore = mockk {
            every { collection("data").document(userId).collection("records").document(any<String>())
                .set(any<Record>()) } returns mockTask(exception = exception)
            every { collection("data").document(userId).collection("balance")
                .document("balance").update("balance", any()) } returns mockTask(
                exception = exception
            )
        }
    }
    private fun mockViewModel() {
        val repository = AddRecordRepository(auth, firestore)
        viewModel = AddRecordViewModel(repository, scopeProvider)
    }

    @Test
    fun addRecord_isExpense_success() = runTest {
        val date = formatter.format(Instant.now().toEpochMilli())
        var navigatedBack = false
        scopeProvider = CoroutineScopeProvider(this)
        composeRule.apply {
            setContentWithSnackbar(snackbarScope) {
                AddRecordScreen(onNavigateBack = { navigatedBack = true }, isExpense = true, currency = "$", viewModel = viewModel)
            }
            onNodeWithText("Add Expense").assertIsDisplayed()
            onNodeWithTag("Expense grid").assertIsDisplayed()

            onNodeWithContentDescription(getString(expenseIcons.keys.first())).performClick()
            onNodeWithText(date).assertIsDisplayed()
            onNodeWithTag("addRecordTextField").performTextReplacement("9")
            onNodeWithTag("AddRecordButton", true).performScrollTo().assertIsEnabled().performClick()
            advanceUntilIdle()
            waitForIdle()
        }
        assertTrue(navigatedBack)
    }

    @Test
    fun addRecord_isExpense_failure() = runTest {
        val date = formatter.format(Instant.now().toEpochMilli())
        var navigatedBack = false
        mockAuth()
        mockFirestore(Exception("exception"))
        mockViewModel()
        scopeProvider = CoroutineScopeProvider(this)
        composeRule.apply {
            setContentWithSnackbar(snackbarScope) {
                AddRecordScreen(onNavigateBack = { navigatedBack = true }, isExpense = true, currency = "$", viewModel = viewModel)
            }
            onNodeWithText("Add Expense").assertIsDisplayed()
            onNodeWithTag("Expense grid").assertIsDisplayed()

            onNodeWithContentDescription(getString(expenseIcons.keys.first())).performClick()
            onNodeWithText(date).assertIsDisplayed()
            onNodeWithTag("addRecordTextField").performTextReplacement("9")
            onNodeWithTag("AddRecordButton", true).performScrollTo().assertIsEnabled().performClick()
            advanceUntilIdle()
            waitForIdle()
        }
        assertTrue(!navigatedBack)
    }

    @Test
    fun inputAmount_incorrectInput_addButtonNotEnabled() {
        composeRule.apply {
            setContentWithSnackbar(snackbarScope) {
                AddRecordScreen(onNavigateBack = {  }, isExpense = true, currency = "$", viewModel = viewModel)
            }
            onNodeWithContentDescription(getString(expenseIcons.keys.first())).assertIsEnabled().performClick()
            onNodeWithTag("addRecordTextField").performTextReplacement("-1")
            onNodeWithTag("AddRecordButton").assertIsNotEnabled()
            onNodeWithTag("addRecordTextField").performTextReplacement("4..")
            onNodeWithTag("AddRecordButton").assertIsNotEnabled()
            onNodeWithTag("addRecordTextField").performTextReplacement("..")
            onNodeWithTag("AddRecordButton").assertIsNotEnabled()
            onNodeWithTag("addRecordTextField").performTextReplacement("df")
            onNodeWithTag("AddRecordButton").assertIsNotEnabled()
            onNodeWithTag("addRecordTextField").performTextReplacement("000")
            onNodeWithTag("AddRecordButton").assertIsNotEnabled()
            onNodeWithTag("addRecordTextField").performTextReplacement("0")
            onNodeWithTag("AddRecordButton").assertIsNotEnabled()
        }
    }

    @Test
    fun inputAmount_correctInput_addButtonEnabled() {
        composeRule.apply {
            setContentWithSnackbar(snackbarScope) {
                AddRecordScreen(onNavigateBack = {  }, isExpense = true, currency = "$", viewModel = viewModel)
            }
            onNodeWithContentDescription(getString(expenseIcons.keys.first())).assertIsEnabled().performClick()
            onNodeWithTag("addRecordTextField").performTextReplacement("1")
            onNodeWithTag("AddRecordButton").assertIsEnabled()

            onNodeWithTag("addRecordTextField").performTextReplacement("4.5")
            onNodeWithTag("AddRecordButton").assertIsEnabled()

            onNodeWithTag("addRecordTextField").performTextReplacement("123234")
            onNodeWithTag("AddRecordButton").assertIsEnabled()

            onNodeWithTag("addRecordTextField").performTextReplacement("0.24")
            onNodeWithTag("AddRecordButton").assertIsEnabled()

            onNodeWithTag("addRecordTextField").performTextReplacement("3957385388")
            assertEquals(viewModel.recordState.value.amount, "3957385388".substring(0, 6))
            onNodeWithTag("AddRecordButton").assertIsEnabled()
        }
    }
}