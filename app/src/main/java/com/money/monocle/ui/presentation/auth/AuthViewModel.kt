package com.money.monocle.ui.presentation.auth

import android.app.Activity
import android.content.IntentSender
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.money.monocle.R
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.auth.AuthRepository
import com.money.monocle.domain.useCases.AuthType
import com.money.monocle.domain.useCases.EmailValidationResult
import com.money.monocle.domain.useCases.EmailValidator
import com.money.monocle.domain.useCases.PasswordValidationResult
import com.money.monocle.domain.useCases.PasswordValidator
import com.money.monocle.domain.useCases.UsernameValidationResult
import com.money.monocle.domain.useCases.UsernameValidator
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.StringValue
import com.money.monocle.ui.presentation.toStringIfMessageIsNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    coroutineScopeProvider: CoroutineScopeProvider
): ViewModel() {
    private val usernameValidator = UsernameValidator()
    private val emailValidator = EmailValidator()
    private val passwordValidator = PasswordValidator()
    private val scope = coroutineScopeProvider.provide() ?: viewModelScope
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    val auth = authRepository.authRef

    fun onUsername(username: String) {
        val result = when (usernameValidator(username)) {
            UsernameValidationResult.IS_EMPTY -> StringValue.StringResource(R.string.username_not_long_enough)
            else -> StringValue.Empty
        }
        _uiState.update { it.copy(validationState = it.validationState.copy(usernameValidationError = result),
            authState = it.authState.copy(username = username)) }
    }
    fun onEmail(email: String) {
        val result = when (emailValidator(email)) {
            EmailValidationResult.INCORRECT_FORMAT -> StringValue.StringResource(R.string.invalid_email_format)
            else -> StringValue.Empty
        }
        _uiState.update { it.copy(validationState = it.validationState.copy(emailValidationError = result),
            authState = it.authState.copy(email = email)) }
    }
    fun onPassword(password: String) {
        val result = when (passwordValidator(password)) {
            PasswordValidationResult.NOT_LONG_ENOUGH -> StringValue.StringResource(R.string.password_not_long_enough)
            PasswordValidationResult.NOT_ENOUGH_UPPERCASE -> StringValue.StringResource(R.string.password_not_enough_uppercase)
            PasswordValidationResult.NOT_ENOUGH_DIGITS -> StringValue.StringResource(R.string.password_not_enough_digits)
            else -> StringValue.Empty
        }
        _uiState.update { it.copy(validationState = it.validationState.copy(passwordValidationError = result),
            authState = it.authState.copy(password = password)) }
    }
    fun changeAuthType() {
        _uiState.update { it.copy(authType = AuthType.entries[it.authType.ordinal xor 1]) }
    }

    fun onCustomAuth() {
        val authType = _uiState.value.authType
        updateAuthResult(CustomResult.InProgress)
        scope.launch {
            try {
                if (authType == AuthType.SIGN_UP) {
                    authRepository.signUp(_uiState.value.authState)
                }
                authRepository.signIn(_uiState.value.authState)
                updateAuthResult(CustomResult.Success)
            } catch (e: Exception) {
                updateAuthResult(CustomResult.DynamicError(e.toStringIfMessageIsNull()))
            }
        }
    }
    suspend fun onGoogleSignIn(): IntentSender? =
        try {
            updateAuthResult(CustomResult.InProgress)
            authRepository.googleSignIn()
        } catch (e: Exception) {
            updateAuthResult(CustomResult.DynamicError(e.message!!))
            null
        }

    fun onSignInWithIntent(activityResult: ActivityResult) = scope.launch {
        try {
            if (activityResult.resultCode != Activity.RESULT_OK && activityResult.data == null){
                updateAuthResult(CustomResult.ResourceError(R.string.couldnt_sign_in))
                return@launch
            }
            authRepository.signInWithIntent(activityResult.data!!)
            updateAuthResult(CustomResult.Success)
        } catch (e: Exception) {
            updateAuthResult(if (auth.currentUser != null) CustomResult.DynamicError(e.message!!) else CustomResult.Idle)
        }
    }
    private fun updateAuthResult(result: CustomResult) {
        _uiState.update { it.copy(authResult = result) }
    }

    data class UiState(
        val authType: AuthType = AuthType.SIGN_IN,
        val authState: AuthState = AuthState(),
        val validationState: ValidationState = ValidationState(),
        val authResult: CustomResult = CustomResult.Idle)
    data class ValidationState(
        val usernameValidationError: StringValue = StringValue.Empty,
        val emailValidationError: StringValue = StringValue.Empty,
        val passwordValidationError: StringValue = StringValue.Empty,
    )
}
data class AuthState(
    val username: String? = null,
    val email: String? = null,
    val password: String? = null,
)