package com.money.monocle.ui.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.money.monocle.domain.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        themeFlow()
    }
    private fun themeFlow() {
        viewModelScope.launch {
            settingsRepository.themeFlow().collectLatest {isThemeDark ->
                _uiState.update { it.copy(isThemeDark = isThemeDark) }
            }
        }
    }
    fun changeThemeMode(isThemeDark: Boolean) = viewModelScope.launch {
        settingsRepository.changeTheme(isThemeDark)
    }
    fun signOut() = settingsRepository.signOut()

    data class UiState(
        val isThemeDark: Boolean? = null
    )
}