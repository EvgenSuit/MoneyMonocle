package com.money.monocle.ui.screens.history

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.money.monocle.domain.isInProgress
import com.money.monocle.domain.useCases.DateFormatter
import com.money.monocle.ui.presentation.history.TransactionHistoryViewModel
import com.money.monocle.ui.screens.components.CustomTopBar
import com.money.monocle.ui.theme.MoneyMonocleTheme
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
    LaunchedEffect(uiState.fetchResult) {
        snackbarController.showSnackbar(uiState.fetchResult)
    }
    LaunchedEffect(uiState.deleteResult) {
        snackbarController.showSnackbar(uiState.deleteResult)
    }
    val sheetState = rememberModalBottomSheetState()
    var showDetailsSheet by remember {
        mutableStateOf(false)
    }
    var recordToShow by remember {
        mutableStateOf(Record())
    }
    var currentCategoryId by remember {
        mutableStateOf("")
    }
    Log.d("custom", currentCategoryId)
    Log.d("custom", uiState.customCategories.toString())
    val selectedCustomCategory by remember(currentCategoryId, uiState.customCategories) {
        mutableStateOf(uiState.customCategories.firstOrNull { it.id == currentCategoryId })
    }
    val listState = rememberLazyListState()
    val lastVisibleRecordIndex by remember(listState) {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
    }
    LaunchedEffect(uiState.deleteResult) {
        if (uiState.deleteResult is CustomResult.Success) {
            sheetState.hide()
            showDetailsSheet = false
            recordToShow = Record()
        }
    }
    LaunchedEffect(lastVisibleRecordIndex) {
        viewModel.fetchRecords(lastVisibleRecordIndex)
    }
    val state = TransactionHistoryContentState(
        listState = listState,
        selectedCustomCategory = selectedCustomCategory,
        customCategories = uiState.customCategories,
        fetchResult = uiState.fetchResult,
        sheetState = sheetState,
        currency = uiState.currency,
        records = uiState.records,
        recordToShow = recordToShow,
        showDetailsSheet = showDetailsSheet,
        onFormatDate = viewModel::formatDate,
        onDetails = {id, record, show ->
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
    val listState: LazyListState,
    val selectedCustomCategory: Category?,
    val customCategories: List<Category>,
    val fetchResult: CustomResult,
    val currency: String,
    val sheetState: SheetState,
    val records: List<Record>,
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
    Scaffold(
        topBar = {
            CustomTopBar(text = stringResource(id = R.string.transaction_history),
                isInProgress = state.fetchResult.isInProgress(), onNavigateBack = state.onBackClick)
        }
    ) {paddingValues ->
       RecordsColumn(
           listState = state.listState,
           selectedRecordId = state.recordToShow.id,
           currency = state.currency,
           records = state.records,
           onFormatDate = state.onFormatDate,
           onDetailsShow = {id, record ->
               state.onDetails(id, record, true)
           },
           modifier = Modifier.padding(paddingValues))
       if (state.fetchResult is CustomResult.Empty){
           Box(
               contentAlignment = Alignment.Center,
               modifier = Modifier.fillMaxSize()
           ) {
               Text(stringResource(id = R.string.nothing_to_show),
                   style = MaterialTheme.typography.displaySmall)
           }
       }
    }
    if (state.showDetailsSheet) {
        TransactionDetailSheet(
            customCategory = state.customCategories.firstOrNull { it.id == state.recordToShow.categoryId },
            currency = state.currency,
            sheetState = state.sheetState,
            onFormatDate = state.onFormatDate,
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(10.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("LazyColumn")
    ) {
        items(records, key = {it.id}) { record ->
            var isVisible by remember {
                mutableStateOf(false)
            }
            LaunchedEffect(Unit) {
                isVisible = true
            }
            AnimatedVisibility(isVisible, enter = fadeIn(
                tween(integerResource(id = R.integer.list_item_enter_duration))
            )) {
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
        .firstOrNull { it.categoryId == record.category }
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
            .size(400.dp, 70.dp)
            .testTag(record.timestamp.toString())
            .semantics {
                selected = isSelected
                contentDescription = record.timestamp.toString()
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .background(Color.Transparent)
        ) {
            Text("${record.amount}$currency",
                color = (if (!isSelected) color else MaterialTheme.colorScheme.background).copy(0.7f),
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.weight(1f))
            Image(painterResource(id = res),
                modifier = Modifier.size(50.dp),
                contentDescription = defaultCategory?.name ?: stringResource(id = customRawCategory?.name ?: R.string.unknown))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    customCategory: Category?,
    currency: String,
    record: Record,
    sheetState: SheetState,
    onFormatDate: (Long) -> String,
    onDismiss: () -> Unit,
    onDeleteClick: (String) -> Unit,
) {
    val color = if (record.expense) Color.Red else Color.Green
    val allCategories = LocalDefaultCategories.current
    val defaultCategories = if (record.expense) allCategories.first else allCategories.second
    val categoryName = customCategory?.name ?: defaultCategories.firstOrNull { it.category == record.category }?.name
    ?: stringResource(id = R.string.unknown)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier
            .width(dimensionResource(id = R.dimen.modal_sheet_width))
            .height(IntrinsicSize.Min)
            .testTag("DetailsSheet")) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp)
                .padding(bottom = dimensionResource(id = R.dimen.sheet_bottom_padding))
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("${record.amount}$currency",
                    color = color,
                    style = MaterialTheme.typography.displayMedium)
                Column {
                    Text(onFormatDate(record.date), style = MaterialTheme.typography.labelSmall)
                    Text(categoryName, style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = { onDeleteClick(record.id) },
                modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Delete,
                    tint = MaterialTheme.colorScheme.error,
                    contentDescription = "DeleteRecord",
                    modifier = Modifier.fillMaxSize())
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun TransactionHistoryPreview() {
    val defaultExpenseCategories = defaultRawExpenseCategories.map {
        Category(id = it.id, category = it.categoryId, name = stringResource(id = it.name!!), res = it.res)
    }
    val defaultIncomeCategories = defaultRawIncomeCategories.map {
        Category(id = it.id, category = it.categoryId, name = stringResource(id = it.name!!), res = it.res)
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
        customCategories = listOf(),
        selectedCustomCategory = Category(),
        listState = rememberLazyListState(),
        fetchResult = CustomResult.Success,
        currency = "$",
        sheetState = rememberStandardBottomSheetState(),
        records = records,
        recordToShow = Record(),
        showDetailsSheet = false,
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
