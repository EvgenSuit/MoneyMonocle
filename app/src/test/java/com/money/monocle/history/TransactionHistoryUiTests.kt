package com.money.monocle.history

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.core.app.FrameMetricsAggregator.ANIMATION_DURATION
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.Query
import com.money.monocle.BaseTestClass
import com.money.monocle.R
import com.money.monocle.assertSnackbarIsNotDisplayed
import com.money.monocle.assertSnackbarTextEquals
import com.money.monocle.data.Record
import com.money.monocle.domain.history.TransactionHistoryRepository
import com.money.monocle.domain.useCases.DateFormatter
import com.money.monocle.getString
import com.money.monocle.mockAuth
import com.money.monocle.setContentWithSnackbar
import com.money.monocle.setContentWithSnackbarAndDefaultCategories
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.history.TransactionHistoryViewModel
import com.money.monocle.ui.presentation.record.currentCategories
import com.money.monocle.ui.screens.history.TransactionHistoryScreen
import com.money.monocle.userId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class TransactionHistoryUiTests: BaseTestClass() {
    private var limit = 3
    private lateinit var viewModel: TransactionHistoryViewModel

    @get: Rule
    val composeRule = createComposeRule()
    @Before
    fun init() {
        auth = mockAuth()
        createViewModel(records)
    }
    private fun createViewModel(records: List<Record>, exception: Exception? = null, empty: Boolean = false) {
        firestore = mockFirestore(limit, records, exception = exception, empty = empty)
        val repository = TransactionHistoryRepository(
            limit = limit,
            auth = auth, firestore = firestore.collection("data"))
        viewModel = TransactionHistoryViewModel(repository, DateFormatter(),
            CoroutineScopeProvider(testScope),
            mockk { every { get<String>("currency") } returns "$"})
    }
    private fun ComposeContentTestRule.customSetContent() {
        setContentWithSnackbarAndDefaultCategories(snackbarScope) {
            TransactionHistoryScreen(
                onBackClick = {  },
                viewModel = viewModel)
        }
    }
    @Test
    fun fetchRecords_success_recordsShown() = testScope.runTest {
        val firstRef = firestore.collection("data").document(userId).collection("records")
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(limit.toLong())
        composeRule.apply {
            customSetContent()
            fetchItems_assertSizeIsCorrect()
            onNodeWithContentDescription(records[0].id).performScrollTo().assertIsDisplayed()
        }
        // verify only one limit call was made
        verify(exactly = 1) { firstRef.get() }
    }
    @Test
    fun fetchRecords_error_snackbarShown() = testScope.runTest {
        val exception = Exception("error")
        createViewModel(records, exception = exception)
        composeRule.apply {
            customSetContent()
            waitForIdle()
            advanceUntilIdle()
            onNodeWithText(getString(R.string.nothing_to_show)).assertIsNotDisplayed()
            assertSnackbarTextEquals(snackbarScope, exception.message!!)
        }
    }

    @Test
    fun fetchRecords_successNoRecords_emptyMessageShown() = testScope.runTest {
        createViewModel(records, empty = true)
        composeRule.apply {
            customSetContent()
            waitForIdle()
            advanceUntilIdle()

            onNodeWithText(getString(R.string.nothing_to_show)).assertIsDisplayed()
        }
    }

    @Test
    fun openDetailsSheet_detailsShown_knownCategoryName() = testScope.runTest {
        composeRule.apply {
            customSetContent()
            advanceUntilIdle()
            waitForIdle()
            onNodeWithContentDescription(records[0].id).assertIsDisplayed().performClick().assertIsSelected()
            onNodeWithTag("DetailsSheet").assertIsDisplayed()
            onNodeWithText(records[0].category.lowercase()).assertIsDisplayed()

            onNodeWithTag("DetailsSheet").performTouchInput { swipeDown() }.assertIsNotDisplayed()
            waitForIdle()
            onNodeWithContentDescription(records[0].id).assertIsNotSelected()
        }
    }
    @Test
    fun openDetailsSheet_detailsNotShown_unknownCategory() = testScope.runTest {
        createViewModel(records.map { it.copy(category = "unknown category") })
        composeRule.apply {
            customSetContent()
            advanceUntilIdle()
            waitForIdle()
            onNodeWithContentDescription(records[0].id).assertIsDisplayed().performClick().assertIsNotSelected()
            onNodeWithTag("DetailsSheet").assertIsNotDisplayed()
        }
    }
    @Test
    fun openDetailsSheet_deleteClicked_recordNotShown() = testScope.runTest {
        limit = 1
        createViewModel(listOf(records[0]))
        val ref = firestore.collection("data").document(userId).collection("records")
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(limit.toLong())
        composeRule.apply {
            customSetContent()
            advanceUntilIdle()
            waitForIdle()
            onNodeWithContentDescription(records[0].id).assertIsDisplayed().performClick().assertIsSelected()
            onNodeWithTag("DetailsSheet").assertIsDisplayed()
            onNodeWithContentDescription(Icons.Filled.Delete.name).assertIsDisplayed().performClick()
            advanceUntilIdle()
            waitForIdle()
            onNodeWithContentDescription(records[0].id).assertIsNotDisplayed()
            onNodeWithTag("DetailsSheet").assertIsNotDisplayed()
            onNodeWithText(getString(R.string.nothing_to_show)).assertIsDisplayed()
            assertSnackbarIsNotDisplayed(snackbarScope)
            verify(exactly = 1) { ref.get() }
        }
    }
    @Test
    fun deleteClicked_newRecordsLoaded() = testScope.runTest {
        composeRule.apply {
            customSetContent()
            fetchItems_assertSizeIsCorrect()

            val initSize = viewModel.uiState.value.records.size
            for (record in records.subList(0, limit*2)) {
                println(record)
                onNodeWithContentDescription(record.id).assertIsDisplayed().performClick().assertIsSelected()
                onNodeWithContentDescription(Icons.Filled.Delete.name).assertIsDisplayed().performClick()
                advanceUntilIdle()
                assertSnackbarIsNotDisplayed(snackbarScope)
            }
            while (viewModel.uiState.value.records.size <= initSize) {
                waitForIdle()
                advanceUntilIdle()
            }
            assertTrue(viewModel.uiState.value.records.size >= initSize)
        }
    }
    private fun ComposeContentTestRule.fetchItems_assertSizeIsCorrect(iters: Int = 4) {
        testScope.apply {
            for (i in 0..iters) {
                waitForIdle()
                advanceUntilIdle()
                assertSnackbarIsNotDisplayed(snackbarScope)
            }
            while (viewModel.uiState.value.records.size < limit * iters) {
                onNodeWithTag("LazyColumn").performTouchInput { swipeUp() }
                waitForIdle()
                advanceUntilIdle()
            }
        }
        assertTrue(viewModel.uiState.value.records.size >= limit * iters)
        onNodeWithTag("LazyColumn").performTouchInput { swipeDown() }
    }
}