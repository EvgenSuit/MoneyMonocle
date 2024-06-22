package com.money.monocle.ui.screens.home

import androidx.compose.foundation.Image
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.money.monocle.LocalSnackbarController
import com.money.monocle.R
import com.money.monocle.data.expenseIcons
import com.money.monocle.data.incomeIcons
import com.money.monocle.domain.CustomResult
import com.money.monocle.ui.presentation.record.AddRecordViewModel
import com.money.monocle.ui.screens.components.CommonButton
import com.money.monocle.ui.screens.components.rememberImeState
import com.money.monocle.ui.theme.MoneyMonocleTheme
import java.text.SimpleDateFormat
import java.util.Locale

val LocalAddRecordScreenState = compositionLocalOf<AddRecordScreenContentState> {
    error("No add record screen state provided")
}

@Preview
@Composable
fun AddRecordScreenPreview() {
    val state = AddRecordScreenContentState(
        recordState = AddRecordViewModel.RecordState(
            selectedCategory = 3
        ),
    )
    MoneyMonocleTheme {
        Surface(Modifier.background(MaterialTheme.colorScheme.background)) {
            CompositionLocalProvider(LocalAddRecordScreenState provides state) {
                AddRecordScreenContent()
            }
        }
    }
}

@Composable
fun AddRecordScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddRecordViewModel = hiltViewModel()
) {
    val recordState by viewModel.recordState.collectAsState()
    val snackbarController = LocalSnackbarController.current
    LaunchedEffect(recordState.uploadResult) {
        snackbarController.showSnackbar(recordState.uploadResult)
        if (recordState.uploadResult is CustomResult.Success) {
            onNavigateBack()
        }
    }
    val state = AddRecordScreenContentState(
        recordState = recordState,
        onNavigateBack = onNavigateBack,
        onCategoryChange = viewModel::onCategoryChange,
        onAmountChange = viewModel::onAmountChange,
        onAddRecord = viewModel::addRecord,
        onDateChange = viewModel::onDateChange
    )
    CompositionLocalProvider(LocalAddRecordScreenState provides state) {
        AddRecordScreenContent()
    }
}


@Composable
fun AddRecordScreenContent() {
    val imeState by rememberImeState()
    val scrollState = rememberScrollState()
    val state = LocalAddRecordScreenState.current
    val recordState = state.recordState
    val selectedCategory = recordState.selectedCategory
    val selectedDate = recordState.selectedDate
    val amount = recordState.amount
    val result = recordState.uploadResult
    var showDatePicker by remember {
        mutableStateOf(false)
    }
    val dateFormatter = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    val stringDate by remember(selectedDate) {
        mutableStateOf(dateFormatter.format(selectedDate))
    }
    val enabled by remember(result) {
        mutableStateOf(result !is CustomResult.InProgress)
    }
    LaunchedEffect(imeState) {
        if (imeState) {
            scrollState.animateScrollTo(scrollState.maxValue)
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
            IconButton(onClick = state.onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "BackButton")
            }
            Text(text = stringResource(id = R.string.add) +
                    " ${stringResource(id = if (recordState.isExpense) R.string.expense else R.string.income)}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f))
        }
        CategoriesGrid(
            enabled = enabled,
            isExpense = recordState.isExpense,
            selectedCategory = selectedCategory,
            onCategoryChange = state.onCategoryChange)
        OpenDatePickerRow(
            stringDate = stringDate,
            enabled = enabled) {
            showDatePicker = true
        }
        AddRecordTextField(
            currency = recordState.currency,
            enabled = enabled && selectedCategory != -1,
            amount = amount,
            onAmountChange = state.onAmountChange)
        CommonButton(
            enabled = enabled && !amount.isNullOrBlank() && selectedCategory != -1,
            onClick = state.onAddRecord, text = stringResource(id = R.string.add))
    }
    AddRecordDatePicker(
        selectedDate = selectedDate,
        showDialog = showDatePicker,
        onShowDialogChange = { showDatePicker = it },
        onDateChange = state.onDateChange
    )
}
@Composable
fun OpenDatePickerRow(
    stringDate: String,
    enabled: Boolean,
    onClick: () -> Unit) {
    val shape = RoundedCornerShape(dimensionResource(id = R.dimen.common_corner))
    ElevatedButton(
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(),
        onClick = onClick,
        modifier = Modifier.clip(shape)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringDate)
            Image(painterResource(id = R.drawable.calendar),
                modifier = Modifier.size(50.dp),
                contentDescription = "Open date picker")
        }
    }
}

@Composable
fun AddRecordTextField(
    currency: String,
    enabled: Boolean,
    amount: String?,
    onAmountChange: (String) -> Unit,
) {
    OutlinedTextField(
        enabled = enabled,
        value = (amount ?: "").toString(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        onValueChange = onAmountChange,
        placeholder = { if (amount == null) Text("0.0") },
        suffix = { Text(currency) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("addRecordTextField"))
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
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.common_corner)))
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
    onDateChange: (Long) -> Unit) {
    if (showDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
        )
        DatePickerDialog(
            onDismissRequest = { onShowDialogChange(false) },
            confirmButton = { 
                TextButton(
                    onClick = {
                        onDateChange(datePickerState.selectedDateMillis!!)
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
    val isSelected = selectedCategory == categoryIndex
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            ),
            onClick = { onCategoryChange(if (isSelected) -1 else categoryIndex) },
            ) {
            Image(icon,
                contentDescription = name,
                modifier = Modifier.size(dimensionResource(id = R.dimen.category_icon_size)))
        }
        Text(text = name,
            textAlign = TextAlign.Center
        )
    }
}
data class AddRecordScreenContentState(
    val recordState: AddRecordViewModel.RecordState,
    val onNavigateBack: () -> Unit = {},
    val onCategoryChange: (Int) -> Unit = {},
    val onAmountChange: (String) -> Unit = {},
    val onAddRecord: () -> Unit = {},
    val onDateChange: (Long) -> Unit = {}
)

