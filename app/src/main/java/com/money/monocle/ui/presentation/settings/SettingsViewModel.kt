package com.money.monocle.ui.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.money.monocle.data.Balance
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.domain.Result
import com.money.monocle.domain.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    val currencyChangeResultFlow = _uiState.map { it.currencyChangeResult }

    init {
        themeFlow()
        balanceFlow()
    }
    private fun themeFlow() {
        viewModelScope.launch {
            settingsRepository.themeFlow().collectLatest {isThemeDark ->
                _uiState.update { it.copy(isThemeDark = isThemeDark) }
            }
        }
    }
    private fun balanceFlow() {
        viewModelScope.launch {
            settingsRepository.balanceFlow().collectLatest { newCurrency ->
                _uiState.update { it.copy(balance = Balance(newCurrency.first, newCurrency.second)) }
            }
        }
    }
    fun changeCurrency(newCurrency: CurrencyEnum) = viewModelScope.launch {
        val balance = _uiState.value.balance
        settingsRepository.changeCurrency(balance, newCurrency).collect {
            updateCurrencyChangeResult(it)
        }
    }
    fun changeThemeMode(isThemeDark: Boolean) = viewModelScope.launch {
        settingsRepository.changeTheme(isThemeDark)
    }
    fun signOut() = settingsRepository.signOut()
    fun updateCurrencyChangeResult(result: Result) =
        _uiState.update { it.copy(currencyChangeResult = result) }

    data class UiState(
        val isThemeDark: Boolean? = null,
        val balance: Balance = Balance(),
        val currencyChangeResult: Result = Result.Idle
    )
}