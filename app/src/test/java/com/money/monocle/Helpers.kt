package com.money.monocle

import android.content.Context
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk


val userId = "id"
val username = "Evgen"

typealias BalanceListener = CapturingSlot<EventListener<QuerySnapshot>>
typealias StatsListener = CapturingSlot<EventListener<QuerySnapshot>>
typealias LastTimeUpdatedListener = CapturingSlot<EventListener<DocumentSnapshot>>

fun getString(@StringRes id: Int): String =
    ApplicationProvider.getApplicationContext<Context>().getString(id)

fun getInt(@IntegerRes id: Int): Int =
    ApplicationProvider.getApplicationContext<Context>().resources.getInteger(id)
inline fun <reified T> mockTask(result: T? = null, exception: Exception? = null): Task<T> {
    val task = mockk<Task<T>>()
    every { task.result } returns result
    every { task.exception } returns exception
    every { task.isCanceled } returns false
    every { task.isComplete } returns true
    return task
}

fun mockAuth(): FirebaseAuth =
    mockk {
        every { currentUser?.uid } returns userId
        every { currentUser } returns mockk<FirebaseUser>{
            every { uid } returns userId
            every { displayName } returns username
            every { signOut() } returns Unit
        }
    }
fun SemanticsNodeInteraction.printToLog(
    maxDepth: Int = Int.MAX_VALUE,
) {
    val result = "printToLog:\n" + printToString(maxDepth)
    println(result)
}