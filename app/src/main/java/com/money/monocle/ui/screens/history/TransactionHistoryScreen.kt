package com.money.monocle.ui.screens.history

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.money.monocle.LocalSnackbarController
import com.money.monocle.R
import com.money.monocle.data.Record
import com.money.monocle.data.expenseIcons
import com.money.monocle.data.incomeIcons
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.useCases.DateFormatter
import com.money.monocle.ui.presentation.history.TransactionHistoryViewModel
import com.money.monocle.ui.theme.MoneyMonocleTheme
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    currency: String,
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
    val listState = rememberLazyListState()
    val lastVisibleRecordIndex by remember(listState) {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
    }
    LaunchedEffect(Unit) {
        viewModel.deleteResultFlow.collect {res ->
            if (res is CustomResult.Success) {
                sheetState.hide()
                showDetailsSheet = false
                recordToShow = Record()
            }
        }
    }
    LaunchedEffect(lastVisibleRecordIndex) {
        viewModel.fetchRecords(lastVisibleRecordIndex)
    }
    TransactionHistoryContent(
        listState = listState,
        currency = currency,
        result = uiState.fetchResult,
        sheetState = sheetState,
        records = uiState.records,
        recordToShow = recordToShow,
        showDetailsSheet = showDetailsSheet,
        onFormatDate = viewModel::formatDate,
        onDetails = {record, show ->
            recordToShow = record
            showDetailsSheet = show
        },
        onDeleteClick = viewModel::deleteRecord,
        onBackClick = onBackClick)
    DisposableEffect(Unit) {
        onDispose {
            viewModel.onDispose()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryContent(
    listState: LazyListState,
    result: CustomResult,
    currency: String,
    sheetState: SheetState,
    records: List<Record>,
    recordToShow: Record,
    showDetailsSheet: Boolean,
    onFormatDate: (Long) -> String,
    onDetails: (Record, Boolean) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopBar(result = result, onBackClick = onBackClick)
        }
    ) {paddingValues ->
       RecordsColumn(
           listState = listState,
           selectedRecordTimestamp = recordToShow.timestamp,
           currency = currency,
           records = records,
           onFormatDate = onFormatDate,
           onDetailsShow = {
               onDetails(it, true)
           },
           modifier = Modifier.padding(paddingValues))
       if (result is CustomResult.Empty){
           Box(
               contentAlignment = Alignment.Center,
               modifier = Modifier.fillMaxSize()
           ) {
               Text(stringResource(id = R.string.nothing_to_show),
                   style = MaterialTheme.typography.displaySmall)
           }
       }
    }
    if (showDetailsSheet) {
        TransactionDetailSheet(
            currency = currency,
            sheetState = sheetState,
            onFormatDate = onFormatDate,
            record = recordToShow,
            onDismiss = {
                onDetails(Record(), false)
            },
            onDeleteClick = onDeleteClick)
    }
}

@Composable
fun RecordsColumn(
    listState: LazyListState,
    selectedRecordTimestamp: Long,
    currency: String,
    records: List<Record>,
    onFormatDate: (Long) -> String,
    onDetailsShow: (Record) -> Unit,
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
        items(records, key = {it.timestamp}
        ) { record ->
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(onFormatDate(record.date),
                    style = MaterialTheme.typography.labelSmall)
                RecordItem(
                    isSelected = record.timestamp == selectedRecordTimestamp,
                    currency = currency,
                    record = record,
                    onDetailsShow = onDetailsShow)
            }
        }

    }
}

@Composable
fun RecordItem(
    isSelected: Boolean,
    currency: String,
    record: Record,
    onDetailsShow: (Record) -> Unit) {
    val color = if (record.expense) Color.Red else Color.Green
    val containerColor by animateColorAsState(targetValue = if (!isSelected) color else MaterialTheme.colorScheme.onBackground)
    val category = if (record.expense) expenseIcons.entries.toList()[record.category]
    else incomeIcons.entries.toList()[record.category]
    val shape = RoundedCornerShape(dimensionResource(id = R.dimen.button_corner))
    ElevatedCard(onClick = {onDetailsShow(record)},
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color
            else MaterialTheme.colorScheme.background
        ),
        modifier = Modifier
            .shadow(
                dimensionResource(id = R.dimen.shadow_elevation),
                spotColor = containerColor,
                shape = shape
            )
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
                color = if (!isSelected) color else MaterialTheme.colorScheme.background,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.weight(1f))
            Icon(painterResource(id = category.value),
                tint = color,
                modifier = Modifier.size(50.dp),
                contentDescription = stringResource(id = category.key))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    currency: String,
    record: Record,
    sheetState: SheetState,
    onFormatDate: (Long) -> String,
    onDismiss: () -> Unit,
    onDeleteClick: (Long) -> Unit,
) {
    val color = if (record.expense) Color.Red else Color.Green
    val category = if (record.expense) expenseIcons.keys.toList()[record.category]
    else incomeIcons.keys.toList()[record.category]
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
                    Text(stringResource(id = category), style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = { onDeleteClick(record.timestamp) },
                modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Delete,
                    tint = MaterialTheme.colorScheme.error,
                    contentDescription = "DeleteRecord",
                    modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun TopBar(
    result: CustomResult,
    onBackClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "BackButton")
                }
                Text(text = stringResource(id = R.string.transaction_history),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f))
            }
            if (result is CustomResult.InProgress) {
                LinearProgressIndicator(modifier = Modifier
                    .width(150.dp)
                    .align(Alignment.BottomCenter))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun TransactionHistoryPreview() {
    val records = List(10) {
        val isExpense = it % 2 == 0
        Record(expense = isExpense,
            category = (if (isExpense) expenseIcons else incomeIcons).keys.indices.random(),
            timestamp = Instant.now().toEpochMilli() - it*15700000000,
            amount = (it+1 + it/2).toFloat())
    }
    val recordToShow = records[5]
    MoneyMonocleTheme {
        Surface {
            TransactionHistoryContent(
                listState = rememberLazyListState(),
                result = CustomResult.Success,
                currency = "$",
                sheetState = rememberStandardBottomSheetState(),
                records = records,
                recordToShow = recordToShow,
                showDetailsSheet = true,
                onFormatDate = {_ -> DateFormatter().invoke(recordToShow.timestamp)},
                onDetails = {_, _ ->},
                onDeleteClick = {}
            ) {
                
            }
        }
    }
}