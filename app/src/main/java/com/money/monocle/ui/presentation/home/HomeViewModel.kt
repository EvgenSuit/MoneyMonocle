package com.money.monocle.ui.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.money.monocle.data.Balance
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.home.AccountState
import com.money.monocle.domain.home.HomeRepository
import com.money.monocle.domain.home.WelcomeRepository
import com.money.monocle.domain.isError
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.toStringIfMessageIsNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val welcomeRepository: WelcomeRepository,
    private val dataStoreManager: DataStoreManager,
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
        scope.launch {
            dataStoreManager.accountFlow().collect { accountId ->
                if (_uiState.value.currentAccountId != accountId) {
                    _uiState.update { it.copy(currentAccountId = accountId) }
                    withContext(Dispatchers.Main) {
                        listen()
                    }
                }
            }
        }
    }

    private fun listen() {
        listenForPieChart()
        listenForBalance()
    }

    private fun getUsername() {
        _uiState.update { it.copy(username = currentUser?.displayName ?: "") }
    }

    private fun listenForBalance() {
        updateBalanceFetchResult(CustomResult.InProgress)
        homeRepository.listenForBalance(
            currentAccountId = _uiState.value.currentAccountId,
            scope = scope,
            onAccountState = {state ->
                _uiState.update { it.copy(accountState = state) }
                updateBalanceFetchResult(CustomResult.Success)
            },
            onError = { updateBalanceFetchResult(CustomResult.DynamicError(it.toStringIfMessageIsNull()))},
            onCurrentBalance = {balance ->
                _uiState.update { it.copy(balance = balance) }
                updateBalanceFetchResult(CustomResult.Success)
            }
        )
    }
    private fun listenForPieChart() {
        updatePieChartFetchResult(CustomResult.InProgress)
        homeRepository.listenForStats(
            currentAccountId = _uiState.value.currentAccountId,
            onError = {updatePieChartFetchResult(CustomResult.DynamicError(it.message ?: it.toString()))},
            onPieChartData = {totalSpent, totalEarned ->
                _uiState.update { it.copy(pieChartState = PieChartState(totalSpent, totalEarned)) }
                updatePieChartFetchResult(CustomResult.Success)
            }
        )
    }
    fun retryIfNecessary() {
        if (_uiState.value.dataFetchResult.isError()) listenForBalance()
        if (_uiState.value.pieChartState.result.isError()) listenForPieChart()
    }
    fun setBalance(balance: Balance) = scope.launch {
        welcomeRepository.setBalance(balance,
            _uiState.value.currentAccountId).collectLatest {
            updateWelcomeScreenResult(it)
        }
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
        val balance: Balance = Balance(),
        val pieChartState: PieChartState = PieChartState(),
        val currentAccountId: String = "",
        val accountState: AccountState = AccountState.NONE,
        val dataFetchResult: CustomResult = CustomResult.Idle
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