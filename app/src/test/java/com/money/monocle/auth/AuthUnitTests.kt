package com.money.monocle.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.UserProfileChangeRequest
import com.money.monocle.BaseTestClass
import com.money.monocle.CorrectAuthData
import com.money.monocle.R
import com.money.monocle.data.Balance
import com.money.monocle.domain.auth.AuthRepository
import com.money.monocle.domain.useCases.AuthType
import com.money.monocle.history.mockAuthForAuthentication
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.StringValue
import com.money.monocle.ui.presentation.auth.AuthViewModel
import com.money.monocle.userId
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AuthUnitTests: BaseTestClass() {
    private val userProfileChangeRequestSlot = slot<UserProfileChangeRequest>()
    private lateinit var viewModel: AuthViewModel
    @Before
    fun init() {
        auth = mockAuthForAuthentication(userProfileChangeRequestSlot)
        firestore = mockFirestore()
        val repository = AuthRepository(auth, firestore, mockk<SignInClient>())
        viewModel = AuthViewModel(repository, CoroutineScopeProvider(testScope))
    }

    @Test
    fun incorrectInput_error() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resources = context.resources
        viewModel.apply {
            onUsername("")
            assertEquals(uiState.value.validationState.usernameValidationError.asString(context),
                resources.getString(R.string.username_not_long_enough))

            onEmail("dfjdjfk")
            assertEquals(uiState.value.validationState.emailValidationError.asString(context),
                resources.getString(R.string.invalid_email_format))

            onPassword("dfdf")
            assertEquals(uiState.value.validationState.passwordValidationError.asString(context),
                resources.getString(R.string.password_not_long_enough))
            onPassword("eirhgejbrj")
            assertEquals(uiState.value.validationState.passwordValidationError.asString(context),
                resources.getString(R.string.password_not_enough_uppercase))
            onPassword("Geirhgejbrj")
            assertEquals(uiState.value.validationState.passwordValidationError.asString(context),
                resources.getString(R.string.password_not_enough_digits))
        }
    }

    @Test
    fun correctInput_success() {
        viewModel.apply {
            onUsername(CorrectAuthData.USERNAME)
            assertTrue(uiState.value.validationState.usernameValidationError == StringValue.Empty)

            onEmail(CorrectAuthData.EMAIL)
            assertTrue(uiState.value.validationState.emailValidationError == StringValue.Empty)

            onPassword(CorrectAuthData.PASSWORD)
            assertTrue(uiState.value.validationState.passwordValidationError == StringValue.Empty)
        }
    }

    @Test
    fun signUp_success() = testScope.runTest {
        viewModel.apply {
            changeAuthType()
            assertEquals(uiState.value.authType, AuthType.SIGN_UP)
            onUsername(CorrectAuthData.USERNAME)
            onEmail(CorrectAuthData.EMAIL)
            onPassword(CorrectAuthData.PASSWORD)
            onCustomAuth()
        }
        advanceUntilIdle()
        verify { auth.createUserWithEmailAndPassword(CorrectAuthData.EMAIL, CorrectAuthData.PASSWORD) }
        val user = auth.currentUser
        assertEquals(userProfileChangeRequestSlot.captured.displayName, CorrectAuthData.USERNAME)
        verify { user!!.updateProfile(userProfileChangeRequestSlot.captured) }
        verify { firestore.collection("data").document(userId).collection("balance")
            .document("balance").set(Balance()) }
    }

    @Test
    fun signIn_success() = testScope.runTest {
        viewModel.apply {
            assertEquals(uiState.value.authType, AuthType.SIGN_IN)
            onUsername(CorrectAuthData.USERNAME)
            onEmail(CorrectAuthData.EMAIL)
            onPassword(CorrectAuthData.PASSWORD)
            onCustomAuth()
        }
        advanceUntilIdle()
        verify { auth.signInWithEmailAndPassword(CorrectAuthData.EMAIL, CorrectAuthData.PASSWORD) }
    }
}