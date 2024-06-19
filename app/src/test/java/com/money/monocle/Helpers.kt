package com.money.monocle

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.QuerySnapshot
import com.money.monocle.ui.screens.components.CustomErrorSnackbar
import com.money.monocle.ui.screens.components.SnackbarController
import com.money.monocle.R
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle

class CorrectAuthData {
    companion object {
        const val USERNAME: String = "Evgen"
        const val EMAIL: String = "someemail@gmail.com"
        const val PASSWORD: String = "SomePassword123"
    }
}
class IncorrectAuthData {
    companion object {
        const val USERNAME: String = " "
        const val EMAIL: String = "incorrect"
        const val PASSWORD: String = " "
    }
}


val userId = "id"

typealias BalanceListener = CapturingSlot<EventListener<QuerySnapshot>>
typealias StatsListener = CapturingSlot<EventListener<QuerySnapshot>>
typealias LastTimeCurrencyUpdatedListener = CapturingSlot<EventListener<DocumentSnapshot>>

fun getString(@StringRes id: Int): String =
    ApplicationProvider.getApplicationContext<Context>().getString(id)

@OptIn(ExperimentalCoroutinesApi::class)
fun AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>
        .assertSnackbarIsDisplayed(snackbarScope: TestScope): Boolean {
    snackbarScope.advanceUntilIdle()
    return onNodeWithTag(getString(R.string.error_snackbar)).isDisplayed()
}
@OptIn(ExperimentalCoroutinesApi::class)
fun ComposeContentTestRule.assertSnackbarIsDisplayed(snackbarScope: TestScope) {
    snackbarScope.advanceUntilIdle()
    onNodeWithTag(getString(R.string.error_snackbar)).assertIsDisplayed()
}
@OptIn(ExperimentalCoroutinesApi::class)
fun ComposeContentTestRule.assertSnackbarIsNotDisplayed(snackbarScope: TestScope) {
    waitForIdle()
    snackbarScope.advanceUntilIdle()
    onNodeWithTag(getString(R.string.error_snackbar)).assertIsNotDisplayed()
}
@OptIn(ExperimentalCoroutinesApi::class)
fun ComposeContentTestRule.assertSnackbarTextEquals(snackbarScope: TestScope, message: String) {
    waitForIdle()
    snackbarScope.advanceUntilIdle()
    onNodeWithTag(getString(R.string.error_snackbar), true).assertTextEquals(message)
}

@OptIn(ExperimentalMaterial3Api::class)
fun ComposeContentTestRule.setContentWithSnackbar(
    coroutineScope: CoroutineScope,
    content: @Composable () -> Unit) {
    setContent {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val snackbarHostState = remember { SnackbarHostState() }
        val snackbarController = SnackbarController(snackbarHostState, coroutineScope, context)
        CompositionLocalProvider(LocalSnackbarController provides snackbarController) {
            CustomErrorSnackbar(snackbarHostState = snackbarHostState,
                swipeToDismissBoxState = rememberSwipeToDismissBoxState())
            content()
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
fun ComponentActivity.setContentWithSnackbar(
    coroutineScope: CoroutineScope,
    content: @Composable () -> Unit) {
    setContent {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val snackbarHostState = remember { SnackbarHostState() }
        val snackbarController = SnackbarController(snackbarHostState, coroutineScope, context)
        CompositionLocalProvider(LocalSnackbarController provides snackbarController) {
            CustomErrorSnackbar(snackbarHostState = snackbarHostState,
                swipeToDismissBoxState = rememberSwipeToDismissBoxState())
            content()
        }
    }
}

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

fun mockAuth(): FirebaseAuth {
    val user = mockk<FirebaseUser>{
        every { uid } returns userId
        every { displayName } returns CorrectAuthData.USERNAME
    }
    return mockk {
        every { currentUser } returns user
        every { signOut() } returns Unit
        every { createUserWithEmailAndPassword(any(), any()) } answers {
            every { currentUser } returns user
            mockTask()
        }
        every { signInWithEmailAndPassword(any(), any()) } answers {
            every { currentUser } returns user
            mockTask()
        }
    }
}

fun SemanticsNodeInteraction.printToLog(
    maxDepth: Int = Int.MAX_VALUE,
) {
    val result = "printToLog:\n" + printToString(maxDepth)
    println(result)
}