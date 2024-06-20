package com.money.monocle.settings

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.money.monocle.data.LastTimeUpdated
import com.money.monocle.mockTask
import com.money.monocle.userId
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk

fun mockFirestore(listener: CapturingSlot<EventListener<DocumentSnapshot>>) = mockk<FirebaseFirestore> {
        every { collection("data").document(userId).collection("balance")
            .document("lastTimeUpdated").addSnapshotListener(capture(listener))} returns mockk<ListenerRegistration>()
        every { collection("data").document(userId).collection("balance")
            .document("lastTimeUpdated").addSnapshotListener(capture(listener)).remove()} returns Unit
        every { collection("data").document(userId).collection("balance")
            .document("lastTimeUpdated").set(any()) } answers {
                listener.captured.onEvent(mockk<DocumentSnapshot> {
                    every { toObject(LastTimeUpdated::class.java) } returns firstArg<LastTimeUpdated>()
                }, null)
            mockTask()
        }
        every { collection("data").document(userId).collection("balance")
            .document("balance").set(any())} returns mockTask()
}