package com.money.monocle.accounts

import androidx.compose.ui.test.junit4.createComposeRule
import com.money.monocle.BaseTestClass
import com.money.monocle.data.Account
import com.money.monocle.data.Balance
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.accounts.AccountsRepository
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.mockAuth
import com.money.monocle.mockDataStoreManager
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.accounts.AccountsViewModel
import com.money.monocle.userId
import io.mockk.coVerify
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsUnitTests: BaseTestClass() {
    val composeRule = createComposeRule()
    private lateinit var viewModel: AccountsViewModel
    private val accountSlot = slot<Account>()
    private val balanceSlot = slot<Balance>()

    @Before
    fun setup() {
        auth = mockAuth()
        mockViewModel()
    }

    private fun mockViewModel(
        isMoreThanFourAccounts: Boolean = false,
        currentAccountId: String = "",
        fetchException: Exception? = null,
        creationException: Exception? = null,
        nameChangeException: Exception? = null,
        deletionException: Exception? = null,
        customAccounts: List<Account>? = null
    ) {
        firestore = mockAccountsFirestore(
            isMoreThanFourAccounts = isMoreThanFourAccounts,
            accountSlot = accountSlot,
            balanceSlot = balanceSlot,
            customAccounts = customAccounts,
            fetchException = fetchException,
            creationException = creationException,
            nameChangeException = nameChangeException,
            deletionException = deletionException
        )
        dataStoreManager = mockDataStoreManager(accountId = currentAccountId)
        val repository = AccountsRepository(auth, firestore, dataStoreManager)
        viewModel = AccountsViewModel(
            repository = repository,
            dataStoreManager = dataStoreManager,
            CoroutineScopeProvider(testScope),
        )
    }

    @Test
    fun onAccountAdd_success() = testScope.runTest {
        val name = "name"
        val balance = Balance(balance = 12f)
        viewModel.apply {
            onAccountCreate(name, balance)
            advanceUntilIdle()
        }
        assertTrue(viewModel.uiState.value.accounts.map { it.name }.contains(name))
        assertEquals(accountSlot.captured.name, name)
        firestore.apply {
            every { collection(userId).document("accounts").collection("accounts")
                .document(accountSlot.captured.id).set(accountSlot) }
        }
    }

    @Test
    fun onAccountSwitch_success() = testScope.runTest {
        val account = accounts[0]
        viewModel.onAccountSwitch(account.id)
        advanceUntilIdle()

        coVerify { dataStoreManager.setAccount(account.id) }
        assertEquals(CustomResult.Success, viewModel.uiState.value.switchResult)
    }
}