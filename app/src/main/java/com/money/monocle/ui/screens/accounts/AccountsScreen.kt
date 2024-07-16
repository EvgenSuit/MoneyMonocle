package com.money.monocle.ui.screens.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.money.monocle.LocalSnackbarController
import com.money.monocle.R
import com.money.monocle.data.Account
import com.money.monocle.data.AccountName
import com.money.monocle.data.Balance
import com.money.monocle.data.simpleCurrencyMapper
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.isEmpty
import com.money.monocle.domain.isError
import com.money.monocle.domain.isInProgress
import com.money.monocle.domain.isSuccess
import com.money.monocle.ui.presentation.accounts.AccountsViewModel
import com.money.monocle.ui.screens.components.AccountTextField
import com.money.monocle.ui.screens.components.AnimatedItem
import com.money.monocle.ui.screens.components.BackButton
import com.money.monocle.ui.screens.components.CommonButton
import com.money.monocle.ui.screens.components.CurrencySelection
import com.money.monocle.ui.screens.components.CustomTopBar
import com.money.monocle.ui.screens.components.InProgressLinearIndicator
import com.money.monocle.ui.screens.components.LOTTIE_SPEED
import com.money.monocle.ui.screens.components.NothingToShowText
import com.money.monocle.ui.screens.components.SuccessLottieAnimation
import com.money.monocle.ui.theme.MoneyMonocleTheme
import java.util.UUID

@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarController = LocalSnackbarController.current
    var selectedAccount by remember {
        mutableStateOf(Account())
    }
    LaunchedEffect(uiState.switchResult) {
        snackbarController.showSnackbar(uiState.switchResult)
    }
    LaunchedEffect(uiState.fetchResult) {
        snackbarController.showSnackbar(uiState.fetchResult)
    }
    LaunchedEffect(uiState.creationResult) {
        snackbarController.showSnackbar(uiState.creationResult)
    }
    LaunchedEffect(uiState.deletionResult) {
        if (!uiState.accounts.map { it.id }.contains(selectedAccount.id)) selectedAccount = Account()
        snackbarController.showSnackbar(uiState.deletionResult)
    }
    LaunchedEffect(uiState.nameEditResult) {
        selectedAccount = uiState.accounts.firstOrNull { it.id == selectedAccount.id } ?: selectedAccount
        snackbarController.showSnackbar(uiState.nameEditResult)
    }
    AccountsContent(
        uiState = uiState,
        selectedAccount = selectedAccount,
        onSelect = { selectedAccount = it },
        onNavigateBack = onNavigateBack,
        onCreate = viewModel::onAccountCreate,
        onNameEdit = viewModel::onAccountNameEdit,
        onDelete = viewModel::onAccountDelete,
        onDismiss = {
            viewModel.updateSwitchResult(CustomResult.Idle)
            viewModel.updateCreationResult(CustomResult.Idle)
            viewModel.updateDeletionResult(CustomResult.Idle)
        },
        onSwitch = viewModel::onAccountSwitch)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsContent(
    uiState: AccountsViewModel.UiState,
    selectedAccount: Account,
    onSelect: (Account) -> Unit,
    onNavigateBack: () -> Unit,
    onCreate: (String, Balance) -> Unit,
    onDelete: (String) -> Unit,
    onNameEdit: (String, String) -> Unit,
    onDismiss: () -> Unit,
    onSwitch: (String) -> Unit
) {
    val currentAccountId = uiState.currentAccountId
    var showNameEdit by remember {
        mutableStateOf(false)
    }
    var showAddAccount by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(uiState.nameEditResult) {
        if (uiState.nameEditResult.isSuccess()) showNameEdit = false
    }
    if (currentAccountId != null) {
        Scaffold(
            topBar = {
                CustomTopBar(
                    textId = R.string.financial_accounts,
                    result = uiState.fetchResult,
                    onNavigateBack = onNavigateBack
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddAccount = true }) {
                    val icon = Icons.Filled.Add
                    Icon(icon, contentDescription = icon.name)
                }
            }
        ) {padding ->
            AccountsColumn(
                accountsList = {
                    AccountsList(accounts = uiState.accounts,
                        currentAccountId = currentAccountId,
                        selectedAccount = selectedAccount,
                        onSelect = onSelect)
                },
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize())
            if (uiState.fetchResult.isEmpty()) NothingToShowText()
            if (selectedAccount.name.isNotEmpty()) {
                AccountDetailsSheet(
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    onDismiss = {
                        onDismiss()
                        showNameEdit = false
                        onSelect(Account()) },
                    accountDetailsColumn = {
                        val deletionResult = uiState.deletionResult
                        if (deletionResult.isInProgress()) InProgressLinearIndicator()
                        else if (deletionResult.isSuccess() && selectedAccount.id != currentAccountId) {
                            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.success))
                            val progress = animateLottieCompositionAsState(composition,
                                speed = LOTTIE_SPEED,
                                isPlaying = deletionResult.isSuccess())
                            SuccessLottieAnimation(composition = composition, progress = progress,
                                result = deletionResult, onDismiss = {
                                    onDismiss()
                                    showNameEdit = false
                                    onSelect(Account())
                                })
                        } else {
                            val enabled = !uiState.nameEditResult.isInProgress()
                            AccountDetailsColumn(
                                focusOnLaunch = !uiState.nameEditResult.isError(),
                                balance = uiState.balances.firstOrNull { it.accountId == selectedAccount.id }?.balance ?: Balance(),
                                showNameEdit = showNameEdit,
                                onNameEdit = { onNameEdit(selectedAccount.id, it) },
                                enabled = enabled,
                                nameEditButton = {
                                    NameEditButton(
                                        enabled = enabled,
                                        accountName = selectedAccount.name,
                                        showNameEdit = showNameEdit,
                                        onShowNameEdit = { showNameEdit = it })
                                },
                                controlButtons = {
                                    ControlButtons(accountId = selectedAccount.id,
                                        currentAccountId = currentAccountId,
                                        onDelete = onDelete,
                                        onSwitch = onSwitch) })
                        }
                    }
                )
            }
            if (showAddAccount) {
                var name by remember {
                    mutableStateOf("")
                }
                var balance by remember {
                    mutableStateOf(Balance())
                }
                AddAccountSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    content = {
                        var showCurrencySelection by remember {
                            mutableStateOf(true)
                        }
                        val creationResult = uiState.creationResult
                      if (creationResult.isInProgress()) InProgressLinearIndicator()
                      else if (creationResult.isSuccess()) {
                          val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.success))
                          val progress = animateLottieCompositionAsState(composition, speed = LOTTIE_SPEED,
                              isPlaying = creationResult.isSuccess())
                          SuccessLottieAnimation(composition = composition, progress = progress,
                              result = creationResult, onDismiss = {
                                  onDismiss()
                                  showAddAccount = false })
                      } else {
                          AddAccountSheetContent(
                              selectionSubcategory = {
                                  if(showCurrencySelection) {
                                      CurrencySelection(
                                          buttonTextId = R.string.next,
                                          onBalance = {
                                              balance = it
                                              showCurrencySelection = false
                                          })
                                  } else {
                                      NameEditColumn(
                                          focusOnLaunch = !creationResult.isError(),
                                          enabled = !creationResult.isInProgress(),
                                          newName = name,
                                          onNewName = { name = it },
                                          onNameEdit = {
                                              onCreate(it, balance)
                                          })
                                  }
                              },
                              showCurrencySelection = showCurrencySelection,
                              onShowCurrencySelection = { showCurrencySelection = it }
                          )
                      }
                    },
                    onDismiss = {
                        onDismiss()
                        showAddAccount = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountSheet(
    sheetState: SheetState,
    content: @Composable ColumnScope.() -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(stringResource(id = R.string.bottom_sheet))) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.items_list_padding)+10.dp),
            modifier = Modifier
                .padding(10.dp)
                .padding(bottom = dimensionResource(id = R.dimen.sheet_bottom_padding))
        ) {
            content()
        }
    }
}

@Composable
fun ColumnScope.AddAccountSheetContent(
    selectionSubcategory: @Composable ColumnScope.() -> Unit,
    showCurrencySelection: Boolean,
    onShowCurrencySelection: (Boolean) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!showCurrencySelection) BackButton(onBack = { onShowCurrencySelection(true) },
            modifier = Modifier.testTag(Icons.AutoMirrored.Filled.ArrowBack.name))
        Text(stringResource(id = if (showCurrencySelection) R.string.enter_balance else R.string.give_a_name_to_account),
            style = MaterialTheme.typography.titleMedium)
    }
    selectionSubcategory()
}

@Composable
fun AccountsColumn(
    accountsList: @Composable () -> Unit,
    modifier: Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.items_list_spacing)),
        modifier = modifier
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.items_list_padding))
    ) {
        accountsList()
    }
}

@Composable
fun AccountsList(
    accounts: List<Account>,
    currentAccountId: String,
    selectedAccount: Account,
    onSelect: (Account) -> Unit
) {
    for (account in accounts) {
        AnimatedItem {
            AccountItem(
                isCurrentAccount = account.id == currentAccountId,
                isSelected = account == selectedAccount,
                account = account,
                onSelect = onSelect)
        }
    }
}

@Composable
fun AccountItem(
    isCurrentAccount: Boolean,
    isSelected: Boolean,
    account: Account,
    onSelect: (Account) -> Unit
) {
    val shape = RoundedCornerShape(dimensionResource(id = R.dimen.button_corner))
    ElevatedCard(onClick = { onSelect(account) },
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else
            MaterialTheme.colorScheme.background
        ),
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .fillMaxWidth()
            .then(
                if (isCurrentAccount) Modifier.border(
                    width = 1.dp,
                    shape = shape,
                    color = MaterialTheme.colorScheme.primary
                ) else Modifier
            )
        ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(id = R.dimen.list_items_padding))
        ) {
            Text(text = account.name,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.displayMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    accountDetailsColumn: @Composable () -> Unit,
) {
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(stringResource(id = R.string.bottom_sheet))) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .padding(bottom = dimensionResource(id = R.dimen.sheet_bottom_padding))
        ) {
            accountDetailsColumn()
        }
    }
}

@Composable
fun AccountDetailsColumn(
    focusOnLaunch: Boolean,
    balance: Balance,
    enabled: Boolean,
    showNameEdit: Boolean,
    onNameEdit: (String) -> Unit,
    nameEditButton: @Composable () -> Unit,
    controlButtons: @Composable () -> Unit) {
    var newName by remember {
        mutableStateOf("")
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        nameEditButton()
        if (!showNameEdit) Text("${balance.balance} ${simpleCurrencyMapper(balance.currency)}",
            style = MaterialTheme.typography.labelMedium)
        if (!showNameEdit) {
            controlButtons()
        }
        AnimatedVisibility(showNameEdit) {
            NameEditColumn(
                focusOnLaunch = focusOnLaunch,
                enabled = enabled,
                newName = newName,
                onNewName = { newName = it },
                onNameEdit = onNameEdit)
        }
    }
}

@Composable
fun NameEditButton(
    enabled: Boolean,
    accountName: String,
    showNameEdit: Boolean,
    onShowNameEdit: (Boolean) -> Unit
) {
    val icon = if (!showNameEdit) Icons.Filled.Edit else Icons.AutoMirrored.Filled.ArrowBack
    TextButton(
        enabled = enabled,
        onClick = { onShowNameEdit(!showNameEdit) },
        modifier = Modifier.testTag(icon.name)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(imageVector = icon, contentDescription = icon.name)
            Text(text = accountName,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.testTag(accountName))
        }
    }
}

@Composable
fun NameEditColumn(
    focusOnLaunch: Boolean = true,
    enabled: Boolean,
    newName: String,
    onNewName: (String) -> Unit,
    onNameEdit: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        AccountTextField(
            focusOnLaunch = focusOnLaunch,
            enabled = enabled,
            value = newName,
            onValueChange = onNewName)
        CommonButton(
            enabled = newName.isNotBlank() && enabled,
            onClick = { onNameEdit(newName) },
            textId = R.string.ok)
    }
}

@Composable
fun ControlButtons(
    accountId: String,
    currentAccountId: String,
    onDelete: (String) -> Unit,
    onSwitch: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (accountId != currentAccountId) CommonButton(onClick = { onSwitch(accountId) },
            textId = R.string.switch_to_this_account)
        if (accountId != AccountName.MAIN.name) {
            CommonButton(
                onClick = { onDelete(accountId) },
                modifier = Modifier
                    .border(width = 1.dp,
                        shape = RoundedCornerShape(dimensionResource(id = R.dimen.button_corner)),
                        color = MaterialTheme.colorScheme.onBackground),
                colors = ButtonDefaults.buttonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                    containerColor = MaterialTheme.colorScheme.background
                ),
                textId = R.string.delete_account
            )
        }
    }
}



@Preview
@Composable
fun AccountsScreenPreview() {
    val accountId = UUID.randomUUID().toString()
    val accounts = listOf(Account(accountId, name = "name")) + List(5) {
        Account(name = "name $it")
    }
    MoneyMonocleTheme {
        Surface {
            AccountsContent(
                uiState = AccountsViewModel.UiState(
                    fetchResult = CustomResult.Success,
                    currentAccountId = accountId,
                    accounts = accounts
                ),
                selectedAccount = Account(),
                onSelect = {},
                onDelete = {},
                onNameEdit = {_, _ ->},
                onNavigateBack = {},
                onCreate = {_, _ -> },
                onDismiss = {},
                onSwitch = {}
            )
        }
    }
}

/*@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun AddAccountSheetPreview() {
    val showCurrencySelection = true
    val creationResult = CustomResult.Success
    MoneyMonocleTheme {
        Surface {
            AddAccountSheet(sheetState = rememberStandardBottomSheetState(),
                content = {
                    if (creationResult.isInProgress()) InProgressLinearIndicator()
                    else if (creationResult.isSuccess()) {
                        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.success))
                        val progress = animateLottieCompositionAsState(composition, speed = LOTTIE_SPEED,
                            isPlaying = creationResult.isSuccess())
                        SuccessLottieAnimation(composition = composition, progress = progress,
                            result = creationResult, onDismiss = {  })
                    } else {
                  AddAccountSheetContent(
                      selectionSubcategory = {
                        if (showCurrencySelection) {
                            CurrencySelection(buttonTextId = R.string.next) {}
                        } else {
                            NameEditColumn(enabled = true, newName = "name", onNewName = {}) {}
                        }
                      },
                      showCurrencySelection = showCurrencySelection,
                      onShowCurrencySelection = {}
                  )
                }
                          },
                onDismiss = {})
        }
    }
}*/

/*
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun AccountDetailsPreview() {
    val accountId = UUID.randomUUID().toString()
    val accounts = listOf(Account(accountId, name = "name"))
    MoneyMonocleTheme {
        Surface {
            AccountDetailsSheet(
                sheetState = rememberStandardBottomSheetState(),
                onDismiss = {},
                accountDetailsColumn = {
                    AccountDetailsColumn(
                        balance = Balance(currency = CurrencyEnum.USD.ordinal),
                        showNameEdit = false,
                        onNameEdit = {},
                        nameEditButton = {
                             NameEditButton(accountName = accounts[0].name,
                                 showNameEdit = false) {

                             }
                        },
                        controlButtons = {
                            ControlButtons(accountId = accountId, currentAccountId = AccountName.MAIN.name) {

                            }
                        })
                }
            )

        }
    }
}*/
