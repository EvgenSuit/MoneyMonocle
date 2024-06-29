package com.money.monocle.record

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.money.monocle.BaseTestClass
import com.money.monocle.R
import com.money.monocle.data.CustomExpenseCategoriesIds
import com.money.monocle.data.CustomIncomeCategoriesIds
import com.money.monocle.data.Record
import com.money.monocle.domain.record.AddCategoryRepository
import com.money.monocle.getString
import com.money.monocle.mockAuth
import com.money.monocle.setContentWithSnackbar
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.record.AddCategoryViewModel
import com.money.monocle.ui.screens.record.AddCategoryScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AddCategoryUiTests: BaseTestClass() {
    @get: Rule
    val composeRule = createComposeRule()
    private lateinit var viewModel: AddCategoryViewModel
    private val maxNameLength = 30
    @Before
    fun setup() {
        auth = mockAuth()
        firestore = mockCategoryFirestore()
        createViewModel()

    }

    private fun createViewModel(isExpense: Boolean = Record().expense) {
        val repository = AddCategoryRepository(auth, firestore.collection("data"))
        viewModel = AddCategoryViewModel(repository,
            CoroutineScopeProvider(testScope),
            mockCategorySavedStateHandle(isExpense), maxNameLength
        )
    }
    private fun customSetContent(onNavigateBack: () -> Unit = {}) = composeRule.setContentWithSnackbar(snackbarScope) {
        AddCategoryScreen(viewModel, onNavigateBack)
    }

    @Test
    fun onAddExpenseCategory_success_navigateBack() = testScope.runTest {
        createViewModel(true)
        val categoryId = CustomExpenseCategoriesIds.DONATION.name
        var navigatedBack = false
        composeRule.apply {
            customSetContent { navigatedBack = true }
            onNodeWithTag(getString(R.string.other)).performScrollTo()
            onNodeWithTag(categoryId).performScrollTo().performClick()

            onNodeWithTag("NameField").performTextReplacement("my name")
            onNodeWithText(getString(R.string.add)).performClick()

            onNodeWithTag("NameField").assertIsNotEnabled()
            onNodeWithText(getString(R.string.add)).assertIsNotEnabled()

            onNodeWithContentDescription("BackButton").performClick()

            onNodeWithText(getString(R.string.give_a_name_to_category)).assertIsNotDisplayed()
            onNodeWithTag(getString(R.string.other)).performScrollTo()
            onNodeWithTag(categoryId).performScrollTo().assertIsNotEnabled()

            advanceUntilIdle()
            waitForIdle()
            assertTrue(navigatedBack)
        }
    }
    @Test
    fun onExpenseCategorySelect_showCreateCategoryScreen() {
        createViewModel(true)
        val categoryId = CustomExpenseCategoriesIds.DONATION.name
        composeRule.apply {
            customSetContent {  }
            onNodeWithTag(getString(R.string.other)).performScrollTo()
            onNodeWithTag(categoryId).performScrollTo().performClick()

            onNodeWithText("${getString(R.string.add)} ${getString(R.string.expense)} ${getString(R.string.category)}").assertIsDisplayed()
            onNodeWithText(getString(R.string.give_a_name_to_category)).assertIsDisplayed()
        }
    }
    @Test
    fun onIncomeCategorySelect_showCreateCategoryScreen() {
        val categoryId = CustomIncomeCategoriesIds.CAPITAL.name
        composeRule.apply {
            customSetContent()
            onNodeWithTag(getString(R.string.investment)).performScrollTo()
            onNodeWithText("${getString(R.string.add)} ${getString(R.string.income)} ${getString(R.string.category)}").assertIsDisplayed()
            onNodeWithTag(categoryId).performScrollTo().performClick()
            onNodeWithText(getString(R.string.give_a_name_to_category)).assertIsDisplayed()
        }
    }
    @Test
    fun testNameField() {
        createViewModel(true)
        val categoryId = CustomExpenseCategoriesIds.DONATION.name
        composeRule.apply {
            customSetContent()
            onNodeWithTag(getString(R.string.other)).performScrollTo()
            onNodeWithTag(categoryId).performScrollTo().performClick()

            val field = onNodeWithTag("NameField")
            field.performTextReplacement("my name")
            field.assertTextEquals("my name")

            field.performTextClearance()
            for (v in "m".repeat(maxNameLength)) {
                field.performTextInput(v.toString())
            }
            field.assertTextEquals("m".repeat(maxNameLength-1))
        }
    }
}