package com.money.monocle.ui.screens.home

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.money.monocle.R
import com.money.monocle.data.simpleCurrencyMapper
import com.money.monocle.domain.Result
import com.money.monocle.domain.home.AccountState
import com.money.monocle.ui.presentation.HomeViewModel
import com.money.monocle.ui.screens.WelcomeScreen
import com.money.monocle.ui.screens.components.LoadScreen
import com.money.monocle.ui.theme.MoneyMonocleTheme
import kotlinx.coroutines.launch

typealias isExpense = Boolean
typealias Currency = String
@Composable
fun HomeScreen(
    onNavigateToAddRecord: (Currency, isExpense) -> Unit,
    onError: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val noDataAttachedToAccount = stringResource(id = R.string.no_data_attached_to_account)
    LaunchedEffect(uiState.accountState) {
        if (uiState.accountState == AccountState.DELETED) {
            onError(noDataAttachedToAccount)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.dataFetchResultFlow.collect {res ->
            if (res is Result.Error) {
                onError(res.error)
            }
        }
    }
    AnimatedVisibility(uiState.accountState == AccountState.NEW,
        enter = fadeIn(animationSpec = tween(400)),
        exit = fadeOut(animationSpec = tween(400))
    ) {
        val focusManger = LocalFocusManager.current
        var isSubmitEnabled by rememberSaveable {
            mutableStateOf(true)
        }
        LaunchedEffect(viewModel) {
            viewModel.welcomeScreenResultFlow.collect {res ->
                isSubmitEnabled = res !is Result.InProgress
                if (res is Result.Error) onError(res.error)
            }
        }
        WelcomeScreen(
            isSubmitEnabled = isSubmitEnabled,
            onBalance = {c, a ->
                viewModel.setBalance(c, a)
                focusManger.clearFocus(true)
            }
        )
    }
    AnimatedVisibility(uiState.accountState == AccountState.USED
            && viewModel.currentUser != null,
        enter = fadeIn(animationSpec = tween(600)),
        exit = fadeOut(animationSpec = tween(400)),

    ) {
        MainContent(viewModel,
            uiState,
            onNavigateToAddRecord = onNavigateToAddRecord,
            onError = onError)
    }
    if (uiState.dataFetchResult is Result.InProgress ||
        uiState.accountState == AccountState.NONE) {
        LoadScreen()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    viewModel: HomeViewModel,
    uiState: HomeViewModel.UiState,
    onNavigateToAddRecord: (Currency, isExpense) -> Unit,
    onError: (String) -> Unit,
) {
    val username = viewModel.currentUser!!.displayName
    val balanceState = uiState.balanceState
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember {
        mutableStateOf(false)
    }
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { scope.launch {
                showBottomSheet = true } }) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(25.dp)
        ) {
            Text(
                text = "${stringResource(id = R.string.hello)}, $username",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            CurrentBalanceBox(balance = balanceState.currentBalance,
                currencyOrdinal = balanceState.currency)
        }
        if (showBottomSheet) {
            AddRecordModalSheet(
                sheetState = sheetState,
                onDismiss = { showBottomSheet = false },
                onNavigateToAddRecord = {
                    scope.launch {
                        sheetState.hide()
                        onNavigateToAddRecord(simpleCurrencyMapper(balanceState.currency), it)
                    }
                })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordModalSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onNavigateToAddRecord: (Boolean) -> Unit,
) {
    ModalBottomSheet(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .width(600.dp),
        sheetState = sheetState,
        onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            AddRecordModalButton(text = stringResource(id = R.string.expense)) {
                onNavigateToAddRecord(true)
            }
            AddRecordModalButton(text = stringResource(id = R.string.income)) {
                onNavigateToAddRecord(false)
            }
        }
    }
}

@Composable
fun AddRecordModalButton(
    text: String,
    onClick: () -> Unit) {
    val shape = RoundedCornerShape(dimensionResource(id = R.dimen.button_corner))
    ElevatedButton(onClick = onClick,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(shape)) {
        Text(text,
            style = MaterialTheme.typography.labelSmall)
    }
}

//@Preview(name = "currentBalanceBox")
@Composable
fun CurrentBalanceBoxPreview() {
    MoneyMonocleTheme {
        CurrentBalanceBox(balance = 3288878787f, currencyOrdinal = 0)
    }
}

@Composable
fun CurrentBalanceBox(balance: Float,
                      currencyOrdinal: Int) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 10.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.onBackground,
            contentColor = MaterialTheme.colorScheme.background
        ),
        modifier = Modifier
            .size(250.dp, 130.dp)
            .shadow(
                elevation = 15.dp,
                spotColor = MaterialTheme.colorScheme.onBackground
            )
    ){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = stringResource(id = R.string.current_balance),
                style = MaterialTheme.typography.labelMedium)
            Text("$balance${simpleCurrencyMapper(currencyOrdinal)}",
                style = MaterialTheme.typography.titleLarge,
                overflow = TextOverflow.Ellipsis)
        }
    }
}
