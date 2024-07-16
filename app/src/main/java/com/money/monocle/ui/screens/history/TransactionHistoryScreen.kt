package com.money.monocle.ui.screens.history

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.money.monocle.LocalDefaultCategories
import com.money.monocle.LocalSnackbarController
import com.money.monocle.R
import com.money.monocle.data.Category
import com.money.monocle.data.CustomRawExpenseCategories
import com.money.monocle.data.CustomRawIncomeCategories
import com.money.monocle.data.Record
import com.money.monocle.data.defaultRawExpenseCategories
import com.money.monocle.data.defaultRawIncomeCategories
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.isEmpty
import com.money.monocle.domain.isInProgress
import com.money.monocle.domain.isSuccess
import com.money.monocle.domain.useCases.DateFormatter
import com.money.monocle.ui.presentation.history.TransactionHistoryViewModel
import com.money.monocle.ui.screens.components.AnimatedItem
import com.money.monocle.ui.screens.components.CustomTopBar
import com.money.monocle.ui.screens.components.DeleteIconButton
import com.money.monocle.ui.screens.components.InProgressLinearIndicator
import com.money.monocle.ui.screens.components.LOTTIE_SPEED
import com.money.monocle.ui.screens.components.NothingToShowText
import com.money.monocle.ui.screens.components.SuccessLottieAnimation
import com.money.monocle.ui.theme.MoneyMonocleTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

val LocalTransactionHistoryState = compositionLocalOf<TransactionHistoryContentState> {
    error("No transaction history content provided")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    onBackClick: () -> Unit,
    viewModel: TransactionHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarController = LocalSnackbarController.current
    val scope = rememberCoroutineScope()
    var showDetailsSheet by remember {
        mutableStateOf(false)
    }
    var recordToShow by remember {
        mutableStateOf(Record())
    }
    LaunchedEffect(uiState.fetchResult) {
        snackbarController.showSnackbar(uiState.fetchResult)
    }
    LaunchedEffect(uiState.deleteResult) {
        snackbarController.showSnackbar(uiState.deleteResult)
    }
    val sheetState = rememberModalBottomSheetState()
    var currentCategoryId by remember {
        mutableStateOf("")
    }
    val selectedCustomCategory by remember(currentCategoryId, uiState.customCategories) {
        mutableStateOf(uiState.customCategories.firstOrNull { it.id == currentCategoryId })
    }

    val listState = rememberLazyListState()
    val lastVisibleRecordIndex by remember(listState) {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
    }
    LaunchedEffect(lastVisibleRecordIndex) {
        if (lastVisibleRecordIndex != 0) {
            viewModel.fetchRecords(lastVisibleRecordIndex)
        }
    }
    val state = TransactionHistoryContentState(
        uiState = uiState,
        listState = listState,
        selectedCustomCategory = selectedCustomCategory,
        sheetState = sheetState,
        recordToShow = recordToShow,
        showDetailsSheet = showDetailsSheet,
        onFormatDate = viewModel::formatDate,
        onDetails = {id, record, show ->
            if (!show) {
                scope.launch {
                    viewModel.updateDeleteResult(CustomResult.Idle)
                    sheetState.hide()
                }
            }
            currentCategoryId = id
            recordToShow = record
            showDetailsSheet = show
        },
        onDeleteClick = viewModel::deleteRecord,
        onBackClick = onBackClick
    )
    CompositionLocalProvider(LocalTransactionHistoryState provides state) {
        TransactionHistoryContent()
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.onDispose()
        }
    }
}

data class TransactionHistoryContentState @OptIn(ExperimentalMaterial3Api::class) constructor(
    val uiState: TransactionHistoryViewModel.UiState,
    val listState: LazyListState,
    val selectedCustomCategory: Category?,
    val sheetState: SheetState,
    val recordToShow: Record,
    val showDetailsSheet: Boolean,
    val onFormatDate: (Long) -> String,
    val onDetails: (String, Record, Boolean) -> Unit,
    val onDeleteClick: (String) -> Unit,
    val onBackClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryContent() {
    val state = LocalTransactionHistoryState.current
    val uiState = state.uiState
    Scaffold(
        topBar = {
            CustomTopBar(textId = R.string.transaction_history,
                result = uiState.fetchResult, onNavigateBack = state.onBackClick)
        }
    ) {paddingValues ->
       RecordsColumn(
           listState = state.listState,
           selectedRecordId = state.recordToShow.id,
           currency = uiState.currency,
           records = uiState.records,
           onFormatDate = state.onFormatDate,
           onDetailsShow = {id, record ->
               state.onDetails(id, record, true)
           },
           modifier = Modifier.padding(paddingValues))
       if (uiState.fetchResult.isEmpty()){
           NothingToShowText()
       }
    }
    if (state.showDetailsSheet) {
        TransactionDetailSheet(
            customCategory = uiState.customCategories.firstOrNull { it.id == state.recordToShow.categoryId },
            currency = uiState.currency,
            sheetState = state.sheetState,
            onFormatDate = state.onFormatDate,
            deletionResult = uiState.deleteResult,
            record = state.recordToShow,
            onDismiss = {
                state.onDetails("", Record(), false)
            },
            onDeleteClick = state.onDeleteClick)
    }
}

@Composable
fun RecordsColumn(
    listState: LazyListState,
    selectedRecordId: String,
    currency: String,
    records: List<Record>,
    onFormatDate: (Long) -> String,
    onDetailsShow: (String, Record) -> Unit,
    modifier: Modifier) {
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.items_list_spacing)),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(dimensionResource(id = R.dimen.items_list_padding)),
        modifier = modifier
            .fillMaxSize()
            .testTag("LazyColumn")
    ) {
        items(records, key = {it.id}) { record ->
            AnimatedItem {
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(onFormatDate(record.date),
                        style = MaterialTheme.typography.labelSmall)
                    RecordItem(
                        isSelected = record.id == selectedRecordId,
                        currency = currency,
                        record = record,
                        onDetailsShow = onDetailsShow)
                }
            }
        }
    }
}

@Composable
fun RecordItem(
    isSelected: Boolean,
    currency: String,
    record: Record,
    onDetailsShow: (String, Record) -> Unit) {
    val allCategories = LocalDefaultCategories.current
    val customRawCategory = (if (record.expense) CustomRawExpenseCategories.categories else CustomRawIncomeCategories.categories).values.flatten()
        .firstOrNull { it.category == record.category }
    val defaultCategory = (if (record.expense) allCategories.first else allCategories.second).firstOrNull { it.category == record.category }
    val res = defaultCategory?.res ?: customRawCategory?.res ?: R.drawable.unknown

    val color = if (record.expense) Color.Red else Color.Green
    val shape = RoundedCornerShape(dimensionResource(id = R.dimen.button_corner))
    ElevatedCard(onClick = {
        if (defaultCategory != null) onDetailsShow(defaultCategory.id, record)
        else if (customRawCategory != null) onDetailsShow(customRawCategory.id, record) },
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color
            else MaterialTheme.colorScheme.background
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .semantics {
                selected = isSelected
                contentDescription = record.id
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.list_items_padding))
                .background(Color.Transparent)
        ) {
            Text("${record.amount}$currency",
                color = (if (!isSelected) color else MaterialTheme.colorScheme.background).copy(0.7f),
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.weight(1f))
            Image(painterResource(id = res),
                modifier = Modifier
                    .size(50.dp)
                    .weight(0.2f),
                contentDescription = defaultCategory?.name ?: stringResource(id = customRawCategory?.name ?: R.string.unknown))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    customCategory: Category?,
    currency: String,
    deletionResult: CustomResult,
    record: Record,
    sheetState: SheetState,
    onFormatDate: (Long) -> String,
    onDismiss: () -> Unit,
    onDeleteClick: (String) -> Unit,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.success))
    val progress = animateLottieCompositionAsState(composition,
        speed = LOTTIE_SPEED,
        isPlaying = deletionResult.isSuccess())
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier
            .testTag("DetailsSheet")) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .padding(bottom = dimensionResource(id = R.dimen.sheet_bottom_padding))
        ) {
            if (deletionResult.isInProgress()) {
                InProgressLinearIndicator()
            }
            if (!deletionResult.isSuccess() && !deletionResult.isInProgress()) {
                //Log.d("deletion", deletionResult.toString())
                DetailsMainContent(record = record,
                    currency = currency,
                    customCategory = customCategory,
                    onFormatDate = onFormatDate,
                    onDeleteClick = onDeleteClick)
            }
            if (deletionResult.isSuccess()) {
                SuccessLottieAnimation(
                    composition = composition,
                    progress = progress,
                    result = deletionResult,
                    onDismiss = onDismiss)
            }
        }
    }
}

@Composable
fun DetailsMainContent(
    record: Record,
    currency: String,
    customCategory: Category?,
    onFormatDate: (Long) -> String,
    onDeleteClick: (String) -> Unit,
) {
    val color = if (record.expense) Color.Red else Color.Green
    val allCategories = LocalDefaultCategories.current
    val defaultCategories = if (record.expense) allCategories.first else allCategories.second
    val categoryName = customCategory?.name ?: defaultCategories.firstOrNull { it.category == record.category }?.name
    ?: stringResource(id = R.string.unknown)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text("${record.amount}$currency",
                color = color,
                style = MaterialTheme.typography.displayMedium)
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(onFormatDate(record.date), style = MaterialTheme.typography.labelSmall)
                Text(categoryName, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Start)
            }
        }
        DeleteIconButton(modifier = Modifier.weight(0.2f)) {
            onDeleteClick(record.id)
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun TransactionHistoryPreview() {
    val defaultExpenseCategories = defaultRawExpenseCategories.map {
        Category(id = it.id, category = it.category, name = stringResource(id = it.name!!), res = it.res)
    }
    val defaultIncomeCategories = defaultRawIncomeCategories.map {
        Category(id = it.id, category = it.category, name = stringResource(id = it.name!!), res = it.res)
    }
    val defaultCategories = Pair(defaultExpenseCategories, defaultIncomeCategories)
    val records = List(6) {
        val isExpense = it % 2 == 0
        Record(
            id = UUID.randomUUID().toString(),
            expense = isExpense,
            category = if (isExpense) defaultExpenseCategories[it].category else defaultIncomeCategories[it].category,
            timestamp = Instant.now().toEpochMilli() - it*15700000000,
            amount = (it+1 + it/2).toFloat())
    }
    val recordToShow = records[2]
    val state = TransactionHistoryContentState(
        uiState = TransactionHistoryViewModel.UiState(
            currency = "$",
            records = records,
            fetchResult = CustomResult.Success
        ),
        selectedCustomCategory = Category(),
        listState = rememberLazyListState(),
        sheetState = rememberStandardBottomSheetState(),
        recordToShow = recordToShow,
        showDetailsSheet = true,
        onFormatDate = { _ -> DateFormatter().invoke(recordToShow.timestamp) },
        onDetails = { _, _, _ -> },
        onDeleteClick = {},
        onBackClick = {}
    )
    MoneyMonocleTheme {
        Surface {
            CompositionLocalProvider(LocalDefaultCategories provides defaultCategories) {
                CompositionLocalProvider(LocalTransactionHistoryState provides state) {
                    TransactionHistoryContent()
                }
            }
        }
    }
}
