package com.money.monocle.ui.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.home.AccountState
import com.money.monocle.domain.home.CurrencyFirebase
import com.money.monocle.domain.home.CurrentBalance
import com.money.monocle.domain.home.HomeRepository
import com.money.monocle.domain.home.WelcomeRepository
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val welcomeRepository: WelcomeRepository,
    coroutineScopeProvider: CoroutineScopeProvider
): ViewModel() {
    private val scope = coroutineScopeProvider.provide() ?: viewModelScope
    private val _welcomeScreenUiState = MutableStateFlow(WelcomeScreenUiState())
    val welcomeScreenUiState = _welcomeScreenUiState.asStateFlow()
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    val currentUser: FirebaseUser? = homeRepository.auth.currentUser

    init {
        getUsername()
        listenForPieChart()
        listenForBalance()
    }

    private fun getUsername() {
        _uiState.update { it.copy(username = currentUser?.displayName ?: "") }
    }

    private fun listenForBalance() {
        updateBalanceFetchResult(CustomResult.InProgress)
        homeRepository.listenForBalance(
            scope = scope,
            onAccountState = {state ->
                _uiState.update { it.copy(accountState = state) }
                updateBalanceFetchResult(CustomResult.Success)
            },
            onError = { updateBalanceFetchResult(CustomResult.DynamicError(it.message ?: it.toString())) },
            onCurrentBalance = {balance, currency ->
                updateBalanceState(balance, currency)
                updateBalanceFetchResult(CustomResult.Success)
            }
        )
    }
    private fun listenForPieChart() {
        updatePieChartFetchResult(CustomResult.InProgress)
        homeRepository.listenForStats(
            onError = {updatePieChartFetchResult(CustomResult.DynamicError(it.message ?: it.toString()))},
            onPieChartData = {totalSpent, totalEarned ->
                _uiState.update { it.copy(pieChartState = PieChartState(totalSpent, totalEarned)) }
                updatePieChartFetchResult(CustomResult.Success)
            }
        )
    }
    fun setBalance(currency: CurrencyEnum, amount: Float) = scope.launch {
        welcomeRepository.setBalance(currency, amount).collectLatest {
            updateWelcomeScreenResult(it)
        }
    }

    private fun updateBalanceState(balance: CurrentBalance,
                                   currencyFirebase: CurrencyFirebase) {
        _uiState.update { it.copy(balanceState
        = it.balanceState.copy(balance, currencyFirebase)) }
    }
    private fun updatePieChartFetchResult(result: CustomResult) =
        _uiState.update { it.copy(pieChartState = it.pieChartState.copy(result = result)) }
    private fun updateBalanceFetchResult(result: CustomResult) {
        _uiState.update { it.copy(dataFetchResult = result) }
    }
    private fun updateWelcomeScreenResult(result: CustomResult) {
        _welcomeScreenUiState.update { it.copy(result = result) }
    }

    data class UiState(
        val username: String = "",
        val currentBalance: Float = 0f,
        val showWelcomeScreen: Boolean = false,
        val balanceState: BalanceState = BalanceState(),
        val pieChartState: PieChartState = PieChartState(),
        val accountState: AccountState = AccountState.NONE,
        val dataFetchResult: CustomResult = CustomResult.Idle
    )
    data class BalanceState(
        val currentBalance: Float = 0f,
        val currency: Int = 0
    )
    data class WelcomeScreenUiState(
        val result: CustomResult = CustomResult.Idle
    )
    data class PieChartState(
        val totalSpent: Float? = null,
        val totalEarned: Float? = null,
        val result: CustomResult = CustomResult.Idle
    )
}