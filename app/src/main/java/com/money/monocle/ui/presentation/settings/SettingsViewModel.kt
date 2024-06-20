package com.money.monocle.ui.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.isError
import com.money.monocle.domain.settings.SettingsRepository
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.toStringIfMessageIsNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    scopeProvider: CoroutineScopeProvider
): ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    private val scope = scopeProvider.provide() ?: viewModelScope
    val uiState = _uiState.asStateFlow()

    init {
        themeFlow()
        balanceFlow()
    }
    private fun themeFlow() {
        scope.launch {
            settingsRepository.themeFlow().collectLatest {isThemeDark ->
                _uiState.update { it.copy(isThemeDark = isThemeDark) }
            }
        }
    }
    private fun balanceFlow() {
        scope.launch {
            settingsRepository.balanceFlow().collectLatest { newBalance ->
                _uiState.update { it.copy(balance = newBalance) }
            }
        }
    }
    fun checkLastTimeUpdated() = scope.launch {
        updateLastTimeUpdatedResult(CustomResult.InProgress)
        settingsRepository.listenForLastTimeUpdated(
            onData = {lastTimeUpdated ->
                _uiState.update { it.copy(lastTimeCurrencyUpdated = lastTimeUpdated,
                    lastTimeCurrencyUpdatedResult = CustomResult.Success
                ) }
            },
            onError = {updateLastTimeUpdatedResult(CustomResult.DynamicError(it))})
    }
    fun retryIfNecessary() {
        if (_uiState.value.lastTimeCurrencyUpdatedResult.isError()) checkLastTimeUpdated()
    }
    fun changeCurrency(newCurrency: CurrencyEnum) = scope.launch {
        val balance = _uiState.value.balance
        settingsRepository.changeCurrency(balance, newCurrency)
            .catch { updateCurrencyChangeResult(CustomResult.DynamicError(it.message ?: it.toString())) }
            .collect { updateCurrencyChangeResult(it) }
    }
    fun changeLastTimeUpdated(lastTimeUpdated: Long = -1) = scope.launch {
        try {
            updateLastTimeUpdatedResult(CustomResult.InProgress)
            settingsRepository.changeLastTimeUpdated(lastTimeUpdated)
            updateLastTimeUpdatedResult(CustomResult.Success)
        } catch (e: Exception) {
            updateLastTimeUpdatedResult(CustomResult.DynamicError(e.toStringIfMessageIsNull()))
        }
    }
    fun changeThemeMode(isThemeDark: Boolean) = scope.launch {
        settingsRepository.changeTheme(isThemeDark)
    }
    fun signOut() = settingsRepository.signOut()
    fun updateCurrencyChangeResult(result: CustomResult) =
        _uiState.update { it.copy(currencyChangeResult = result) }
    fun updateLastTimeUpdatedResult(result: CustomResult) =
        _uiState.update { it.copy(lastTimeCurrencyUpdatedResult = result) }

    data class UiState(
        val isThemeDark: Boolean? = null,
        val balance: Balance = Balance(),
        val currencyChangeResult: CustomResult = CustomResult.Idle,
        val lastTimeCurrencyUpdated: Long? = null,
        val lastTimeCurrencyUpdatedResult: CustomResult = CustomResult.Idle
    )
}