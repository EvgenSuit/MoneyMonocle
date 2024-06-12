package com.money.monocle.settings

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.money.monocle.mockAuth
import com.money.monocle.userId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Before

class SettingsUnitTests {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val listener = slot<EventListener<DocumentSnapshot>>()

    @Before
    fun init() {
        auth = mockAuth()
    }
    private fun mockFirestore() {
        firestore = mockk {
            every { collection("data").document(userId).collection("balance")
                .document("balance").addSnapshotListener(capture(listener))} returns mockk<ListenerRegistration>()
            every { collection("data").document(userId).collection("balance")
                .document("balance").addSnapshotListener(capture(listener)).remove()} returns Unit
        }
    }

}