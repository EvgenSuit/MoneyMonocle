package com.money.monocle.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.GestureScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.data.Record
import com.money.monocle.domain.DateFormatter
import com.money.monocle.domain.history.TransactionHistoryRepository
import com.money.monocle.mockAuth
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.history.TransactionHistoryViewModel
import com.money.monocle.ui.screens.history.TransactionHistoryScreen
import com.money.monocle.userId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class TransactionHistoryUiTests {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val snapshotListener = slot<EventListener<QuerySnapshot>>()
    private val listenerRegistration = mockk<ListenerRegistration>()
    @get: Rule
    val composeRule = createComposeRule()

    @Before
    fun init() {
        auth = mockAuth()
        mockFirestore()
    }

    private fun mockFirestore() {
        every { listenerRegistration.remove() } returns Unit
        firestore = mockk {
            every { collection("data").document(userId).collection("records")
                .addSnapshotListener(capture(snapshotListener))} returns listenerRegistration
        }
    }

    @Test
    fun fetchRecords_success_recordsShown() = runTest {
        val id = "dfjkdfk"
        val records = listOf(
            Record(id = id,
                expense = true,
                category = 2,
                timestamp = Instant.now().toEpochMilli(),
                amount = 12f)
        )
        val docs = records.map { mockk<DocumentSnapshot> {
            every { toObject(Record::class.java) } returns it
        } }
        val snapshot = mockk<QuerySnapshot> {
            every { isEmpty } returns false
            every { documents } returns docs
        }
        val repository = TransactionHistoryRepository(auth, firestore.collection("data"))
        val viewModel = TransactionHistoryViewModel(repository, DateFormatter(),
            CoroutineScopeProvider(this)
        )
        snapshotListener.captured.onEvent(snapshot, null)
        advanceUntilIdle()
        composeRule.apply {
            setContent {
                TransactionHistoryScreen(currency = "$",
                    onBackClick = {  },
                    viewModel = viewModel)
            }
            onNodeWithTag(id).assertIsDisplayed()
        }
    }

    @Test
    fun openDetailsSheet_detailsShown() = runTest {
        val id = "dfjkdfk"
        val records = listOf(
            Record(id = id,
                expense = true,
                category = 2,
                timestamp = Instant.now().toEpochMilli(),
                amount = 12f), Record()
        )
        val docs = records.map { mockk<DocumentSnapshot> {
            every { toObject(Record::class.java) } returns it
        } }
        val snapshot = mockk<QuerySnapshot> {
            every { isEmpty } returns false
            every { documents } returns docs
        }
        val repository = TransactionHistoryRepository(auth, firestore.collection("data"))
        val viewModel = TransactionHistoryViewModel(repository, DateFormatter(),
            CoroutineScopeProvider(this)
        )
        snapshotListener.captured.onEvent(snapshot, null)
        advanceUntilIdle()
        composeRule.apply {
            setContent {
                TransactionHistoryScreen(currency = "$",
                    onBackClick = {  },
                    viewModel = viewModel)
            }
            onNodeWithTag(id).assertIsDisplayed().performClick().assertIsSelected()
            onNodeWithTag("DetailsSheet").assertIsDisplayed()
            onNodeWithTag("DetailsSheet").performTouchInput { swipeDown() }.assertIsNotDisplayed()
        }
    }

}