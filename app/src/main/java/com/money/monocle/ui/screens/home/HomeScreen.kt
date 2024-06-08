package com.money.monocle.ui.screens.home

import android.content.res.Resources
import android.graphics.fonts.FontFamily
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.money.monocle.R
import com.money.monocle.data.simpleCurrencyMapper
import com.money.monocle.domain.Result
import com.money.monocle.domain.home.AccountState
import com.money.monocle.domain.home.TotalEarned
import com.money.monocle.domain.home.TotalSpent
import com.money.monocle.ui.presentation.home.HomeViewModel
import com.money.monocle.ui.screens.components.LoadScreen
import com.money.monocle.ui.theme.AppTypography
import com.money.monocle.ui.theme.MoneyMonocleTheme
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

typealias isExpense = Boolean
typealias Currency = String
@Composable
fun HomeScreen(
    onNavigateToAddRecord: (Currency, isExpense) -> Unit,
    onNavigateToHistory: (Currency) -> Unit,
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
    if (uiState.accountState == AccountState.USED
        && viewModel.currentUser != null) {
        MainContent(balanceState = uiState.balanceState,
            pieChartState = uiState.pieChartState,
            displayName = viewModel.currentUser.displayName!!,
            onNavigateToAddRecord = onNavigateToAddRecord,
            onNavigateToHistory = onNavigateToHistory)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    displayName: String,
    balanceState: HomeViewModel.BalanceState,
    pieChartState: HomeViewModel.PieChartState,
    onNavigateToAddRecord: (Currency, isExpense) -> Unit,
    onNavigateToHistory: (Currency) -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember {
        mutableStateOf(false)
    }
    val currencyString = simpleCurrencyMapper(balanceState.currency)
    val totalSpent = pieChartState.totalSpent
    val totalEarned = pieChartState.totalEarned
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
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(45.dp)
        ) {
            Text(
                text = "${stringResource(id = R.string.hello)}, $displayName",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            CurrentBalanceBox(balance = balanceState.currentBalance,
                currencyOrdinal = balanceState.currency,
                onClick = {onNavigateToHistory(currencyString)})
            AnimatedVisibility (totalSpent != null && totalEarned != null,
                enter = fadeIn()) {
                if (totalSpent != null && totalEarned != null) {
                    PieChart(
                        currency = currencyString,
                        totalEarned = totalEarned,
                        totalSpent = totalSpent
                    )
                }
            }
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
            .width(dimensionResource(id = R.dimen.modal_sheet_width)),
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


@Composable
fun CurrentBalanceBox(balance: Float,
                      currencyOrdinal: Int,
                      onClick: () -> Unit) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 10.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.onBackground,
            contentColor = MaterialTheme.colorScheme.background
        ),
        modifier = Modifier
            .size(300.dp, 150.dp)
            .shadow(
                elevation = dimensionResource(id = R.dimen.shadow_elevation),
                spotColor = MaterialTheme.colorScheme.onBackground
            )
            .clickable { onClick() }
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

@Composable
fun PieChart(
    currency: String,
    totalEarned: TotalEarned,
             totalSpent: TotalSpent, ) {
    val data = listOf(
        PieChartData(totalEarned, stringResource(id = R.string.earned), MaterialTheme.colorScheme.inversePrimary),
        PieChartData(totalSpent, stringResource(id = R.string.spent), MaterialTheme.colorScheme.error)
    )
    var animationPlayed by remember {
        mutableStateOf(false)
    }
    val total = totalSpent + totalEarned
    val stroke = BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground)
    val animateRotation by animateFloatAsState(
        targetValue = if (animationPlayed) 90f * 5f else 0f,
        animationSpec = tween(
            durationMillis = 500,
            delayMillis = 0,
            easing = LinearOutSlowInEasing
        )
    )
    LaunchedEffect(true) {
        animationPlayed = true
    }
    var lastValue = 0f
    val chartSize = dimensionResource(id = R.dimen.pie_chart_size)
    Column(
        modifier = Modifier
            .width(chartSize)
            .testTag("PieChart"),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text(
            stringResource(id = R.string.pie_chart_title),
            style = MaterialTheme.typography.displaySmall)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier
                    .size(chartSize)
                    .clip(CircleShape)
                    .rotate(animateRotation)) {
                    if (totalSpent != 0f && totalEarned != 0f) {
                        for (slice in data) {
                            val value = slice.value * 360 / total
                            drawArc(
                                color = slice.color,
                                lastValue,
                                value,
                                useCenter = false,
                                style = Stroke(
                                    (chartSize.value / 4).dp.toPx(),
                                    cap = StrokeCap.Butt
                                )
                            )
                            lastValue += value
                        }
                    }
                        drawCircle(
                            brush = stroke.brush,
                            center = Offset(size.width / 2, size.height / 2),
                            radius = (size.width / 2) - (stroke.width / 2).value,
                            style = Stroke(width = stroke.width.toPx())
                        )

                }
                if (totalSpent == 0f && totalEarned == 0f) {
                    Text(
                        stringResource(id = R.string.nothing_to_show),
                        style = MaterialTheme.typography.labelSmall)
                }
            }
            PieChartDetails(currency = currency, data = data)
    }
}

@Composable
fun PieChartDetails(currency: String, data: List<PieChartData>) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        for (slice in data) {
            PieChartDetailItem(currency = currency, data = slice)
        }
    }
}
@Composable
fun PieChartDetailItem(currency: String, data: PieChartData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.button_corner)))
            .background(data.color))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(data.label, style = MaterialTheme.typography.labelMedium)
            Text("${data.value}$currency", style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.testTag("${data.label}: ${data.value}$currency"))
        }
    }
}
data class PieChartData(val value: Float, val label: String, val color: Color)

@Preview
@Composable
fun PieChartPreview() {
    MoneyMonocleTheme {
        Surface {
            PieChart(currency = "$", totalEarned = 0f, totalSpent = 0f)
        }
    }
}
//@Preview
@Composable
fun HomeScreenPreview() {
    MoneyMonocleTheme(darkTheme = true) {
        Surface {
            MainContent(displayName = "Yauheni Mokich",
                balanceState = HomeViewModel.BalanceState(),
                pieChartState = HomeViewModel.PieChartState(totalSpent = 0f, totalEarned = 0f),
                onNavigateToAddRecord = { _, _ -> },
                onNavigateToHistory = {})
        }
    }
}