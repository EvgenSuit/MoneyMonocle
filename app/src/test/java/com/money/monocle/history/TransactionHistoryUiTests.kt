package com.money.monocle.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import com.money.monocle.domain.history.TransactionHistoryRepository
import com.money.monocle.domain.useCases.DateFormatter
import com.money.monocle.getString
import com.money.monocle.mockAuth
import com.money.monocle.setContentWithSnackbar
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.history.TransactionHistoryViewModel
import com.money.monocle.ui.screens.history.TransactionHistoryScreen
import com.money.monocle.userId
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class TransactionHistoryUiTests: BaseTestClass() {
    private val limit = 3
    private lateinit var viewModel: TransactionHistoryViewModel

    @get: Rule
    val composeRule = createComposeRule()
    @Before
    fun init() {
        auth = mockAuth()
        firestore = mockFirestore(limit, records)
        createViewModel()
    }
    private fun createViewModel() {
        val repository = TransactionHistoryRepository(
            limit = limit,
            auth = auth, firestore = firestore.collection("data"))
        viewModel = TransactionHistoryViewModel(repository, DateFormatter(),
            CoroutineScopeProvider(testScope))
    }
    @Test
    fun fetchRecords_success_recordsShown() = testScope.runTest {
            val firstRef = firestore.collection("data").document(userId).collection("records")
                .orderBy("timestamp", Query.Direction.DESCENDING).limit(limit.toLong())
            composeRule.apply {
                setContentWithSnackbar(snackbarScope) {
                    TransactionHistoryScreen(
                        currency = "$",
                        onBackClick = {  },
                        viewModel = viewModel)
                }
                onNodeWithText(getString(R.string.nothing_to_show)).assertIsNotDisplayed()
                advanceUntilIdle()
                waitForIdle()
                for (i in records.indices) {
                    onNodeWithTag("LazyColumn").performTouchInput { swipeUp() }
                    mainClock.advanceTimeBy(ANIMATION_DURATION.toLong() + 5L)
                    mainClock.autoAdvance = true
                    advanceUntilIdle()
                    waitForIdle()
                }
                snackbarScope.advanceUntilIdle()
                onNodeWithTag(getString(R.string.error_snackbar)).isNotDisplayed()
            }
            // verify only one limit call was made
            verify(exactly = 1) { firstRef.get() }
        }
    @Test
    fun fetchRecords_error_snackbarShown() = testScope.runTest {
        val exception = Exception("error")
        firestore = mockFirestore(limit, records, exception = exception)
        createViewModel()
        composeRule.apply {
            setContentWithSnackbar(snackbarScope) {
                TransactionHistoryScreen(
                    currency = "$",
                    onBackClick = {  },
                    viewModel = viewModel)
            }
            advanceUntilIdle()
            onNodeWithText(getString(R.string.nothing_to_show)).assertIsNotDisplayed()
            assertSnackbarTextEquals(snackbarScope, exception.message!!)
        }
    }

    @Test
    fun fetchRecords_successNoRecords_emptyMessageShown() = testScope.runTest {
            firestore = mockFirestore(limit, records, empty = true)
            val repository = TransactionHistoryRepository(
                limit = limit,
                auth = auth, firestore = firestore.collection("data"))
            val viewModel = TransactionHistoryViewModel(repository, DateFormatter(),
                CoroutineScopeProvider(this)
            )
            composeRule.apply {
                setContentWithSnackbar(snackbarScope) {
                    TransactionHistoryScreen(
                        currency = "$",
                        onBackClick = {  },
                        viewModel = viewModel)
                }
            }
        }

    @Test
    fun openDetailsSheet_detailsShown() = testScope.runTest {
            val repository = TransactionHistoryRepository(limit = limit,
                auth = auth, firestore = firestore.collection("data"))
            val viewModel = TransactionHistoryViewModel(repository, DateFormatter(), CoroutineScopeProvider(this))
            composeRule.apply {
                setContentWithSnackbar(snackbarScope) {
                    TransactionHistoryScreen(
                        currency = "$",
                        onBackClick = {  },
                        viewModel = viewModel)
                }
                advanceUntilIdle()
                waitForIdle()
                onNodeWithContentDescription(records[0].timestamp.toString()).assertIsDisplayed().performClick().assertIsSelected()
                onNodeWithTag("DetailsSheet").assertIsDisplayed()
                onNodeWithTag("DetailsSheet").performTouchInput { swipeDown() }.assertIsNotDisplayed()
                waitForIdle()
                onNodeWithContentDescription(records[0].timestamp.toString()).assertIsNotSelected()
            }
        }
    @Test
    fun openDetailsSheet_deleteClicked_recordsNotShown() = testScope.runTest {
            val limit = 1
            firestore = mockFirestore(limit, listOf(records[0]))
            val ref = firestore.collection("data").document(userId).collection("records")
                .orderBy("timestamp", Query.Direction.DESCENDING).limit(limit.toLong())
            val repository = TransactionHistoryRepository(limit = limit,
                auth = auth, firestore = firestore.collection("data"))
            val viewModel = TransactionHistoryViewModel(repository, DateFormatter(), CoroutineScopeProvider(this))
            composeRule.apply {
                setContentWithSnackbar(snackbarScope) {
                    TransactionHistoryScreen(
                        currency = "$",
                        onBackClick = {  },
                        viewModel = viewModel)
                }
                advanceUntilIdle()
                waitForIdle()
                onNodeWithContentDescription(records[0].timestamp.toString()).assertIsDisplayed().performClick().assertIsSelected()
                onNodeWithTag("DetailsSheet").assertIsDisplayed()
                onNodeWithContentDescription("DeleteRecord").assertIsDisplayed().performClick()
                advanceUntilIdle()
                waitForIdle()
                onNodeWithContentDescription(records[0].timestamp.toString()).assertIsNotDisplayed()
                onNodeWithTag("DetailsSheet").assertIsNotDisplayed()
                onNodeWithText(getString(R.string.nothing_to_show)).assertIsDisplayed()
                assertSnackbarIsNotDisplayed(snackbarScope)
                verify(exactly = 1) { ref.get() }
            }
        }
}