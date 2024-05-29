package com.money.monocle

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.money.monocle.domain.auth.AuthRepository
import com.money.monocle.ui.presentation.AuthViewModel
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.screens.AuthScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AuthTests {
    private lateinit var auth: FirebaseAuth

    @get: Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
         //mockAuth()
    }


    @Test
    fun testGoogleSignIn_signInPerformed_navigatedToHome()  {
        /*val googleIdToken = "3435"
        val mockedActivityResult = mockk<ActivityResult> {
            every { data } returns mockk<Intent>()
        }
        val mockedIntent = mockk<Intent>()
        val signInClient = mockk<SignInClient> {
            every { getSignInCredentialFromIntent(mockedActivityResult.data!!).googleIdToken } returns googleIdToken
        }
        val authRepository = AuthRepository(auth, signInClient)
        val viewModel = AuthViewModel(authRepository, CoroutineScopeProvider(this))*/
        composeTestRule.apply {
            setContent {
                AuthScreen(
                    viewModel =
                    AuthViewModel(AuthRepository(Firebase.auth,
                        Firebase.firestore,
                        Identity.getSignInClient(activity)),
                        CoroutineScopeProvider()),
                    onSignIn = {

                    }, onError = {})
            }
            onNodeWithTag("Google sign in").performClick()
        }
    }
}