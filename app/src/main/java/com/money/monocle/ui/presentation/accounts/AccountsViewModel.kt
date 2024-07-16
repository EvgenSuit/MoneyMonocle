package com.money.monocle.ui.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.money.monocle.data.Account
import com.money.monocle.data.AccountName
import com.money.monocle.data.Balance
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.accounts.AccountBalance
import com.money.monocle.domain.accounts.AccountsRepository
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.isSuccess
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repository: AccountsRepository,
    private val dataStoreManager: DataStoreManager,
    scopeProvider: CoroutineScopeProvider
): ViewModel() {
    private val scope = scopeProvider.provide() ?: viewModelScope
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        scope.launch {
            fetchCurrentAccount()
            fetchAccounts()
            dataStoreManager.accountFlow().collect { account ->
                _uiState.update { it.copy(currentAccountId = account,
                    accounts = it.accounts.moveToFirst { it.id == account }) }
            }
        }

    }
    private suspend fun fetchCurrentAccount() {
        updateFetchResult(CustomResult.InProgress)
        updateCurrentAccountId(dataStoreManager.accountFlow().first())
    }
    private suspend fun fetchAccounts() {
        repository.fetchAccounts(
            onAccounts = { accounts ->
                _uiState.update { it.copy(accounts = accounts.moveToFirst { it.id == _uiState.value.currentAccountId }) }
            },
            onBalances = {balances ->
                _uiState.update { it.copy(balances = balances) }
            }
        ).collectLatest {
            updateFetchResult(if (it.isSuccess() && _uiState.value.accounts.isEmpty()) CustomResult.Empty else it)
        }
    }

    fun onAccountCreate(name: String, balance: Balance) {
        val id = UUID.randomUUID().toString()
        val account = Account(id, name, Instant.now().toEpochMilli())
        updateCreationResult(CustomResult.InProgress)
        scope.launch {
            repository.createAccount(account, balance).collectLatest { res ->
                if (res.isSuccess()) {
                    _uiState.update { it.copy(balances = it.balances + AccountBalance(id, balance)) }
                    _uiState.update { it.copy(accounts = it.accounts + account) }
                }
                updateCreationResult(res)
            }
        }
    }

    fun onAccountDelete(accountId: String) {
        updateDeletionResult(CustomResult.InProgress)
        scope.launch {
            repository.deleteAccount(accountId).collectLatest {res ->
                if (res.isSuccess()) {
                    _uiState.update { it.copy(accounts = it.accounts.filter { it.id != accountId }) }
                }
                updateDeletionResult(res)
            }
        }
    }
    fun onAccountNameEdit(accountId: String, name: String) {
        updateNameEditResult(CustomResult.InProgress)
        scope.launch {
            repository.editAccountName(accountId, name).collectLatest {res ->
                if (res.isSuccess()) _uiState.update { it.copy(accounts = it.accounts.map { if (it.id == accountId) it.copy(name = name) else it }) }
                updateNameEditResult(res)
            }
        }
    }

    fun onAccountSwitch(accountId: String) {
        updateSwitchResult(CustomResult.InProgress)
        scope.launch {
            repository.switchAccount(accountId).collectLatest { res ->
                updateSwitchResult(res)
            }
        }
    }

    private fun List<Account>.moveToFirst(predicate: (Account) -> Boolean): List<Account> {
        val mutableList = this.toMutableList()
        val item = mutableList.find(predicate)
        if (item != null) {
            mutableList.remove(item)
            mutableList.add(0, item)
        }
        return mutableList
    }

    private fun updateCurrentAccountId(accountId: String) =
        _uiState.update { it.copy(currentAccountId = accountId) }

    fun updateCreationResult(result: CustomResult) =
        _uiState.update { it.copy(creationResult = result) }

    fun updateDeletionResult(result: CustomResult) =
        _uiState.update { it.copy(deletionResult = result) }

    private fun updateNameEditResult(result: CustomResult) =
        _uiState.update { it.copy(nameEditResult = result) }

    fun updateSwitchResult(result: CustomResult) =
        _uiState.update { it.copy(switchResult = result) }

    private fun updateFetchResult(result: CustomResult) =
        _uiState.update { it.copy(fetchResult = result) }

    data class UiState(
        val currentAccountId: String? = null,
        val accounts: List<Account> = listOf(),
        val balances: List<AccountBalance> = listOf(),
        val creationResult: CustomResult = CustomResult.Idle,
        val nameEditResult: CustomResult = CustomResult.Idle,
        val deletionResult: CustomResult = CustomResult.Idle,
        val switchResult: CustomResult = CustomResult.Idle,
        val fetchResult: CustomResult = CustomResult.Idle
    )
}