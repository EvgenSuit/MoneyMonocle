package com.money.monocle

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.money.monocle.domain.auth.AuthRepository
import com.money.monocle.ui.presentation.AuthViewModel
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.screens.AuthScreen
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class AuthTests {
    private lateinit var auth: FirebaseAuth

    @get: Rule
    val composeTestRule = createComposeRule()

    @Before
    fun init() {
        mockAuth()
    }

    private fun mockAuth() {
        auth = mockk<FirebaseAuth> {
            every { currentUser?.uid } returns "id"
            every { signInWithCredential(any()) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testGoogleSignIn_signInPerformed() = runTest {
        val activityController = Robolectric.buildActivity(MainActivity::class.java)
        val activity = activityController.create().resume().get()
        val signInClient = Identity.getSignInClient(activity)
        val firestore = mockk<FirebaseFirestore> {
            every { collection("data").document(userId)
                .collection("balance").document("currency").set(-1) } returns mockTask()
        }
        val authRepository = AuthRepository(auth, firestore, signInClient)
        val viewModel = AuthViewModel(authRepository, CoroutineScopeProvider(this))
        composeTestRule.apply {
            setContent {
                AuthScreen(
                    viewModel = viewModel,
                    onSignIn = {

                    }, onError = {})
            }
            onNodeWithTag("Google sign in").performClick()
            advanceUntilIdle()
            waitForIdle()

            //verify { firestore.document("id").set(Currency()) }
        }
    }
}