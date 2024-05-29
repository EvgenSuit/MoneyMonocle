package com.money.monocle.ui.presentation

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.money.monocle.domain.Result
import com.money.monocle.domain.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    coroutineScopeProvider: CoroutineScopeProvider
): ViewModel() {
    private val scope = coroutineScopeProvider.provide() ?: viewModelScope
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    val authResultFlow = _uiState.map { it.authResult }
    val auth = authRepository.authRef

    suspend fun onSignIn(): IntentSender? =
        try {
            updateResult(Result.InProgress)
            authRepository.signIn()
        } catch (e: Exception) {
            updateResult(Result.Error(e.message!!))
            null
        }

    fun onSignInWithIntent(activityResult: ActivityResult) = scope.launch {
        try {
            if (activityResult.resultCode != Activity.RESULT_OK && activityResult.data == null) throw Exception("Couldn't sign in")
            authRepository.signInWithIntent(activityResult.data!!)
            updateResult(Result.Success(""))
        } catch (e: Exception) {
            // status 16 means cancellation of intent by the user
            updateResult(if (auth.currentUser != null) Result.Error(e.message!!) else Result.Idle)
        }
    }

    private fun updateResult(result: Result) {
        _uiState.update { it.copy(authResult = result) }
    }

    data class UiState(val authResult: Result = Result.Idle)
}