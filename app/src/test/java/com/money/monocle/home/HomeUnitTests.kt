package com.money.monocle.home

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.BaseTestClass
import com.money.monocle.data.AccountName
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
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeUnitTests: BaseTestClass() {
    private lateinit var viewModel: HomeViewModel
    private val balanceListenerSlot = slot<EventListener<QuerySnapshot>>()
    private val statsListenerSlot = slot<EventListener<QuerySnapshot>>()
    private val isAccountLoadedSlot = slot<Boolean>()
    private val isWelcomeScreenShownSlot = slot<Boolean>()
    private val balanceSlot = slot<Balance>()
    private val fiveDaysAgoSlot = slot<Long>()

    @Before
    fun init() {
        auth = mockAuth()
        firestore = mockHomeFirestore(balanceListenerSlot, statsListenerSlot, fiveDaysAgoSlot)
        dataStoreManager = mockDataStoreManager(
            isAccountLoadedSlot = isAccountLoadedSlot,
            isWelcomeScreenShownSlot = isWelcomeScreenShownSlot,
            balanceSlot = balanceSlot)
        mockViewModel()
    }
    private fun mockViewModel() {
        val homeRepository = HomeRepository(auth, firestore, dataStoreManager)
        viewModel = HomeViewModel(homeRepository, mockk<WelcomeRepository>(),
            dataStoreManager, CoroutineScopeProvider(testScope))
    }

    @Test
    fun testAccountState_accountDeleted_signOut() = testScope.runTest {
        val homeRepository = HomeRepository(auth, firestore, dataStoreManager)
        val mockedSnapshot = mockk<QuerySnapshot> {
            every { isEmpty } returns true
            every { documents } returns listOf()
        }
        HomeViewModel(homeRepository, mockk<WelcomeRepository>(), dataStoreManager,
            CoroutineScopeProvider(this))
        advanceUntilIdle()
        balanceListenerSlot.captured.onEvent(mockedSnapshot, null)
        advanceUntilIdle()
        verify { auth.signOut() }
        coVerify { dataStoreManager.changeAccountState(false) }
    }

    @Test
    fun testAccountCreation_newAccountOnSubmit_success() = testScope.runTest {
        val currentBalance = 233.4f
        val currency = CurrencyEnum.EUR
        val balance = Balance(currency.ordinal, currentBalance)
        val homeRepository = HomeRepository(auth, firestore, dataStoreManager)
        every {
            firestore.collection(userId).document(AccountName.MAIN.name)
                .collection("balance").document("balance")
                .set(balance)
        } returns mockTask()
        val welcomeRepository = WelcomeRepository(auth, firestore)
        val viewModel = HomeViewModel(homeRepository, welcomeRepository, dataStoreManager,
            CoroutineScopeProvider(this))
        advanceUntilIdle()
        val mockedDocs = listOf(mockk<DocumentSnapshot> {
            every { exists() } returns true
            every { toObject(Balance::class.java) } returns balance
        })
        val mockedSnapshot = mockk<QuerySnapshot> {
            every { isEmpty } returns false
            every { documents } returns mockedDocs
        }
        viewModel.setBalance(balance)
        balanceListenerSlot.captured.onEvent(mockedSnapshot, null)
        advanceUntilIdle()
        verify { firestore.collection(userId).document(AccountName.MAIN.name).collection("balance")
            .document("balance").set(Balance(currency.ordinal, currentBalance))}
        coVerify { dataStoreManager.changeAccountState(true) }
        assertEquals(balance, viewModel.uiState.value.balance)
        assertEquals(balance, viewModel.uiState.value.balance)
    }

    @Test
    fun fetchPieChart_success() = testScope.runTest {
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
        mockViewModel()
        advanceUntilIdle()
        statsListenerSlot.captured.onEvent(query, null)
        advanceUntilIdle()

        val pieChartState = viewModel.uiState.value.pieChartState
        assertEquals(totalSpent, pieChartState.totalSpent)
        assertEquals(totalEarned, pieChartState.totalEarned)
        assertEquals(Instant.now().toEpochMilli() - fiveDaysAgoSlot.captured, (5*24*60*60*1000))
    }
}