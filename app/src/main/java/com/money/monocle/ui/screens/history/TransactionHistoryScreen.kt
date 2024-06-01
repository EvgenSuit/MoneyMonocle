package com.money.monocle.ui.screens.history

import android.util.Log
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.money.monocle.R
import com.money.monocle.data.Record
import com.money.monocle.domain.Result
import com.money.monocle.ui.presentation.history.TransactionHistoryViewModel
import com.money.monocle.ui.screens.home.expenseIcons
import com.money.monocle.ui.screens.home.incomeIcons
import com.money.monocle.ui.theme.MoneyMonocleTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    onError: (String) -> Unit,
    currency: String,
    onBackClick: () -> Unit,
    viewModel: TransactionHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
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
    LaunchedEffect(viewModel) {
        viewModel.resultFlow.collect {res ->
            if (res is Result.Error) {
                onError(res.error)
            }
        }
    }
    LaunchedEffect(lastVisibleRecordIndex) {
        viewModel.fetchRecords(lastVisibleRecordIndex)
    }
    TransactionHistoryContent(
        listState = listState,
        currency = currency,
        result = uiState.result,
        sheetState = sheetState,
        records = uiState.records,
        recordToShow = recordToShow,
        showDetailsSheet = showDetailsSheet,
        onFormatDate = viewModel::formatDate,
        onDetails = {record, show ->
            recordToShow = record
            showDetailsSheet = show
        },
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
    result: Result,
    currency: String,
    sheetState: SheetState,
    records: List<Record>,
    recordToShow: Record,
    showDetailsSheet: Boolean,
    onFormatDate: (Long) -> String,
    onDetails: (Record, Boolean) -> Unit,
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopBar(
                result = result,
                onBackClick = onBackClick)
        }
    ) {paddingValues ->
       RecordsColumn(
           listState = listState,
           selectedRecordId = recordToShow.id,
           currency = currency,
           records = records,
           onFormatDate = onFormatDate,
           onDetailsShow = {
               onDetails(it, true)
           },
           modifier = Modifier.padding(paddingValues))
    }
    if (showDetailsSheet) {
        TransactionDetailSheet(
            currency = currency,
            sheetState = sheetState,
            onFormatDate = onFormatDate,
            record = recordToShow,
            onDismiss = {
                onDetails(Record(), false)
            })
    }
}

@Composable
fun RecordsColumn(
    listState: LazyListState,
    selectedRecordId: String,
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
        items(records, key = {it.id}
        ) { record ->
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
            .testTag(record.id)
            .semantics {
                selected = isSelected
                contentDescription = record.id
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
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 20.dp, start = 20.dp)
        ) {
            Text("${record.amount}$currency",
                color = color,
                style = MaterialTheme.typography.displayMedium)
            Column {
                Text(onFormatDate(record.date), style = MaterialTheme.typography.labelSmall)
                Text(stringResource(id = category), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun TopBar(
    result: Result,
    onBackClick: () -> Unit
) {
    //Log.d("records", result.toString())
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
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f))
            }
            if (result is Result.InProgress) {
                LinearProgressIndicator(modifier = Modifier.width(150.dp).align(Alignment.BottomCenter))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun TransactionHistoryPreview() {
    MoneyMonocleTheme {
        Surface {
            TransactionHistoryContent(
                listState = rememberLazyListState(),
                result = Result.Success(""),
                currency = "$",
                sheetState = rememberModalBottomSheetState(),
                records = listOf(Record(id = "df",
                    amount = 25.5f),
                    Record(id = "dd", amount = 5.5f, expense = true),
                    Record(amount = 25.5f),
                    Record(amount = 678.5f),
                    Record(amount = 45.5f)),
                recordToShow = Record(id = "df",
                    amount = 25.5f),
                showDetailsSheet = false,
                onFormatDate = {_ -> "12:45"},
                onDetails = {_, _ ->}
            ) {
                
            }
        }
    }
}