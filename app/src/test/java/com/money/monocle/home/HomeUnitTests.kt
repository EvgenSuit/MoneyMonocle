package com.money.monocle.home

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.domain.home.HomeRepository
import com.money.monocle.domain.home.WelcomeRepository
import com.money.monocle.mockTask
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.HomeViewModel
import com.money.monocle.ui.screens.home.HomeScreen
import com.money.monocle.userId
import com.money.monocle.username
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeUnitTests {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val listenerSlot = slot<EventListener<QuerySnapshot>>()

    @Before
    fun init() {
        mockAuth()
        mockFirestore()
    }
    private fun mockAuth() {
        auth = mockk {
            every { currentUser?.uid } returns userId
            every { currentUser } returns mockk<FirebaseUser>{
                every { uid } returns userId
                every { displayName } returns username
                every { signOut() } returns Unit
            }
        }
    }
    private fun mockFirestore() {
        firestore = mockk {
            every { collection("data").document(userId).collection("balance")
                .addSnapshotListener(capture(listenerSlot))} returns mockk<ListenerRegistration>()
            every { collection("data").document(userId).collection("balance")
                .addSnapshotListener(capture(listenerSlot)).remove() } returns Unit
        }
    }

    @Test
    fun testAccountState_accountDeleted_signOut() = runTest {
        val homeRepository = HomeRepository(auth, firestore)
        val mockedSnapshot = mockk<QuerySnapshot> {
            every { isEmpty } returns true
            every { documents } returns listOf()
        }
        HomeViewModel(homeRepository, mockk<WelcomeRepository>(), CoroutineScopeProvider(this))
        advanceUntilIdle()
        listenerSlot.captured.onEvent(mockedSnapshot, null)
        verify { auth.signOut() }
        verify { firestore.collection("data").document(userId).collection("balance")
            .addSnapshotListener(capture(listenerSlot)).remove() }
    }

    @Test
    fun testAccountCreation_newAccountOnSubmit_success() = runTest {
        val currentBalance = 233.4f
        val currency = CurrencyEnum.EUR
        val homeRepository = HomeRepository(auth, firestore)
        every {
            firestore.collection("data").document(userId)
                .collection("balance").document("balance")
                .set(Balance(currency.ordinal, currentBalance))
        } returns mockTask()
        val welcomeRepository = WelcomeRepository(auth, firestore)
        val viewModel = HomeViewModel(homeRepository, welcomeRepository, CoroutineScopeProvider(this))
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
        listenerSlot.captured.onEvent(mockedSnapshot, null)
        every { firestore.collection("data").document(auth.currentUser!!.uid).collection("balance")
            .document("balance").set(Balance(currency.ordinal, currentBalance))}
        assertEquals(viewModel.uiState.value.balanceState.currentBalance, currentBalance)
        assertEquals(viewModel.uiState.value.balanceState.currency, currency.ordinal)
    }
}