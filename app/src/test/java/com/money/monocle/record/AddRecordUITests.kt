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
import com.money.monocle.R
import com.money.monocle.data.DefaultExpenseCategoriesIds
import com.money.monocle.data.DefaultIncomeCategoriesIds
import com.money.monocle.data.Record
import com.money.monocle.domain.record.AddRecordRepository
import com.money.monocle.domain.useCases.CurrencyFormatValidator
import com.money.monocle.getString
import com.money.monocle.mockAuth
import com.money.monocle.setContentWithSnackbarAndDefaultCategories
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.record.AddRecordViewModel
import com.money.monocle.ui.screens.record.AddRecordScreen
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
        auth = mockAuth()
        firestore = mockRecordFirestore()
        mockViewModel()
    }
    private fun mockViewModel(isExpense: Boolean = Record().isExpense) {
        val repository = AddRecordRepository(auth, firestore)
        viewModel = AddRecordViewModel(repository, CurrencyFormatValidator(6),
            scopeProvider, mockRecordSavedStateHandle(isExpense = isExpense)
        )
    }

    @Test
    fun addRecord_isExpense_success() = runTest {
        mockViewModel(true)
        val date = formatter.format(Instant.now().toEpochMilli())
        var navigatedBack = false
        scopeProvider = CoroutineScopeProvider(this)
        composeRule.apply {
            setContentWithSnackbarAndDefaultCategories(snackbarScope) {
                AddRecordScreen(onNavigateBack = { navigatedBack = true },
                    onAddCategory = {},
                    viewModel = viewModel)
            }
            onNodeWithText("Add Expense").assertIsDisplayed()
            onNodeWithTag("Expense grid").assertIsDisplayed()

            onNodeWithContentDescription(DefaultExpenseCategoriesIds.INSURANCE.name.lowercase(), useUnmergedTree = true).performClick()
            onNodeWithText(date).assertIsDisplayed()
            onNodeWithTag("addRecordTextField").performTextReplacement("9")
            onNodeWithText(getString(R.string.add)).performScrollTo().assertIsEnabled().performClick()
            advanceUntilIdle()
            waitForIdle()
        }
        assertTrue(navigatedBack)
    }

    @Test
    fun addRecord_isExpense_failure() = runTest {
        val date = formatter.format(Instant.now().toEpochMilli())
        var navigatedBack = false
        auth = mockAuth()
        firestore = mockRecordFirestore(exception = Exception("exception"))
        mockViewModel(true)
        scopeProvider = CoroutineScopeProvider(this)
        composeRule.apply {
            setContentWithSnackbarAndDefaultCategories(snackbarScope) {
                AddRecordScreen(onNavigateBack = { navigatedBack = true },
                    onAddCategory = {},
                    viewModel = viewModel)
            }
            onNodeWithText("Add Expense").assertIsDisplayed()
            onNodeWithTag("Expense grid").assertIsDisplayed()

            onNodeWithContentDescription(DefaultExpenseCategoriesIds.INSURANCE.name.lowercase(), useUnmergedTree = true).performClick()
            onNodeWithText(date).assertIsDisplayed()
            onNodeWithTag("addRecordTextField").performTextReplacement("9")
            onNodeWithText(getString(R.string.add)).performScrollTo().assertIsEnabled().performClick()
            advanceUntilIdle()
            waitForIdle()
        }
        assertTrue(!navigatedBack)
    }

    @Test
    fun inputAmount_incorrectInput_addButtonNotEnabled() {
        val errorCases = listOf("-1", "4..", "..", "df", "000", "0")
        composeRule.apply {
            setContentWithSnackbarAndDefaultCategories(snackbarScope) {
                AddRecordScreen(onNavigateBack = {  },
                    onAddCategory = {},
                    viewModel = viewModel)
            }
            onNodeWithContentDescription(DefaultIncomeCategoriesIds.WAGE.name.lowercase()).assertIsEnabled().performClick()
            for (case in errorCases) {
                onNodeWithTag("addRecordTextField").performTextReplacement(case)
                onNodeWithText(getString(R.string.add)).assertIsNotEnabled()
            }
        }
    }

    @Test
    fun inputAmount_correctInput_addButtonEnabled() {
        val successCases = listOf("1", "4.5", "123234", "0.24", "3957385388")
        composeRule.apply {
            setContentWithSnackbarAndDefaultCategories(snackbarScope) {
                AddRecordScreen(onNavigateBack = {  },
                    onAddCategory = {},
                    viewModel = viewModel)
            }
            onNodeWithContentDescription(DefaultIncomeCategoriesIds.WAGE.name.lowercase()).assertIsEnabled().performClick()
            for (case in successCases) {
                onNodeWithTag("addRecordTextField").performTextReplacement(case)
                onNodeWithText(getString(R.string.add)).assertIsEnabled()
            }
            assertEquals(viewModel.recordState.value.amount, "3957385388".substring(0, 6))
            onNodeWithText(getString(R.string.add)).assertIsEnabled()
        }
    }
}