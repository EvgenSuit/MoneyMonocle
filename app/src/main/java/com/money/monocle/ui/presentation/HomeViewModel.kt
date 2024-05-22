package com.money.monocle.ui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.money.monocle.domain.Result
import com.money.monocle.domain.home.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    coroutineScopeProvider: CoroutineScopeProvider
): ViewModel() {
    private val scope = coroutineScopeProvider.provide() ?: viewModelScope
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        scope.launch {
            updateDataFetchResult(Result.InProgress)
            homeRepository.listen(
                onDoesUserExist = { exists ->
                  _uiState.update { it.copy(showWelcomeScreen = !exists) }
                  updateDataFetchResult(Result.Error(""))
                },
                onError = {}
            )
        }
    }

    private fun updateDataFetchResult(result: Result) {
        _uiState.update { it.copy(dataFetchResult = result) }
    }

    data class UiState(
        val showWelcomeScreen: Boolean = false,
        val dataFetchResult: Result = Result.Idle
    )
}