package com.money.monocle.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.money.monocle.R
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.domain.Result
import com.money.monocle.ui.presentation.settings.SettingsViewModel
import com.money.monocle.ui.screens.components.CurrencyButton
import com.money.monocle.ui.theme.MoneyMonocleTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

private typealias isThemeDark = Boolean
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onError: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isThemeDark = uiState.isThemeDark
    val balance = uiState.balance
    LaunchedEffect(Unit) {
        viewModel.currencyChangeResultFlow.collect { result ->
            if (result is Result.Error) onError(result.error)
        }
    }
    AnimatedVisibility (isThemeDark != null && balance.currency != -1,
        enter = fadeIn()
    ) {
        SettingsScreenContent(
            lastTimeCurrencyUpdatedResult = uiState.lastTimeCurrencyUpdatedResult,
            lastTimeCurrencyUpdated = uiState.lastTimeCurrencyUpdated,
            currencyChangeResult = uiState.currencyChangeResult,
            currency = CurrencyEnum.entries[balance.currency],
            isThemeDark = isThemeDark!!,
            onThemeChange = viewModel::changeThemeMode,
            onNewCurrency = viewModel::changeCurrency,
            onCurrencyChangeResult = viewModel::updateCurrencyChangeResult,
            onCurrencyChangeTap = viewModel::checkLastTimeUpdated,
            onCurrencyInfoDismiss = viewModel::changeLastTimeUpdated,
            onSignOut = viewModel::signOut,
            onError = onError)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    lastTimeCurrencyUpdatedResult: Result,
    lastTimeCurrencyUpdated: Long?,
    currencyChangeResult: Result,
    currency: CurrencyEnum,
    isThemeDark: Boolean,
    onThemeChange: (isThemeDark) -> Unit,
    onNewCurrency: (CurrencyEnum) -> Unit,
    onCurrencyChangeResult: (Result) -> Unit,
    onCurrencyChangeTap: () -> Unit,
    onCurrencyInfoDismiss: () -> Unit,
    onSignOut: () -> Unit,
    onError: (String) -> Unit) {
    val context = LocalContext.current
    val currencySheetState = rememberModalBottomSheetState()
    val currencyInfoSheetState = rememberModalBottomSheetState()
    var showCurrencySheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ChangeThemeSwitch(isThemeDark, onCheckedChange = onThemeChange)
        SettingsButton(textId = R.string.change_currency,
            isEnabled = lastTimeCurrencyUpdatedResult !is Result.InProgress,
            onClick = {
            onCurrencyChangeResult(Result.Idle)
            onCurrencyChangeTap()
            showCurrencySheet = true
        })
        SettingsButton(textId = R.string.privacy_policy, onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/EvgenSuit/PrivacyPolicies/blob/master/MoneyMonocle.md")))
        })
        SettingsButton(textId = R.string.sign_out, textColor = MaterialTheme.colorScheme.error,
            onClick = onSignOut)
        Spacer(modifier = Modifier.weight(1f))
        IconsBy()
    }
    if (lastTimeCurrencyUpdatedResult is Result.Success && showCurrencySheet) {
        if (lastTimeCurrencyUpdated == null) {
            CurrencyInfoBottomSheet(sheetState = currencyInfoSheetState,
                onSheetDismiss = onCurrencyInfoDismiss)
        }
        else if (lastTimeCurrencyUpdated == -1L || Instant.now().toEpochMilli() - lastTimeCurrencyUpdated >= 24*60*60*1000 || currencySheetState.isVisible) {
            ChangeCurrencyBottomSheet(
                currencyChangeResult = currencyChangeResult,
                sheetState = currencySheetState,
                currency = currency,
                onSheetDismiss = {
                    scope.launch {
                        currencySheetState.hide()
                        showCurrencySheet = false
                    }},
                onNewCurrency = onNewCurrency)
        } else if (!currencySheetState.isVisible) {
            onError(stringResource(id = R.string.already_changed_currency))
            showCurrencySheet = false
        }
    }
}

@Composable
fun SettingsButton(@StringRes textId: Int,
                   textColor: Color = MaterialTheme.colorScheme.onBackground,
                   isEnabled: Boolean = true,
                   onClick: () -> Unit) {
    val text = stringResource(textId)
    val shape = RoundedCornerShape(dimensionResource(id = R.dimen.button_corner))
    ElevatedButton(onClick = onClick,
        colors = ButtonDefaults.elevatedButtonColors(containerColor = MaterialTheme.colorScheme.background),
        shape = shape,
        enabled = isEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(shape)
            .testTag(text)) {
        Row(Modifier
            .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.displaySmall.copy(textColor))
        }
    }
}

@Composable
fun ChangeThemeSwitch(isThemeDark: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(checked = isThemeDark, onCheckedChange = onCheckedChange,
        thumbContent = {
            Icon(painterResource(id = if (isThemeDark) R.drawable.night else R.drawable.light),
                contentDescription = "${if (isThemeDark) "Night" else "Light"}Mode",
                ) },
        modifier = Modifier
            .scale(1.7f)
            .padding(30.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyInfoBottomSheet(
    sheetState: SheetState,
    onSheetDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onSheetDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(id = R.string.be_advised),
                style = MaterialTheme.typography.displayMedium)
            Text(stringResource(id = R.string.currency_conversion_warning),
                style = MaterialTheme.typography.displaySmall)
            ElevatedButton(onClick = onSheetDismiss,
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.button_corner)),
                modifier = Modifier.size(150.dp, 50.dp)) {
                Text(stringResource(id = R.string.ok),
                    style = MaterialTheme.typography.displaySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeCurrencyBottomSheet(
    currencyChangeResult: Result,
    sheetState: SheetState,
    currency: CurrencyEnum,
    onSheetDismiss: () -> Unit,
    onNewCurrency: (CurrencyEnum) -> Unit) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var selectedCurrency by remember { mutableStateOf(currency) }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.success))
    val progress = animateLottieCompositionAsState(composition, isPlaying = currencyChangeResult is Result.Success)
    val dynamicProperties = rememberLottieDynamicProperties(rememberLottieDynamicProperty(
        property = LottieProperty.COLOR_FILTER,
        value = SimpleColorFilter(Color.Green.toArgb()),
        keyPath = arrayOf("**"),
    ))
    LaunchedEffect(progress.isAtEnd) {
       if (currencyChangeResult is Result.Success && progress.isAtEnd) onSheetDismiss()
    }
    ModalBottomSheet(onDismissRequest = onSheetDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .padding(bottom = 50.dp),
            verticalArrangement = Arrangement.spacedBy(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (currencyChangeResult) {
                !is Result.Success -> Column(Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(id = if (currencyChangeResult !is Result.InProgress)
                        R.string.change_currency_from else R.string.progress),
                        style = MaterialTheme.typography.titleMedium)
                    if (currencyChangeResult is Result.InProgress) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
                else -> LottieAnimation(
                    composition = composition, progress = { progress.value },
                    dynamicProperties = dynamicProperties
                )
            }
            if (currencyChangeResult is Result.Idle || currencyChangeResult is Result.Error) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(currency.name,
                        style = MaterialTheme.typography.displaySmall)
                    Text(stringResource(id = R.string.change_currency_to))
                    CurrencyButton(dropdownExpanded = dropdownExpanded, currency = selectedCurrency,
                        onCurrencySelect = {selectedCurrency = it}, onDropdownTap = {dropdownExpanded = it})
                }
                ElevatedButton(onClick = { onNewCurrency(selectedCurrency) },
                    enabled = selectedCurrency != currency
                ) {
                    Text(stringResource(id = R.string.confirm),
                        style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
@Composable
fun IconsBy() {
    val context = LocalContext.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://icons8.com/")))
        }) {
            Text(buildAnnotatedString {
                append(stringResource(id = R.string.icons_by))
                withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append("Icons8")
                }
            }, style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 15.sp
            ))
        }
    }
}

/*@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun BottomSheetPreview() {
    MoneyMonocleTheme {
        Surface {
            ChangeCurrencyBottomSheet(
                currencyChangeResult = Result.Idle,
                sheetState = rememberStandardBottomSheetState(),
                currency = CurrencyEnum.EUR,
                onSheetDismiss = { *//*TODO*//* }) {

            }
        }
    }
}*/

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun CurrencyInfoBottomSheetPreview() {
    MoneyMonocleTheme {
        Surface {
            CurrencyInfoBottomSheet(sheetState = rememberStandardBottomSheetState()) {
                
            }
        }
    }
}

/*
@Preview
@Composable
fun SettingsScreenPreview() {
    MoneyMonocleTheme {
        Surface {
            SettingsScreenContent(
                currencyChangeResult = Result.Idle,
                currency = CurrencyEnum.EUR,
                isThemeDark = true,
                onThemeChange = {},
                onSignOut = {},
                onCurrencyChangeResult = {},
                onNewCurrency = {}
            )
        }
    }
}*/
