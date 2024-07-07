package com.money.monocle.ui.screens.record

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.money.monocle.LocalSnackbarController
import com.money.monocle.R
import com.money.monocle.data.Category
import com.money.monocle.data.CustomRawExpenseCategories
import com.money.monocle.data.CustomRawIncomeCategories
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.isEmpty
import com.money.monocle.domain.isIdle
import com.money.monocle.domain.isInProgress
import com.money.monocle.domain.isSuccess
import com.money.monocle.ui.presentation.record.CategoryType
import com.money.monocle.ui.presentation.record.CustomCategoriesUiState
import com.money.monocle.ui.presentation.record.CustomCategoriesViewModel
import com.money.monocle.ui.presentation.record.IncomeCategoriesState
import com.money.monocle.ui.presentation.record.currentCategories
import com.money.monocle.ui.presentation.record.currentState
import com.money.monocle.ui.screens.components.AnimatedItem
import com.money.monocle.ui.screens.components.CategoryTextField
import com.money.monocle.ui.screens.components.CommonButton
import com.money.monocle.ui.screens.components.CustomTopBar
import com.money.monocle.ui.screens.components.InProgressLinearIndicator
import com.money.monocle.ui.screens.components.LOTTIE_SPEED
import com.money.monocle.ui.screens.components.NothingToShowText
import com.money.monocle.ui.screens.components.SuccessLottieAnimation
import com.money.monocle.ui.theme.MoneyMonocleTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomCategoriesScreen(
    viewModel: CustomCategoriesViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarController = LocalSnackbarController.current
    val sheetState = rememberModalBottomSheetState(
        // with skipPartiallyExpanded set to false the sheet will not be entirely shown
        // when keyboard appears
        skipPartiallyExpanded = true
    )
    val incomeListState = rememberLazyListState()
    val expenseListState = rememberLazyListState()
    val listState by remember(uiState.selectedType) {
        mutableStateOf(if (uiState.selectedType == CategoryType.INCOME) incomeListState
        else expenseListState)
    }
    var categoryToShow by remember {
        mutableStateOf(Category())
    }
    var showCategoryDetails by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(uiState.expenseCategoriesState.fetchResult) {
        snackbarController.showSnackbar(uiState.expenseCategoriesState.fetchResult)
    }
    LaunchedEffect(uiState.incomeCategoriesState.fetchResult) {
        snackbarController.showSnackbar(uiState.incomeCategoriesState.fetchResult)
    }
    LaunchedEffect(uiState.deletionResult) {
        snackbarController.showSnackbar(uiState.deletionResult)
    }
    LaunchedEffect(uiState.nameChangeResult) {
        categoryToShow = uiState.currentCategories().firstOrNull { it.id == categoryToShow.id } ?: categoryToShow
        snackbarController.showSnackbar(uiState.nameChangeResult)
    }
    // IMPORTANT - remember(listState), since listState changes upon selectedType change
    val lastVisibleCategoryIndex by remember(listState) {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
    }
    LaunchedEffect(listState, lastVisibleCategoryIndex) {
        viewModel.onCategoriesFetch(lastVisibleCategoryIndex)
    }
    CustomCategoriesContent(
        sheetState = sheetState,
        categoryToShow = categoryToShow,
        showCategoryDetails = showCategoryDetails,
        listState = listState,
        onTypeChange = viewModel::onTypeChange,
        onCategoryToShow = {categoryToShow = it},
        onShowCategoryDetails = {
            if (!it) {
                viewModel.updateDeletionResult(CustomResult.Idle)
                viewModel.updateNameChangeResult(CustomResult.Idle)
            }
            showCategoryDetails = it },
        onNameChange = viewModel::onCategoryNameChange,
        onCategoryDelete = viewModel::onCategoryDelete,
        uiState = uiState,
        onNavigateBack = onNavigateBack)
    DisposableEffect(Unit) {
        onDispose {
            viewModel.onDispose()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomCategoriesContent(
    sheetState: SheetState,
    categoryToShow: Category,
    showCategoryDetails: Boolean,
    listState: LazyListState,
    onTypeChange: () -> Unit,
    onCategoryToShow: (Category) -> Unit,
    onShowCategoryDetails: (Boolean) -> Unit,
    onNameChange: (Category) -> Unit,
    onCategoryDelete: (Category) -> Unit,
    uiState: CustomCategoriesUiState,
    onNavigateBack: () -> Unit
) {
    val selectedType = uiState.selectedType
    val expenseCategoriesState = uiState.expenseCategoriesState
    val incomeCategoriesState = uiState.incomeCategoriesState
    val currentCategories = if (selectedType == CategoryType.INCOME) incomeCategoriesState.categories
    else expenseCategoriesState.categories
    Scaffold(
        topBar = {
            CustomTopBar(text = stringResource(id = R.string.categories),
                isInProgress = if (selectedType == CategoryType.EXPENSE) expenseCategoriesState.fetchResult.isInProgress() else
            incomeCategoriesState.fetchResult.isInProgress(), onNavigateBack = onNavigateBack)
        }
    ) {padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedType.ordinal,
                divider = {
                    HorizontalDivider(color = Color.Transparent)
                }) {
                for (type in CategoryType.entries) {
                    Tab(selected = type == selectedType,
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onBackground,
                        text = {
                            Text(stringResource(id =
                            if (type == CategoryType.INCOME) R.string.income
                            else R.string.expense),
                                style = MaterialTheme.typography.labelSmall)
                        },
                        onClick = onTypeChange
                    )
                }
            }
            CategoriesColumn(
                enabled = !uiState.currentState().fetchResult.isInProgress(),
                fetchResult = uiState.currentState().fetchResult,
                selectedCategory = categoryToShow,
                categoryType = uiState.selectedType,
                listState = listState,
                categories = currentCategories,
                onDetailsShow = {
                    onCategoryToShow(it)
                    onShowCategoryDetails(true)
                },
            )
        }
    }
    if (showCategoryDetails) {
        CategoryDetails(
            category = categoryToShow,
            sheetState = sheetState,
            onNameChange = onNameChange,
            onDelete = onCategoryDelete,
            nameChangeResult = uiState.nameChangeResult,
            deletionResult = uiState.deletionResult,
            onDismiss = {
                onShowCategoryDetails(false)
                onCategoryToShow(Category())
            })
    }
}

@Composable
fun CategoriesColumn(
    enabled: Boolean,
    fetchResult: CustomResult,
    selectedCategory: Category,
    categoryType: CategoryType,
    listState: LazyListState,
    categories: List<Category>,
    onDetailsShow: (Category) -> Unit,
) {
    if (fetchResult.isEmpty()) {
        NothingToShowText()
    }
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(dimensionResource(id = R.dimen.items_list_padding)),
        modifier = Modifier
            .fillMaxSize()
            .testTag(categoryType.name)
    ) {
        items(categories, key = {it.id}) {category ->
            AnimatedItem {
                ExpandedCategoryItem(
                    enabled = enabled,
                    isExpense = categoryType == CategoryType.EXPENSE,
                    selectedCategory = selectedCategory,
                    category = category,
                    onDetailsShow = onDetailsShow
                )
            }
        }
    }
}

@Composable
fun ExpandedCategoryItem(
    isExpense: Boolean,
    enabled: Boolean,
    category: Category,
    selectedCategory: Category,
    onDetailsShow: (Category) -> Unit
) {
    val name = category.name
    val customRawCategories = (if (isExpense) CustomRawExpenseCategories.categories else CustomRawIncomeCategories.categories).values.flatten()
    val icon = painterResource(id = category.res ?:
    customRawCategories.firstOrNull { it.category == category.category }?.res ?: R.drawable.unknown)
    val isSelected = selectedCategory.id == category.id
    ElevatedCard(
        onClick = { onDetailsShow(category) },
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .semantics { selected = isSelected }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Text(name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.weight(1f))
            Image(painter = icon, contentDescription = category.id,
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.category_icon_size))
                    .weight(0.2f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetails(
    category: Category,
    sheetState: SheetState,
    nameChangeResult: CustomResult,
    deletionResult: CustomResult,
    onNameChange: (Category) -> Unit,
    onDelete: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.success))
    val progress = animateLottieCompositionAsState(composition,
        speed = LOTTIE_SPEED,
        isPlaying = deletionResult.isSuccess())
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier
            .testTag(stringResource(id = R.string.bottom_sheet))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .padding(bottom = dimensionResource(id = R.dimen.sheet_bottom_padding))
        ) {
            if (!deletionResult.isSuccess() && !deletionResult.isInProgress()) {
                CategoryDetailsMainContent(
                    nameChangeResult = nameChangeResult,
                    category = category,
                    onNameChange = onNameChange,
                    onDelete = onDelete
                )
            }
            if (deletionResult.isInProgress()) {
                InProgressLinearIndicator()
            }
            if (deletionResult.isSuccess()) {
                SuccessLottieAnimation(
                    composition = composition,
                    result = deletionResult,
                    progress = progress,
                    onDismiss = onDismiss)
            }
        }
    }
}

@Composable
fun CategoryDetailsMainContent(
    nameChangeResult: CustomResult,
    category: Category,
    onNameChange: (Category) -> Unit,
    onDelete: (Category) -> Unit
) {
    val enabled = !nameChangeResult.isInProgress()
    val maxNameLength = integerResource(id = R.integer.max_custom_category_name_length)
    var showChangeNameField by remember(nameChangeResult) {
        mutableStateOf(!nameChangeResult.isIdle() && !nameChangeResult.isSuccess())
    }
    var newName by remember {
        mutableStateOf("")
    }
    LaunchedEffect(nameChangeResult) {
        if (nameChangeResult.isSuccess()) newName = ""
    }
    TextButton(onClick = { showChangeNameField = !showChangeNameField },
        enabled = enabled) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val icon = if (!showChangeNameField) Icons.Filled.Edit else Icons.Filled.ArrowBack
            Icon(imageVector = icon, contentDescription = icon.name)
            Text(category.name,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.testTag(category.name))
        }
    }
    if(showChangeNameField) {
        ChangeCategoryNameColumn(name = newName,
            enabled = enabled,
            onNameChange = {if (it.length <= maxNameLength) newName = it},
            onSave = { onNameChange(category.copy(name = newName)) })
    }
    if (!showChangeNameField) {
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp
                )
        ) {
            IconButton(onClick = { onDelete(category) }) {
                val icon = Icons.Filled.Delete
                Icon(icon,
                    tint = MaterialTheme.colorScheme.error,
                    contentDescription = icon.name,
                    modifier = Modifier.size(dimensionResource(id = R.dimen.delete_icon_size)))
            }
        }
    }
}

@Composable
fun ChangeCategoryNameColumn(
    enabled: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val focusRequester = remember {
        FocusRequester()
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(20.dp)
    ) {
        CategoryTextField(value = name,
            enabled = enabled,
            onValueChange = onNameChange,
            modifier = Modifier.focusRequester(focusRequester))
        CommonButton(
            enabled = name.isNotBlank() && enabled,
            onClick = onSave,
            text = stringResource(id = R.string.ok))
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun CustomCategoriesScreenPreview() {
    val categories =CustomRawIncomeCategories.categories.values.flatten().map {
        Category(
            id = it.id, category = it.category, name = it.category.lowercase(),
        ) }
    MoneyMonocleTheme {
        Surface {
            CustomCategoriesContent(
                sheetState = rememberStandardBottomSheetState(),
                categoryToShow = categories[0],
                showCategoryDetails = true,
                onCategoryToShow = {},
                onShowCategoryDetails = {},
                listState = rememberLazyListState(),
                onTypeChange = {},
                onNameChange = {},
                onCategoryDelete = {},
                uiState = CustomCategoriesUiState(
                    selectedType = CategoryType.INCOME,
                    deletionResult = CustomResult.Idle,
                    incomeCategoriesState = IncomeCategoriesState(categories = categories
                    )
                )
            ) {

            }
        }
    }
}