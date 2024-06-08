package com.money.monocle.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.money.monocle.R
import com.money.monocle.domain.DateFormatter
import com.money.monocle.domain.history.TransactionHistoryRepository
import com.money.monocle.getString
import com.money.monocle.mockAuth
import com.money.monocle.printToLog
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.history.TransactionHistoryViewModel
import com.money.monocle.ui.screens.history.TransactionHistoryScreen
import com.money.monocle.userId
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class TransactionHistoryUiTests {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val limit = 3

    @get: Rule
    val composeRule = createComposeRule()

    @Before
    fun init() {
        auth = mockAuth()
        firestore = mockFirestore(limit, records)
    }
    @After
    fun clean() = unmockkAll()
    @Test
    fun fetchRecords_success_recordsShown() = runTest {
        val repository = TransactionHistoryRepository(
            limit = limit,
            auth = auth, firestore = firestore.collection("data"))
        val viewModel = TransactionHistoryViewModel(repository, DateFormatter(),
            CoroutineScopeProvider(this))
        val firstRef = firestore.collection("data").document(userId).collection("records")
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(limit.toLong())
        composeRule.apply {
            setContent {
                TransactionHistoryScreen(
                    onError = {},
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
        }
        // verify only one limit call was made
        verify(exactly = 1) { firstRef.get() }
    }

    @Test
    fun fetchRecords_successNoRecords_emptyMessageShown() = runTest {
        firestore = mockFirestore(limit, records, empty = true)
        val repository = TransactionHistoryRepository(
            limit = limit,
            auth = auth, firestore = firestore.collection("data"))
        val viewModel = TransactionHistoryViewModel(repository, DateFormatter(),
            CoroutineScopeProvider(this)
        )
        composeRule.apply {
            setContent {
                TransactionHistoryScreen(
                    onError = {},
                    currency = "$",
                    onBackClick = {  },
                    viewModel = viewModel)
            }
            advanceUntilIdle()
        }
    }

    @Test
    fun openDetailsSheet_detailsShown() = runTest {
        val repository = TransactionHistoryRepository(limit = limit,
            auth = auth, firestore = firestore.collection("data"))
        val viewModel = TransactionHistoryViewModel(repository, DateFormatter(), CoroutineScopeProvider(this))
        composeRule.apply {
            setContent {
                TransactionHistoryScreen(
                    onError = {},
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
    fun openDetailsSheet_deleteClicked_recordNotShown() = runTest {
        val limit = 1
        firestore = mockFirestore(limit, listOf(records[0]))
        val ref = firestore.collection("data").document(userId).collection("records")
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(limit.toLong())
        val repository = TransactionHistoryRepository(limit = limit,
            auth = auth, firestore = firestore.collection("data"))
        val viewModel = TransactionHistoryViewModel(repository, DateFormatter(), CoroutineScopeProvider(this))
        composeRule.apply {
            setContent {
                TransactionHistoryScreen(
                    onError = {},
                    currency = "$",
                    onBackClick = {  },
                    viewModel = viewModel)
            }
            advanceUntilIdle()
            waitForIdle()
            onNodeWithContentDescription(records[0].timestamp.toString()).assertIsDisplayed().performClick().assertIsSelected()
            onNodeWithTag("DetailsSheet").assertIsDisplayed()
            onNodeWithContentDescription("DeleteRecord").assertIsDisplayed().performClick()
            onNodeWithContentDescription("DeleteRecord").printToLog()
            advanceUntilIdle()
            waitForIdle()
            onNodeWithContentDescription(records[0].timestamp.toString()).assertIsNotDisplayed()
            onNodeWithTag("DetailsSheet").assertIsNotDisplayed()
            onNodeWithText(getString(R.string.nothing_to_show)).assertIsDisplayed()
            verify(exactly = 1) { ref.get() }
        }
    }
}