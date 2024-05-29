package com.money.monocle.home

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.money.monocle.domain.home.HomeRepository
import com.money.monocle.domain.home.WelcomeRepository
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.HomeViewModel
import com.money.monocle.userId
import com.money.monocle.username
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeUnitTests {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val listenerSlot = slot<EventListener<DocumentSnapshot>>()

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
            every { collection("users").document(userId)
                .addSnapshotListener(capture(listenerSlot)) } returns mockk<ListenerRegistration>()
        }
    }

}