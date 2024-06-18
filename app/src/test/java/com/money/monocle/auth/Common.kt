package com.money.monocle.auth

import com.google.firebase.firestore.FirebaseFirestore
import com.money.monocle.data.Balance
import com.money.monocle.mockTask
import com.money.monocle.userId
import io.mockk.every
import io.mockk.mockk


fun mockFirestore(): FirebaseFirestore =
    mockk {
        every { collection("data").document(userId).collection("balance")
            .document("balance").set(Balance())  } returns mockTask()
    }