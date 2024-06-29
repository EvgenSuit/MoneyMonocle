package com.money.monocle.ui.screens.record

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.money.monocle.LocalSnackbarController
import com.money.monocle.R
import com.money.monocle.data.Category
import com.money.monocle.data.CustomExpenseCategoriesIds
import com.money.monocle.data.CustomRawExpenseCategories
import com.money.monocle.data.CustomRawIncomeCategories
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.isInProgress
import com.money.monocle.ui.presentation.record.AddCategoryViewModel
import com.money.monocle.ui.screens.components.CommonButton
import com.money.monocle.ui.screens.components.CustomTopBar
import com.money.monocle.ui.theme.MoneyMonocleTheme

@Preview(device = "spec:id=reference_phone,shape=Normal,width=411,height=891,unit=dp,dpi=420")
@Composable
fun AddCategoryContentPreview() {
    MoneyMonocleTheme {
        Surface {
            AddCategoryContent(
                uiState = AddCategoryViewModel.UiState(
                    uploadResult = CustomResult.InProgress,
                    isExpense = true,
                    selectedCategory = Category(category = CustomExpenseCategoriesIds.DEBT.name)
                ),
                showCreateCategoryScreen = false,
                onShowCreateCategoryScreen = {},
                onCategorySelect = {},
                onNavigateBack = {},
                onCategoryAdd = {},
                onNameChange = {}
            )
        }
    }
}

@Composable
fun AddCategoryScreen(
    viewModel: AddCategoryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarController = LocalSnackbarController.current
    var showCreateCategoryScreen by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(uiState.uploadResult) {
        if (uiState.uploadResult is CustomResult.Success) onNavigateBack()
        snackbarController.showSnackbar(uiState.uploadResult)
    }
    AddCategoryContent(
        uiState = uiState,
        showCreateCategoryScreen = showCreateCategoryScreen,
        onShowCreateCategoryScreen = { showCreateCategoryScreen = it },
        onCategorySelect = viewModel::onCategoryChange,
        onNameChange = viewModel::onNameChange,
        onCategoryAdd = viewModel::onCategoryAdd,
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun AddCategoryContent(
    uiState: AddCategoryViewModel.UiState,
    showCreateCategoryScreen: Boolean,
    onShowCreateCategoryScreen: (Boolean) -> Unit,
    onCategorySelect: (Category) -> Unit,
    onNameChange: (String) -> Unit,
    onCategoryAdd: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val enabled = uiState.uploadResult !is CustomResult.InProgress
    val isExpense = uiState.isExpense
    val selectedCategory = uiState.selectedCategory
    LaunchedEffect(selectedCategory) {
        if (selectedCategory.res != null) {
            onShowCreateCategoryScreen(true)
        }
    }
    BackHandler(showCreateCategoryScreen) {
        onShowCreateCategoryScreen(false)
    }
    Scaffold(
        topBar = {
            CustomTopBar(text = stringResource(id = R.string.add) +
                    " ${if (isExpense) stringResource(id = R.string.expense) else stringResource(id = R.string.income)}" +
                    " ${stringResource(id = R.string.category)}",
                isInProgress = uiState.uploadResult.isInProgress(),
                onNavigateBack = {
                    if (showCreateCategoryScreen) onShowCreateCategoryScreen(false)
                    else onNavigateBack()
                })
        }
    ) {padding ->
        AnimatedVisibility(!showCreateCategoryScreen,
            exit = slideOutHorizontally { -it },
            enter = slideInHorizontally { -it },
            modifier = Modifier.padding(padding)) {
            LaunchedEffect(Unit) {
                onCategorySelect(Category())
            }
            SelectCategoryScreen(
                enabled = enabled,
                isExpense = isExpense,
                selectedCategory = selectedCategory,
                onCategorySelect = onCategorySelect
            )
        }
        AnimatedVisibility(showCreateCategoryScreen,
            exit = slideOutHorizontally { -it },
            enter = slideInHorizontally { -it },
            modifier = Modifier.padding(padding)) {
            CreateCategoryScreen(
                enabled = enabled,
                name = uiState.selectedCategory.name,
                onNameChange = onNameChange,
                onCategoryAdd = onCategoryAdd)
        }
    }
}

@Composable
fun CreateCategoryScreen(
    enabled: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    onCategoryAdd: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(50.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(text = stringResource(id = R.string.give_a_name_to_category),
            style = MaterialTheme.typography.displayMedium)
        OutlinedTextField(
            enabled = enabled,
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth().testTag("NameField"))
        CommonButton(
            enabled = enabled && name.isNotEmpty(),
            onClick = onCategoryAdd,
            text = stringResource(id = R.string.add))
    }
}

@Composable
fun SelectCategoryScreen(
    enabled: Boolean,
    isExpense: Boolean,
    selectedCategory: Category,
    onCategorySelect: (Category) -> Unit
) {
    val categories = (if (isExpense) CustomRawExpenseCategories.categories else CustomRawIncomeCategories.categories)
    Column(
        verticalArrangement = Arrangement.spacedBy(30.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        for (entry in categories.entries) {
            val text = stringResource(id = entry.key)
            Text(text = text,
                style = MaterialTheme.typography.labelMedium)
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = dimensionResource(id = R.dimen.grid_category_size)),
                modifier = Modifier
                    .heightIn(max = dimensionResource(id = R.dimen.max_categories_grid_height))
                    .testTag(text)) {
                items(entry.value.toList()) {rawCategory ->
                    val category = Category(
                        id = rawCategory.id,
                        category = rawCategory.categoryId,
                        res = rawCategory.res)
                    CategoryItem(
                        isExpense = isExpense,
                        enabled = enabled,
                        currentCategory = category,
                        selectedCategory = selectedCategory,
                        onCategoryChange = onCategorySelect,
                        modifier = Modifier.testTag(category.category))
                }
            }
        }
    }
}