package com.money.monocle.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.money.monocle.R
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.record.AddRecordRepository
import com.money.monocle.ui.presentation.record.AddRecordViewModel
import com.money.monocle.ui.presentation.CoroutineScopeProvider
import com.money.monocle.ui.screens.components.rememberImeState
import com.money.monocle.ui.theme.MoneyMonocleTheme
import java.text.SimpleDateFormat
import java.util.Locale


@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddRecordScreenPreview() {
    MoneyMonocleTheme {
        Surface(Modifier.background(MaterialTheme.colorScheme.background)) {
            AddRecordScreen({},
                currency = "$",
                false, AddRecordViewModel(AddRecordRepository(Firebase.auth, Firebase.firestore),
                CoroutineScopeProvider()
            )
            )
        }
    }
}

@Composable
fun AddRecordScreen(
    onNavigateBack: () -> Unit,
    currency: String,
    isExpense: Boolean,
    viewModel: AddRecordViewModel = hiltViewModel()
) {
    val recordState by viewModel.recordState.collectAsState()
    val selectedCategory = recordState.selectedCategory
    val selectedDate = recordState.selectedDate
    val amount = recordState.amount
    val result = recordState.uploadResult

    val scrollState = rememberScrollState()
    val imeState by rememberImeState()
    var showDatePicker by remember {
        mutableStateOf(false)
    }
    val dateFormatter = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    val stringDate by remember(selectedDate) {
        mutableStateOf(dateFormatter.format(selectedDate))
    }
    val isAmountOfCorrectFormat by remember(amount) {
        mutableStateOf(amount?.toFloatOrNull() != null && (amount.toFloatOrNull() ?: 0f) > 0f)
    }
    val enabled by remember(result) {
        mutableStateOf(result !is CustomResult.InProgress)
    }
    LaunchedEffect(imeState) {
        if (imeState) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
    LaunchedEffect(recordState.uploadResult) {
        if (recordState.uploadResult is CustomResult.Success) {
            onNavigateBack()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(50.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "BackButton")
            }
            Text(text = stringResource(id = R.string.add) +
                    " ${stringResource(id = if (isExpense) R.string.expense else R.string.income)}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f))
        }
        CategoriesGrid(
            enabled = enabled,
            isExpense = isExpense,
            selectedCategory = selectedCategory,
            onCategoryChange = viewModel::onCategoryChange)
        OpenDatePickerRow(
            stringDate = stringDate,
            enabled = enabled) {
            showDatePicker = true
        }
        AddRecordTextField(
            isError = imeState && !isAmountOfCorrectFormat,
            currency = currency,
            enabled = enabled && selectedCategory != -1,
            amount = amount,
            onAmountChange = viewModel::onAmountChange)
        AddRecordButton(enabled = enabled && isAmountOfCorrectFormat && selectedCategory != -1) {
            viewModel.addRecord(isExpense)
        }
    }
    AddRecordDatePicker(
        selectedDate = selectedDate,
        showDialog = showDatePicker,
        onShowDialogChange = { showDatePicker = it },
        onDateSelect = viewModel::onDateChange
    )
}

@Composable
fun OpenDatePickerRow(
    stringDate: String,
    enabled: Boolean,
    onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stringDate)
        Icon(painterResource(id = R.drawable.calendar),
            modifier = Modifier.size(50.dp),
            contentDescription = "Open date picker")
    }
}

@Composable
fun AddRecordTextField(
    isError: Boolean,
    currency: String,
    enabled: Boolean,
    amount: String?,
    onAmountChange: (String) -> Unit,
) {
    Column {
        OutlinedTextField(
            enabled = enabled,
            value = (amount ?: "").toString(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            onValueChange = onAmountChange,
            placeholder = { if (amount == null) Text("0.0") },
            suffix = { Text(currency) },
            isError = isError,
            modifier = Modifier.testTag("addRecordTextField"))
        if (isError) {
            Text(stringResource(id = R.string.invalid_format))
        }
    }
}

@Composable
fun AddRecordButton(
    enabled: Boolean,
    onClick: () -> Unit) {
    ElevatedButton(
        enabled = enabled,
        onClick = onClick,
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.button_corner)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("AddRecordButton")
    ) {
        Text(stringResource(id = R.string.add))
    }
}

@Composable
fun CategoriesGrid(
    enabled: Boolean,
    isExpense: Boolean,
    selectedCategory: Int,
    onCategoryChange: (Int) -> Unit) {
    val icons = if (isExpense) expenseIcons else incomeIcons
    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 90.dp),
        modifier = Modifier
            .heightIn(max = 1000.dp)
            .testTag("${if (isExpense) "Expense" else "Income"} grid")) {
        items(icons.size) { category ->
            CategoryItem(
                enabled = enabled,
                categoryIndex = category,
                currentCategory = icons.entries.toList()[category],
                selectedCategory = selectedCategory,
                onCategoryChange = onCategoryChange)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordDatePicker(
    selectedDate: Long,
    showDialog: Boolean,
    onShowDialogChange: (Boolean) -> Unit,
    onDateSelect: (Long) -> Unit) {
    if (showDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
        )
        DatePickerDialog(
            onDismissRequest = { onShowDialogChange(false) },
            confirmButton = { 
                TextButton(
                    onClick = {
                        onDateSelect(datePickerState.selectedDateMillis!!)
                        onShowDialogChange(false)
                }) {
                    Text(text = stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowDialogChange(false) }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }) {
            DatePicker(state = datePickerState)
        }
    }
}
@Composable
fun CategoryItem(
    enabled: Boolean,
    categoryIndex: Int,
    currentCategory: Map.Entry<Int, Int>,
    selectedCategory: Int,
    onCategoryChange: (Int) -> Unit,
) {
    val name = stringResource(id = currentCategory.key)
    val icon = painterResource(id = currentCategory.value)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            enabled = enabled,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (selectedCategory == categoryIndex) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.background,
                contentColor = if (selectedCategory == categoryIndex) MaterialTheme.colorScheme.background
                else MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier.size(60.dp),
            onClick = {
                onCategoryChange(if (selectedCategory == categoryIndex) -1 else categoryIndex)
            }) {
            Icon(painter = icon,
                contentDescription = name)
        }
        Text(text = name,
            textAlign = TextAlign.Center
        )
    }
}

val expenseIcons = mapOf(
    R.string.entertainment to R.drawable.entertainment,
    R.string.groceries to R.drawable.groceries,
    R.string.insurance to R.drawable.insurance,
    R.string.transportation to R.drawable.transportation,
    R.string.utilities to R.drawable.utilities
)

val incomeIcons = mapOf(
    R.string.wage to R.drawable.wage,
    R.string.business to R.drawable.business,
    R.string.interest to R.drawable.interest,
    R.string.investment to R.drawable.investment,
    R.string.gift to R.drawable.gift,
    R.string.government_payment to R.drawable.government
)

