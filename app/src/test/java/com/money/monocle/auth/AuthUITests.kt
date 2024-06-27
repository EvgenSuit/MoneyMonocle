package com.money.monocle.auth

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.auth.api.identity.SignInClient
import com.money.monocle.BaseTestClass
import com.money.monocle.CorrectAuthData
import com.money.monocle.IncorrectAuthData
import com.money.monocle.R
import com.money.monocle.assertSnackbarIsNotDisplayed
import com.money.monocle.assertSnackbarTextEquals
import com.money.monocle.domain.auth.AuthRepository
import com.money.monocle.getString
import com.money.monocle.history.mockAuthForAuthentication
import com.money.monocle.mockTask
import com.money.monocle.setContentWithSnackbar
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.auth.AuthViewModel
import com.money.monocle.ui.screens.auth.AuthScreen
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AuthUITests: BaseTestClass() {
    @get: Rule
    val composeRule = createComposeRule()
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        auth = mockAuthForAuthentication()
        firestore = mockFirestore()
        createViewModel()
        composeRule.apply {
            setContentWithSnackbar(snackbarScope) {
                AuthScreen(onSignIn = { }, viewModel = viewModel)
            }
        }
    }
    private fun createViewModel() {
        val repository = AuthRepository(auth, firestore, mockk<SignInClient>())
        viewModel = AuthViewModel(repository, CoroutineScopeProvider(testScope))
    }

    @Test
    fun signIn_testIncorrectInput() {
        composeRule.apply {
            onNodeWithText(getString(R.string.dont_have_an_account)).assertIsDisplayed()
            onNodeWithTag(getString(R.string.email)).performTextReplacement(
                IncorrectAuthData.EMAIL)
            onNodeWithTag(getString(R.string.password)).performTextReplacement(
                IncorrectAuthData.PASSWORD)
            onNodeWithText(getString(R.string.sign_in)).assertIsNotEnabled()
        }
    }
    @Test
    fun signUp_testIncorrectInput() {
        composeRule.apply {
            onNodeWithText(getString(R.string.go_to_signup)).performClick()
            onNodeWithText(getString(R.string.dont_have_an_account)).assertIsNotDisplayed()

            onNodeWithTag(getString(R.string.username)).performTextReplacement(
                IncorrectAuthData.USERNAME)
            onNodeWithText(getString(R.string.username_not_long_enough)).assertIsDisplayed()
            onNodeWithTag(getString(R.string.email)).performTextReplacement(
                IncorrectAuthData.EMAIL)
            onNodeWithText(getString(R.string.invalid_email_format)).assertIsDisplayed()
            onNodeWithTag(getString(R.string.password)).performTextReplacement(
                IncorrectAuthData.PASSWORD)
            onNodeWithText(getString(R.string.password_not_long_enough)).assertIsDisplayed()

            onNodeWithText(getString(R.string.sign_up)).assertIsNotEnabled()
        }
    }

    @Test
    fun signIn_testCorrectInput() {
        composeRule.apply {
            onNodeWithText(getString(R.string.dont_have_an_account)).assertIsDisplayed()
            onNodeWithTag(getString(R.string.email)).performTextReplacement(
                CorrectAuthData.EMAIL)
            onNodeWithTag(getString(R.string.password)).performTextReplacement(
                CorrectAuthData.PASSWORD)
            onNodeWithText(getString(R.string.sign_in)).assertIsEnabled()
        }
    }
    @Test
    fun signUp_testCorrectInput() {
        composeRule.apply {
            onNodeWithText(getString(R.string.go_to_signup)).performClick()
            onNodeWithText(getString(R.string.dont_have_an_account)).assertIsNotDisplayed()
            onNodeWithTag(getString(R.string.username)).performTextReplacement(
                CorrectAuthData.USERNAME)
            onNodeWithTag(getString(R.string.email)).performTextReplacement(
                CorrectAuthData.EMAIL)
            onNodeWithTag(getString(R.string.password)).performTextReplacement(
                CorrectAuthData.PASSWORD)
            onNodeWithText(getString(R.string.sign_up)).assertIsEnabled()
        }
    }
    @Test
    fun signInCorrectInputTest_onGoToSignUpClick_isSignUpDisabled() {
        composeRule.apply {
            onNodeWithTag(getString(R.string.email)).performTextReplacement(
                CorrectAuthData.EMAIL)
            onNodeWithTag(getString(R.string.password)).performTextReplacement(
                CorrectAuthData.PASSWORD)
            onNodeWithText(getString(R.string.go_to_signup)).performClick()
            onNodeWithText(getString(R.string.sign_up)).assertIsNotEnabled()
        }
    }
    @Test
    fun signUpCorrectInputTest_onGoToSignInClick_isSignInEnabled() {
        composeRule.apply {
            onNodeWithText(getString(R.string.go_to_signup)).performClick()

            onNodeWithTag(getString(R.string.username)).performTextReplacement(
                CorrectAuthData.USERNAME)
            onNodeWithTag(getString(R.string.email)).performTextReplacement(
                CorrectAuthData.EMAIL)
            onNodeWithTag(getString(R.string.password)).performTextReplacement(
                CorrectAuthData.PASSWORD)

            onNodeWithText(getString(R.string.sign_up)).assertIsEnabled()
            onNodeWithText(getString(R.string.go_to_signin)).performClick()
            onNodeWithText(getString(R.string.sign_in)).assertIsEnabled()
        }
    }
    @Test
    fun signIn_onSuccess_snackbarNotShown() = testScope.runTest {
        composeRule.apply {
            onNodeWithTag(getString(R.string.email)).performTextReplacement(
                CorrectAuthData.EMAIL)
            onNodeWithTag(getString(R.string.password)).performTextReplacement(
                CorrectAuthData.PASSWORD)
            onNodeWithText(getString(R.string.sign_in)).performClick()

            onNodeWithTag(getString(R.string.email)).assertIsNotEnabled()
            onNodeWithTag(getString(R.string.password)).assertIsNotEnabled()
            onNodeWithText(getString(R.string.go_to_signup)).assertIsNotEnabled()
            onNodeWithText(getString(R.string.sign_in)).assertIsNotEnabled()

            advanceUntilIdle()
            assertSnackbarIsNotDisplayed(snackbarScope)
        }
    }
    @Test
    fun signUp_onSuccess_snackbarNotShown() = testScope.runTest {
        composeRule.apply {
            onNodeWithText(getString(R.string.go_to_signup)).performClick()
            onNodeWithTag(getString(R.string.username)).performTextReplacement(
                CorrectAuthData.USERNAME)
            onNodeWithTag(getString(R.string.email)).performTextReplacement(
                CorrectAuthData.EMAIL)
            onNodeWithTag(getString(R.string.password)).performTextReplacement(
                CorrectAuthData.PASSWORD)
            onNodeWithText(getString(R.string.sign_up)).performClick()

            onNodeWithTag(getString(R.string.username)).assertIsNotEnabled()
            onNodeWithTag(getString(R.string.email)).assertIsNotEnabled()
            onNodeWithTag(getString(R.string.password)).assertIsNotEnabled()
            onNodeWithText(getString(R.string.go_to_signin)).assertIsNotEnabled()
            onNodeWithText(getString(R.string.sign_up)).assertIsNotEnabled()

            advanceUntilIdle()
            assertSnackbarIsNotDisplayed(snackbarScope)
        }
    }
    @Test
    fun signIn_onError_snackbarShown() = testScope.runTest {
        val exception = Exception("exception")
        every { auth.signInWithEmailAndPassword(CorrectAuthData.EMAIL, CorrectAuthData.PASSWORD) } returns mockTask(exception = exception)
        composeRule.apply {
            onNodeWithTag(getString(R.string.email)).performTextReplacement(
                CorrectAuthData.EMAIL)
            onNodeWithTag(getString(R.string.password)).performTextReplacement(
                CorrectAuthData.PASSWORD)
            onNodeWithText(getString(R.string.sign_in)).performClick()

            onNodeWithTag(getString(R.string.email)).assertIsNotEnabled()
            onNodeWithTag(getString(R.string.password)).assertIsNotEnabled()
            onNodeWithText(getString(R.string.go_to_signup)).assertIsNotEnabled()
            onNodeWithText(getString(R.string.sign_in)).assertIsNotEnabled()

            advanceUntilIdle()
            assertSnackbarTextEquals(snackbarScope, exception.message!!)
        }
    }

    @Test
    fun signUp_onError_snackbarShown() = testScope.runTest {
        val exception = Exception("exception")
        every { auth.createUserWithEmailAndPassword(CorrectAuthData.EMAIL, CorrectAuthData.PASSWORD) } returns mockTask(exception = exception)
        composeRule.apply {
            onNodeWithText(getString(R.string.go_to_signup)).performClick()
            onNodeWithTag(getString(R.string.username)).performTextReplacement(
                CorrectAuthData.USERNAME)
            onNodeWithTag(getString(R.string.email)).performTextReplacement(
                CorrectAuthData.EMAIL)
            onNodeWithTag(getString(R.string.password)).performTextReplacement(
                CorrectAuthData.PASSWORD)
            onNodeWithText(getString(R.string.sign_up)).performClick()

            onNodeWithTag(getString(R.string.username)).assertIsNotEnabled()
            onNodeWithTag(getString(R.string.email)).assertIsNotEnabled()
            onNodeWithTag(getString(R.string.password)).assertIsNotEnabled()
            onNodeWithText(getString(R.string.go_to_signin)).assertIsNotEnabled()
            onNodeWithText(getString(R.string.sign_up)).assertIsNotEnabled()

            advanceUntilIdle()
            assertSnackbarTextEquals(snackbarScope, exception.message!!)
        }
    }
}