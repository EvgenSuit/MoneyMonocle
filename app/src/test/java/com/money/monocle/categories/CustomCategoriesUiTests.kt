package com.money.monocle.categories

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.money.monocle.BaseTestClass
import com.money.monocle.R
import com.money.monocle.assertSnackbarIsNotDisplayed
import com.money.monocle.assertSnackbarTextEquals
import com.money.monocle.data.Category
import com.money.monocle.domain.record.CustomCategoriesRepository
import com.money.monocle.getString
import com.money.monocle.mockAuth
import com.money.monocle.setContentWithSnackbar
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.record.CustomCategoriesViewModel
import com.money.monocle.ui.presentation.record.currentCategories
import com.money.monocle.ui.presentation.record.isIncome
import com.money.monocle.ui.screens.record.CustomCategoriesScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CustomCategoriesUiTests: BaseTestClass() {
    @get:Rule
    val composeRule = createComposeRule()
    private val limit = 3
    lateinit var viewModel: CustomCategoriesViewModel

    @Before
    fun init() {
        auth = mockAuth()
        mockViewModel()
    }
    private fun mockViewModel(
        empty: Boolean = false,
        fetchException: Exception? = null,
                              deletionException: Exception? = null,
                              nameChangeException: Exception? = null) {
        firestore = mockCategoriesFirestore(limit,
            empty = empty,
            fetchException = fetchException,
            deletionException = deletionException,
            changeException = nameChangeException)
        viewModel = CustomCategoriesViewModel(
            CustomCategoriesRepository(limit, auth, firestore.collection("data")),
            CoroutineScopeProvider(testScope)
        )
    }
    private fun customSetContent(onNavigateBack: () -> Unit = {}) = composeRule.setContentWithSnackbar(snackbarScope) {
        CustomCategoriesScreen(viewModel, onNavigateBack)
    }

    @Test
    fun incomeCategoriesFetch_success() = testScope.runTest {
        composeRule.apply {
            customSetContent()
            onNodeWithText(getString(R.string.income)).assertIsSelected()
            fetchCategories_assertSizeIsCorrect()
            onNodeWithContentDescription(customIncomeCategories[0].id).assertIsDisplayed()
        }
    }

    @Test
    fun incomeCategoriesFetch_success_isEmpty() = testScope.runTest {
        mockViewModel(empty = true)
        composeRule.apply {
            customSetContent()
            onNodeWithText(getString(R.string.income)).assertIsSelected()
            waitForIdle()
            advanceUntilIdle()
            onNodeWithContentDescription(customIncomeCategories[0].id).assertIsNotDisplayed()
            onNodeWithText(getString(R.string.nothing_to_show)).assertIsDisplayed()
        }
    }
    @Test
    fun incomeCategoriesFetch_failure() = testScope.runTest {
        mockViewModel(fetchException = exception)
        composeRule.apply {
            customSetContent()
            onNodeWithText(getString(R.string.income)).assertIsSelected()
            fetchCategories_assertEmpty()
            assertSnackbarTextEquals(snackbarScope, exception.message!!)
        }
    }
    @Test
    fun expenseCategoriesFetch_success() = testScope.runTest {
        composeRule.apply {
            customSetContent()
            selectExpense()
            fetchCategories_assertSizeIsCorrect()
            onNodeWithContentDescription(customExpenseCategories[0].id).assertIsDisplayed()
        }
    }
    @Test
    fun expenseCategoriesFetch_failure() = testScope.runTest {
        mockViewModel(fetchException = exception)
        composeRule.apply {
            customSetContent()
            selectExpense()
            fetchCategories_assertEmpty()
            assertSnackbarTextEquals(snackbarScope, exception.message!!)
        }
    }

    @Test
    fun performCategoriesSwitch_success() = testScope.runTest {
        composeRule.apply {
            customSetContent()
            fetchCategories_assertSizeIsCorrect()
            selectExpense()
            fetchCategories_assertSizeIsCorrect()
            assertSnackbarIsNotDisplayed(snackbarScope)
        }
    }

    @Test
    fun onIncomeCategorySelected_isShownCorrectly() = testScope.runTest {
        val category = customIncomeCategories[0]
        composeRule.apply {
            customSetContent()
            fetchCategories_assertSizeIsCorrect()
            selectCategory(category)
        }
    }

    @Test
    fun onExpenseCategorySelected_isShownCorrectly() = testScope.runTest {
        val category = customExpenseCategories[0]
        composeRule.apply {
            customSetContent()
            selectExpense()
            fetchCategories_assertSizeIsCorrect()
            selectCategory(category)
        }
    }

    @Test
    fun onIncomeCategoryDeleted_success_categoryDoesNotExist() = testScope.runTest {
        val category = customIncomeCategories[0]
        composeRule.apply {
            customSetContent()
            fetchCategories_assertSizeIsCorrect()
            selectCategory(category)
            onNodeWithContentDescription(Icons.Filled.Delete.name).performClick()
            onNodeWithText(getString(R.string.progress)).assertIsDisplayed()
            onNodeWithContentDescription(Icons.Filled.Edit.name).assertIsNotDisplayed()
            advanceUntilIdle()
            assertSnackbarIsNotDisplayed(snackbarScope)
            onNodeWithContentDescription(Icons.Filled.Edit.name).assertIsNotDisplayed()
            onNodeWithContentDescription(category.id).assertDoesNotExist()
        }
    }

    @Test
    fun onExpenseCategoryDeleted_success_categoryDoesNotExist() = testScope.runTest {
        val category = customExpenseCategories[0]
        composeRule.apply {
            customSetContent()
            selectExpense()
            fetchCategories_assertSizeIsCorrect()
            selectCategory(category)
            onNodeWithContentDescription(Icons.Filled.Delete.name).performClick()
            onNodeWithText(getString(R.string.progress)).assertIsDisplayed()
            advanceUntilIdle()
            onNodeWithContentDescription(category.id).assertDoesNotExist()
        }
    }

    @Test
    fun onIncomeCategoryDeleted_failure() = testScope.runTest {
        val category = customIncomeCategories[0]
        mockViewModel(deletionException = exception)
        composeRule.apply {
            customSetContent()
            fetchCategories_assertSizeIsCorrect()
            selectCategory(category)
            onNodeWithContentDescription(Icons.Filled.Delete.name).performClick()
            onNodeWithText(getString(R.string.progress)).assertIsDisplayed()
            advanceUntilIdle()
            assertSnackbarTextEquals(snackbarScope, exception.message!!)
        }
    }

    @Test
    fun onExpenseCategoryDeleted_failure() = testScope.runTest {
        val category = customExpenseCategories[0]
        mockViewModel(deletionException = exception)
        composeRule.apply {
            customSetContent()
            selectExpense()
            fetchCategories_assertSizeIsCorrect()
            selectCategory(category)
            onNodeWithContentDescription(Icons.Filled.Delete.name).performClick()
            onNodeWithText(getString(R.string.progress)).assertIsDisplayed()
            advanceUntilIdle()
            assertSnackbarTextEquals(snackbarScope, exception.message!!)
        }
    }

    @Test
    fun onIncomeCategoryNameChange_success() = testScope.runTest {
        val category = customIncomeCategories[0]
        val newName = "new name"
        composeRule.apply {
            customSetContent()
            fetchCategories_assertSizeIsCorrect()
            selectCategory(category)

            onNodeWithContentDescription(Icons.Filled.Edit.name).performClick()
            onNodeWithTag(getString(R.string.text_field)).assertIsFocused().performTextReplacement(newName)
            onNodeWithText(getString(R.string.ok)).performClick().assertIsNotEnabled()
            advanceUntilIdle()
            assertSnackbarIsNotDisplayed(snackbarScope)

            onNodeWithContentDescription(Icons.Filled.Edit.name).assertIsDisplayed()
            onNodeWithTag(newName, useUnmergedTree = true).assertIsDisplayed()

            onNodeWithTag(getString(R.string.bottom_sheet)).performTouchInput { swipeDown() }
            selectCategory(category.copy(name = newName))
            onNodeWithContentDescription(Icons.Filled.Edit.name).assertIsDisplayed()
        }
    }
    @Test
    fun onIncomeCategoryNameChange_failure() = testScope.runTest {
        val category = customIncomeCategories[0]
        val newName = "new name"
        mockViewModel(nameChangeException = exception)
        composeRule.apply {
            customSetContent()
            fetchCategories_assertSizeIsCorrect()
            selectCategory(category)

            onNodeWithContentDescription(Icons.Filled.Edit.name).performClick()
            onNodeWithTag(getString(R.string.text_field)).assertIsFocused().performTextReplacement(newName)
            onNodeWithText(getString(R.string.ok)).performClick().assertIsNotEnabled()
            advanceUntilIdle()

            assertSnackbarTextEquals(snackbarScope, exception.message!!)
            onNodeWithTag(getString(R.string.text_field)).assertIsDisplayed()
            onNodeWithTag(newName, useUnmergedTree = true).assertIsNotDisplayed()

            onNodeWithTag(getString(R.string.bottom_sheet)).performTouchInput { swipeDown() }
            selectCategory(category)
            onNodeWithContentDescription(Icons.Filled.Edit.name).assertIsDisplayed()
        }
    }

    @Test
    fun onExpenseCategoryNameChange_success() = testScope.runTest {
        val category = customExpenseCategories[0]
        val newName = "new name"
        composeRule.apply {
            customSetContent()
            selectExpense()
            fetchCategories_assertSizeIsCorrect()
            selectCategory(category)

            onNodeWithContentDescription(Icons.Filled.Edit.name).performClick()
            onNodeWithTag(getString(R.string.text_field)).assertIsFocused().performTextReplacement(newName)
            onNodeWithText(getString(R.string.ok)).performClick().assertIsNotEnabled()
            advanceUntilIdle()
            assertSnackbarIsNotDisplayed(snackbarScope)

            onNodeWithContentDescription(Icons.Filled.Edit.name).assertIsDisplayed().performClick()
            onNodeWithTag(getString(R.string.text_field)).assertIsFocused().assertTextEquals("")
            onNodeWithTag(newName, useUnmergedTree = true).assertIsDisplayed()

            onNodeWithTag(getString(R.string.bottom_sheet)).performTouchInput { swipeDown() }
            selectCategory(category.copy(name = newName))
            onNodeWithContentDescription(Icons.Filled.Edit.name).assertIsDisplayed().performClick()
            onNodeWithTag(getString(R.string.text_field)).assertIsFocused().assertTextEquals("")
        }
    }

    @Test
    fun onExpenseCategoryNameChange_failure() = testScope.runTest {
        val category = customExpenseCategories[0]
        val newName = "new name"
        mockViewModel(nameChangeException = exception)
        composeRule.apply {
            customSetContent()
            selectExpense()
            fetchCategories_assertSizeIsCorrect()
            selectCategory(category)

            onNodeWithContentDescription(Icons.Filled.Edit.name).performClick()
            onNodeWithTag(getString(R.string.text_field)).assertIsFocused().performTextReplacement(newName)
            onNodeWithText(getString(R.string.ok)).performClick().assertIsNotEnabled()
            advanceUntilIdle()
            assertSnackbarTextEquals(snackbarScope, exception.message!!)

            onNodeWithTag(getString(R.string.text_field)).assertIsDisplayed()
            onNodeWithTag(newName, useUnmergedTree = true).assertIsNotDisplayed()

            onNodeWithTag(getString(R.string.bottom_sheet)).performTouchInput { swipeDown() }
            selectCategory(category)
            onNodeWithContentDescription(Icons.Filled.Edit.name).assertIsDisplayed()
        }
    }
    @Test
    fun onCategoriesDeleted_newFetched() = testScope.runTest {
        val iters = limit+2
        composeRule.apply {
            customSetContent()
            fetchCategories_assertSizeIsCorrect()
            val initSize = viewModel.uiState.value.incomeCategoriesState.categories.size
            // delete enough items such that loading of new ones begins
            for (category in customIncomeCategories.subList(0, iters)) {
                selectCategory(category)
                onNodeWithContentDescription(Icons.Filled.Delete.name).performClick()
                advanceUntilIdle()

                assertSnackbarIsNotDisplayed(snackbarScope)
            }
            while (viewModel.uiState.value.currentCategories().size <= initSize) {
                waitForIdle()
                advanceUntilIdle()
            }
            assertTrue(viewModel.uiState.value.currentCategories().size >= initSize )
        }
    }

    private fun ComposeContentTestRule.selectCategory(category: Category) {
        onNodeWithContentDescription(category.id).performScrollTo().assertIsDisplayed().performClick().assertIsSelected()
        onNodeWithTag(getString(R.string.bottom_sheet)).assertIsDisplayed()

        onNodeWithContentDescription(Icons.Filled.Edit.name).assertIsDisplayed()
        onNodeWithTag(category.name, useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag(getString(R.string.text_field)).assertIsNotDisplayed()
        onNodeWithContentDescription(Icons.Filled.Delete.name).assertIsDisplayed()
    }
    private fun TestScope.fetchCategories_assertSizeIsCorrect(iters: Int = 3) {
        composeRule.apply {
            for (i in 0 .. iters) {
                waitForIdle()
                advanceUntilIdle()
                assertSnackbarIsNotDisplayed(snackbarScope)
            }
        }
        val state = viewModel.uiState.value
        val categories = state.currentCategories()
        assertEquals(limit*iters, categories.size)
    }
    private fun TestScope.fetchCategories_assertEmpty() {
        composeRule.apply {
            waitForIdle()
            advanceUntilIdle()
            val state = viewModel.uiState.value
            val categories = if (state.isIncome()) state.incomeCategoriesState.categories else state.expenseCategoriesState.categories
            assertEquals(0, categories.size)
        }
    }

    private fun ComposeContentTestRule.selectExpense() {
        onNodeWithText(getString(R.string.expense)).performClick().assertIsSelected()
    }
}