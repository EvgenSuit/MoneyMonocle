package com.money.monocle.accounts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.money.monocle.BaseTestClass
import com.money.monocle.R
import com.money.monocle.assertSnackbarIsNotDisplayed
import com.money.monocle.assertSnackbarTextEquals
import com.money.monocle.data.Account
import com.money.monocle.data.AccountName
import com.money.monocle.data.Balance
import com.money.monocle.data.simpleCurrencyMapper
import com.money.monocle.domain.accounts.AccountsRepository
import com.money.monocle.getString
import com.money.monocle.mockAuth
import com.money.monocle.mockDataStoreManager
import com.money.monocle.setContentWithSnackbar
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.presentation.accounts.AccountsViewModel
import com.money.monocle.ui.screens.accounts.AccountsScreen
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AccountsUiTests: BaseTestClass() {
    @get: Rule
    val composeRule = createComposeRule()
    private lateinit var viewModel: AccountsViewModel
    private val accountSlot = slot<Account>()
    private val balanceSlot = slot<Balance>()

    @Before
    fun setup() {
        mockViewModel()
    }

    private fun customSetContent(testActions: ComposeContentTestRule.() -> Unit) {
        composeRule.setContentWithSnackbar(snackbarScope) {
            AccountsScreen(viewModel) {}
            testScope.advanceUntilIdle()
        }
        testActions(composeRule)
    }

    private fun mockViewModel(
        isMoreThanFourAccounts: Boolean = false,
        currentAccountId: String = "",
        fetchException: Exception? = null,
        creationException: Exception? = null,
        nameChangeException: Exception? = null,
        deletionException: Exception? = null,
        customAccounts: List<Account>? = null) {
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
        val dataStoreManager = mockDataStoreManager(accountId =  currentAccountId)
        val repository = AccountsRepository(auth, firestore, dataStoreManager)
        viewModel = AccountsViewModel(
            repository = repository,
            dataStoreManager = dataStoreManager,
            CoroutineScopeProvider(testScope),
        )
    }

    @Test
    fun onAccountsFetch_success() = testScope.runTest {
        customSetContent {
            assertSnackbarIsNotDisplayed(snackbarScope)
            for (account in accounts) {
                onNodeWithText(account.name).assertIsDisplayed()
            }
        }

    }
    @Test
    fun onAccountsFetch_failure() = testScope.runTest {
        mockViewModel(fetchException = exception)
        customSetContent {
            assertSnackbarTextEquals(snackbarScope, exception.message!!)
            for (account in accounts) {
                onNodeWithText(account.name).assertIsNotDisplayed()
            }
        }
    }
    @Test
    fun onAccountsFetch_empty() = testScope.runTest {
        mockViewModel(customAccounts = listOf())
        customSetContent {
            assertSnackbarIsNotDisplayed(snackbarScope)
            for (account in accounts) {
                onNodeWithText(account.name).assertIsNotDisplayed()
            }
            onNodeWithText(getString(R.string.nothing_to_show)).assertIsDisplayed()
        }
    }

    @Test
    fun onAccountSwitch_success() = testScope.runTest {
        customSetContent {
            assertSnackbarIsNotDisplayed(snackbarScope)
            onNodeWithText(accounts[0].name).performClick()
            onNodeWithTag(accounts[0].name, useUnmergedTree = true).assertIsDisplayed()

            onNodeWithText(getString(R.string.switch_to_this_account)).performClick()
            advanceUntilIdle()

            assertSnackbarIsNotDisplayed(snackbarScope)
        }
    }

   @Test
   fun onAccountAddClick_isShownCorrectly() = testScope.runTest {
       customSetContent {
           onNodeWithContentDescription(Icons.Filled.Add.name).performClick()
           onNodeWithTag(getString(R.string.bottom_sheet)).assertIsDisplayed()
           onNodeWithTag(getString(R.string.text_field)).performTextReplacement("2.3")
           onNodeWithText(getString(R.string.next)).assertIsEnabled().performClick()

           onNodeWithTag(Icons.AutoMirrored.Filled.ArrowBack.name, useUnmergedTree = true).assertIsDisplayed()
           onNodeWithTag(getString(R.string.text_field)).assertIsDisplayed().performTextReplacement("name")
           onNodeWithText(getString(R.string.ok)).assertIsEnabled()
       }
   }

    @Test
    fun onAccountAdd_success() = testScope.runTest {
        customSetContent {
            onNodeWithContentDescription(Icons.Filled.Add.name).performClick()
            onNodeWithTag(getString(R.string.bottom_sheet)).assertIsDisplayed()
            onNodeWithTag(getString(R.string.text_field)).performTextReplacement("2.3")
            onNodeWithText(getString(R.string.next)).assertIsEnabled().performClick()

            onNodeWithTag(Icons.AutoMirrored.Filled.ArrowBack.name, useUnmergedTree = true).assertIsDisplayed()
            onNodeWithTag(getString(R.string.text_field)).performTextReplacement("name")
            onNodeWithText(getString(R.string.ok)).assertIsEnabled().performClick()

            onNodeWithText(getString(R.string.progress)).assertIsDisplayed()
            advanceUntilIdle()

            assertSnackbarIsNotDisplayed(snackbarScope)
            onNodeWithText("name").assertIsDisplayed().performClick()
            onNodeWithText("2.3 $").assertIsDisplayed()
        }
    }

    @Test
    fun onAccountAdd_failure() = testScope.runTest {
        mockViewModel(creationException = exception)
        customSetContent {
            onNodeWithContentDescription(Icons.Filled.Add.name).performClick()
            onNodeWithTag(getString(R.string.bottom_sheet)).assertIsDisplayed()
            onNodeWithTag(getString(R.string.text_field)).performTextReplacement("2.3")
            onNodeWithText(getString(R.string.next)).assertIsEnabled().performClick()

            onNodeWithTag(Icons.AutoMirrored.Filled.ArrowBack.name, useUnmergedTree = true).assertIsDisplayed()
            onNodeWithTag(getString(R.string.text_field)).assertIsFocused().performTextReplacement("name")
            onNodeWithText(getString(R.string.ok)).assertIsEnabled().performClick()

            onNodeWithText(getString(R.string.progress)).assertIsDisplayed()
            advanceUntilIdle()

            assertSnackbarTextEquals(snackbarScope, exception.message!!)
            onNodeWithTag(getString(R.string.text_field)).assertIsNotFocused()
        }
    }

    @Test
    fun onAccountAdd_failure_isMoreThanFourAccounts() = testScope.runTest {
        mockViewModel(isMoreThanFourAccounts = true)
        customSetContent {
            onNodeWithContentDescription(Icons.Filled.Add.name).performClick()
            onNodeWithTag(getString(R.string.bottom_sheet)).assertIsDisplayed()
            onNodeWithTag(getString(R.string.text_field)).performTextReplacement("2.3")
            onNodeWithText(getString(R.string.next)).assertIsEnabled().performClick()

            onNodeWithTag(Icons.AutoMirrored.Filled.ArrowBack.name, useUnmergedTree = true).assertIsDisplayed()
            onNodeWithTag(getString(R.string.text_field)).performTextReplacement("name")
            onNodeWithText(getString(R.string.ok)).assertIsEnabled().performClick()

            onNodeWithText(getString(R.string.progress)).assertIsDisplayed()
            advanceUntilIdle()

            assertSnackbarTextEquals(snackbarScope, getString(R.string.max_accounts_restriction))
        }
    }

    @Test
    fun onAccountSelect_detailsShown_notCurrentAccount() = testScope.runTest {
        val i = 1
        customSetContent {
            onNodeWithText(accounts[i].name).performClick()
            onNodeWithTag(accounts[i].name, useUnmergedTree = true).assertIsDisplayed()

            onNodeWithText("${balances[i].balance} ${simpleCurrencyMapper(balances[i].currency)}", useUnmergedTree = true).assertIsDisplayed()
            onNodeWithContentDescription(Icons.Filled.Edit.name).assertIsDisplayed()
            onNodeWithText(getString(R.string.switch_to_this_account)).assertIsDisplayed()
            onNodeWithTag(getString(R.string.text_field)).assertIsNotDisplayed()
            onNodeWithText(getString(R.string.switch_to_this_account)).assertIsDisplayed()
            onNodeWithText(getString(R.string.delete_account)).assertIsDisplayed()
        }
    }

    @Test
    fun onAccountSelect_detailsShown_currentAccount() = testScope.runTest {
        val i = 1
        mockViewModel(currentAccountId = accounts[i].id)
        customSetContent {
            onNodeWithText(accounts[i].name).performClick()
            onNodeWithTag(accounts[i].name, useUnmergedTree = true).assertIsDisplayed()

            onNodeWithText("${balances[i].balance} ${simpleCurrencyMapper(balances[i].currency)}", useUnmergedTree = true).assertIsDisplayed()
            onNodeWithContentDescription(Icons.Filled.Edit.name).assertIsDisplayed()
            onNodeWithText(getString(R.string.switch_to_this_account)).assertIsNotDisplayed()
            onNodeWithTag(getString(R.string.text_field)).assertIsNotDisplayed()
            onNodeWithText(getString(R.string.delete_account)).assertIsDisplayed()
        }
    }
    @Test
    fun onAccountSelect_detailsShown_mainAccount() = testScope.runTest {
        val i = accounts.map { it.id }.indexOf(AccountName.MAIN.name)
        mockViewModel(currentAccountId = AccountName.MAIN.name)
        customSetContent {
            onNodeWithText(accounts[i].name).performClick()
            onNodeWithTag(accounts[i].name, useUnmergedTree = true).assertIsDisplayed()

            onNodeWithText("${balances[i].balance} ${simpleCurrencyMapper(balances[i].currency)}", useUnmergedTree = true).assertIsDisplayed()
            onNodeWithContentDescription(Icons.Filled.Edit.name).assertIsDisplayed()
            onNodeWithText(getString(R.string.switch_to_this_account)).assertIsNotDisplayed()
            onNodeWithTag(getString(R.string.text_field)).assertIsNotDisplayed()
            onNodeWithText(getString(R.string.delete_account)).assertIsNotDisplayed()
        }
    }
    @Test
    fun onAccountDelete_success() = testScope.runTest {
        customSetContent {
            onNodeWithText(accounts[0].name).performClick()
            onNodeWithText(getString(R.string.delete_account)).performClick()
            onNodeWithText(getString(R.string.progress)).assertIsDisplayed()
            onNodeWithTag(accounts[0].name, useUnmergedTree = true).assertIsNotDisplayed()
            advanceUntilIdle()

            assertSnackbarIsNotDisplayed(snackbarScope)
            onNodeWithText(accounts[0].name).assertIsNotDisplayed()
            onNodeWithTag(getString(R.string.bottom_sheet)).assertIsNotDisplayed()
        }
    }

    @Test
    fun onAccountDelete_failure() = testScope.runTest {
        mockViewModel(deletionException = exception)
        customSetContent {
            onNodeWithText(accounts[0].name).performClick()
            onNodeWithText(getString(R.string.delete_account)).performClick()
            onNodeWithText(getString(R.string.progress)).assertIsDisplayed()
            advanceUntilIdle()

            assertSnackbarTextEquals(snackbarScope, exception.message!!)
            onNodeWithTag(accounts[0].name, useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    fun onNameEdit_success() = testScope.runTest {
        val i = 2
        val newName = "account $i new name"
        customSetContent {
            val nodesToCheck = listOf(onNodeWithTag(getString(R.string.text_field)), onNodeWithText(getString(R.string.ok)),
                onNodeWithTag(Icons.AutoMirrored.Filled.ArrowBack.name, useUnmergedTree = true))

            onNodeWithText(accounts[i].name).performClick()
            onNodeWithContentDescription(Icons.Filled.Edit.name).performClick()
            onNodeWithText(getString(R.string.ok)).assertIsNotEnabled()
            onNodeWithTag(getString(R.string.text_field)).assertIsFocused().performTextReplacement(newName)
            onNodeWithText(getString(R.string.ok)).assertIsEnabled().performClick()

            for (node in nodesToCheck) {
                node.assertIsNotEnabled()
            }

            advanceUntilIdle()
            assertSnackbarIsNotDisplayed(snackbarScope)
            onNodeWithTag(accounts[i].name, useUnmergedTree = true).assertIsNotDisplayed()
            onNodeWithTag(newName, useUnmergedTree = true).assertIsDisplayed()
            onNodeWithContentDescription(Icons.Filled.Edit.name).performClick()
            for (node in nodesToCheck) {
                node.assertIsEnabled()
            }
            onNodeWithTag(getString(R.string.bottom_sheet)).performTouchInput { swipeDown() }.assertIsNotDisplayed()
            onNodeWithText(newName).assertIsDisplayed()

        }
    }
    @Test
    fun onNameEdit_failure() = testScope.runTest {
        val i = 2
        mockViewModel(nameChangeException = exception)
        val newName = "account $i new name"
        customSetContent {
            onNodeWithText(accounts[i].name).performClick()
            onNodeWithContentDescription(Icons.Filled.Edit.name).performClick()
            onNodeWithText(getString(R.string.ok)).assertIsNotEnabled()
            onNodeWithTag(getString(R.string.text_field)).performTextReplacement(newName)
            onNodeWithText(getString(R.string.ok)).assertIsEnabled().performClick()

            advanceUntilIdle()
            assertSnackbarTextEquals(snackbarScope, exception.message!!)
            onNodeWithTag(accounts[i].name, useUnmergedTree = true).assertIsDisplayed()
            onNodeWithTag(newName, useUnmergedTree = true).assertIsNotDisplayed()
            onNodeWithTag(getString(R.string.bottom_sheet)).performTouchInput { swipeDown() }.assertIsNotDisplayed()
            onNodeWithText(newName).assertIsNotDisplayed()
        }
    }
}