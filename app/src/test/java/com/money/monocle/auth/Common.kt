package com.money.monocle.auth

import com.google.firebase.firestore.FirebaseFirestore
import com.money.monocle.data.Account
import com.money.monocle.data.Balance
import com.money.monocle.mockTask
import com.money.monocle.userId
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.tasks.await


fun mockFirestore(
    accountSlot: CapturingSlot<Account>? = null
): FirebaseFirestore =
    mockk {
        val idSlot = slot<String>()
        every { collection(userId).document(capture(idSlot)).collection("balance")
            .document("balance").set(Balance()) } returns mockTask()
        every { collection(userId).document("accounts").collection("accounts").document(any<String>())
            .set(if (accountSlot != null) capture(accountSlot) else any<Account>()) } returns mockTask()
    }