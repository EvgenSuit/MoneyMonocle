package com.money.monocle.ui.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.domain.Result
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.home.AccountState
import com.money.monocle.domain.home.CurrencyFirebase
import com.money.monocle.domain.home.CurrentBalance
import com.money.monocle.domain.home.HomeRepository
import com.money.monocle.domain.home.TotalEarned
import com.money.monocle.domain.home.TotalSpent
import com.money.monocle.domain.home.WelcomeRepository
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val welcomeRepository: WelcomeRepository,
    private val dataStoreManager: DataStoreManager,
    coroutineScopeProvider: CoroutineScopeProvider
): ViewModel() {
    private val scope = coroutineScopeProvider.provide() ?: viewModelScope
    private val _welcomeScreenUiState = MutableStateFlow(WelcomeScreenUiState())
    val welcomeScreenResultFlow = _welcomeScreenUiState.map { it.result }
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    val dataFetchResultFlow = _uiState.map { it.dataFetchResult }
    val currentUser: FirebaseUser? = homeRepository.auth.currentUser

    init {
        listenForBalance()
        listenForPieChart()
    }

    private fun listenForBalance() {
        updateBalanceFetchResult(Result.InProgress)
        homeRepository.listenForBalance(
            onAccountState = {state ->
                _uiState.update { it.copy(accountState = state) }
                updateBalanceFetchResult(Result.Success(""))
                scope.launch {
                    if (state == AccountState.SIGNED_OUT || state == AccountState.DELETED) {
                        dataStoreManager.changeAccountState(false)
                        homeRepository.removeListeners()
                        if (state == AccountState.DELETED) {
                            homeRepository.signOut()
                        }
                    }
                }
            },
            onError = { updateBalanceFetchResult(Result.Error(it.message ?: it.toString())) },
            onCurrentBalance = {balance, currency ->
                scope.launch {
                    updateBalanceState(balance, currency)
                    updateBalanceFetchResult(Result.Success(""))
                    dataStoreManager.changeAccountState(true)
                }
            }
        )
    }
    private fun listenForPieChart() {
        updatePieChartFetchResult(Result.InProgress)
        homeRepository.listenForStats(
            onError = {updatePieChartFetchResult(Result.Error(it.message ?: it.toString()))},
            onPieChartData = {totalSpent, totalEarned ->
                _uiState.update { it.copy(pieChartState = PieChartState(totalSpent, totalEarned)) }
                updatePieChartFetchResult(Result.Success(""))
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
    private fun updatePieChartFetchResult(result: Result) =
        _uiState.update { it.copy(pieChartState = it.pieChartState.copy(result = result)) }
    private fun updateBalanceFetchResult(result: Result) {
        _uiState.update { it.copy(dataFetchResult = result) }
    }
    private fun updateWelcomeScreenResult(result: Result) {
        _welcomeScreenUiState.update { it.copy(result = result) }
    }

    data class UiState(
        val currentBalance: Float = 0f,
        val showWelcomeScreen: Boolean = false,
        val balanceState: BalanceState = BalanceState(),
        val pieChartState: PieChartState = PieChartState(),
        val accountState: AccountState = AccountState.NONE,
        val dataFetchResult: Result = Result.Idle
    )
    data class BalanceState(
        val currentBalance: Float = 0f,
        val currency: Int = 0
    )
    data class WelcomeScreenUiState(
        val result: Result = Result.Idle
    )
    data class PieChartState(
        val totalSpent: Float? = null,
        val totalEarned: Float? = null,
        val result: Result = Result.Idle
    )
}