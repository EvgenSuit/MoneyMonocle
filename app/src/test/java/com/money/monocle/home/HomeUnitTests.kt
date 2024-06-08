package com.money.monocle.home

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.home.HomeRepository
import com.money.monocle.domain.home.WelcomeRepository
import com.money.monocle.mockAuth
import com.money.monocle.mockTask
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.home.HomeViewModel
import com.money.monocle.userId
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeUnitTests {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var dataStoreManager: DataStoreManager
    private val balanceListenerSlot = slot<EventListener<QuerySnapshot>>()
    private val statsListenerSlot = slot<EventListener<QuerySnapshot>>()
    private val isAccountLoadedSlot = slot<Boolean>()
    private val fiveDaysAgoSlot = slot<Long>()

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
                .addSnapshotListener(capture(balanceListenerSlot))} returns mockk<ListenerRegistration>()
            every { collection("data").document(userId).collection("balance")
                .addSnapshotListener(capture(balanceListenerSlot)).remove() } returns Unit
            every { collection("data").document(userId).collection("records").whereGreaterThan("timestamp", capture(fiveDaysAgoSlot))
                .addSnapshotListener(capture(statsListenerSlot))} returns mockk<ListenerRegistration>()
            every { collection("data").document(userId).collection("records").whereGreaterThan("timestamp", capture(fiveDaysAgoSlot))
                .addSnapshotListener(capture(statsListenerSlot)).remove() } returns Unit
        }
    }

    @Test
    fun testAccountState_accountDeleted_signOut() = runTest {
        val homeRepository = HomeRepository(auth, firestore.collection("data"), dataStoreManager)
        val mockedSnapshot = mockk<QuerySnapshot> {
            every { isEmpty } returns true
            every { documents } returns listOf()
        }
        HomeViewModel(homeRepository, mockk<WelcomeRepository>(),
            CoroutineScopeProvider(this))
        advanceUntilIdle()
        balanceListenerSlot.captured.onEvent(mockedSnapshot, null)
        advanceUntilIdle()
        verify { auth.signOut() }
        verify { firestore.collection("data").document(userId).collection("balance")
            .addSnapshotListener(capture(balanceListenerSlot)).remove() }
        coVerify { dataStoreManager.changeAccountState(false) }
    }

    @Test
    fun testAccountCreation_newAccountOnSubmit_success() = runTest {
        val currentBalance = 233.4f
        val currency = CurrencyEnum.EUR
        val homeRepository = HomeRepository(auth, firestore.collection("data"), dataStoreManager)
        every {
            firestore.collection("data").document(userId)
                .collection("balance").document("balance")
                .set(Balance(currency.ordinal, currentBalance))
        } returns mockTask()
        val welcomeRepository = WelcomeRepository(auth, firestore.collection("data"))
        val viewModel = HomeViewModel(homeRepository, welcomeRepository,
            CoroutineScopeProvider(this))
        advanceUntilIdle()
        val mockedDocs = listOf(mockk<DocumentSnapshot> {
            every { exists() } returns true
            every { toObject(Balance::class.java) } returns Balance(currency.ordinal, currentBalance)
        })
        val mockedSnapshot = mockk<QuerySnapshot> {
            every { isEmpty } returns false
            every { documents } returns mockedDocs
        }
        viewModel.setBalance(currency, currentBalance)
        balanceListenerSlot.captured.onEvent(mockedSnapshot, null)
        advanceUntilIdle()
        verify { firestore.collection("data").document(userId).collection("balance")
            .document("balance").set(Balance(currency.ordinal, currentBalance))}
        coVerify { dataStoreManager.changeAccountState(true) }
        assertEquals(viewModel.uiState.value.balanceState.currentBalance, currentBalance)
        assertEquals(viewModel.uiState.value.balanceState.currency, currency.ordinal)
    }

    @Test
    fun fetchPieChart_success() = runTest {
        val records = List(10) {
            mockk<DocumentSnapshot> {
                every { getDouble("amount") } returns it.toDouble()
                every { getBoolean("expense") } returns (it % 2 == 0)
            }
        }
        val totalSpent = records.filter { it.getBoolean("expense") == true }.sumOf { it.getDouble("amount")!! }.toFloat()
        val totalEarned = records.filter { it.getBoolean("expense") == false }.sumOf { it.getDouble("amount")!! }.toFloat()
        val query = mockk<QuerySnapshot> {
            every { isEmpty } returns false
            every { documents } returns records
        }
        val currentTimestamp = Instant.now().toEpochMilli()
        mockkStatic(Instant::class)
        every { Instant.now().toEpochMilli() } returns currentTimestamp
        val homeRepository = HomeRepository(auth, firestore.collection("data"), dataStoreManager)
        val viewModel = HomeViewModel(homeRepository, mockk<WelcomeRepository>(), CoroutineScopeProvider(this))
        advanceUntilIdle()
        statsListenerSlot.captured.onEvent(query, null)
        advanceUntilIdle()

        val pieChartState = viewModel.uiState.value.pieChartState
        assertEquals(totalSpent, pieChartState.totalSpent)
        assertEquals(totalEarned, pieChartState.totalEarned)
        assertEquals(Instant.now().toEpochMilli() - fiveDaysAgoSlot.captured, (5*24*60*60*1000))
    }
}