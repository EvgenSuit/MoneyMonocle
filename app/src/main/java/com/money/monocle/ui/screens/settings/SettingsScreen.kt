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
import androidx.compose.runtime.DisposableEffect
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
import com.money.monocle.LocalSnackbarController
import com.money.monocle.R
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.isError
import com.money.monocle.ui.presentation.settings.SettingsViewModel
import com.money.monocle.ui.screens.components.CommonButton
import com.money.monocle.ui.screens.components.CurrencyDropdown
import com.money.monocle.ui.screens.components.LOTTIE_SPEED
import com.money.monocle.ui.screens.components.SuccessLottieAnimation
import com.money.monocle.ui.theme.MoneyMonocleTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

private typealias isThemeDark = Boolean
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onManageCategories: () -> Unit = {},
    onManageAccounts: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isThemeDark = uiState.isThemeDark
    val balance = uiState.balance
    val snackbarController = LocalSnackbarController.current
    LaunchedEffect(Unit) {
        viewModel.retryIfNecessary()
    }
    LaunchedEffect(uiState.currencyChangeResult) {
        snackbarController.showSnackbar(uiState.currencyChangeResult)
    }
    LaunchedEffect(uiState.lastTimeCurrencyUpdatedResult) {
        snackbarController.showSnackbar(uiState.lastTimeCurrencyUpdatedResult)
    }
    AnimatedVisibility (isThemeDark != null && balance.currency != -1,
        enter = fadeIn()
    ) {
        val state = SettingsScreenState(
            lastTimeCurrencyUpdatedResult = uiState.lastTimeCurrencyUpdatedResult,
            lastTimeCurrencyUpdated = uiState.lastTimeCurrencyUpdated,
            currencyChangeResult = uiState.currencyChangeResult,
            currency = CurrencyEnum.entries[balance.currency],
            isThemeDark = isThemeDark!!,
            onThemeChange = viewModel::changeThemeMode,
            onManageCategories = onManageCategories,
            onManageAccounts = onManageAccounts,
            onNewCurrency = viewModel::changeCurrency,
            onCurrencyChangeResult = viewModel::updateCurrencyChangeResult,
            onCurrencyChangeTap = viewModel::checkLastTimeUpdated,
            onCurrencyInfoDismiss = viewModel::changeLastTimeUpdated,
            onSnackbarShow = { snackbarController.showSnackbar(it) },
            onSignOut = viewModel::signOut
        )
        SettingsScreenContent(state)
    }
}

data class SettingsScreenState(
    val lastTimeCurrencyUpdatedResult: CustomResult,
    val lastTimeCurrencyUpdated: Long?,
    val currencyChangeResult: CustomResult,
    val currency: CurrencyEnum,
    val isThemeDark: Boolean,
    val onThemeChange: (isThemeDark) -> Unit,
    val onNewCurrency: (CurrencyEnum) -> Unit,
    val onManageCategories: () -> Unit,
    val onCurrencyChangeResult: (CustomResult) -> Unit,
    val onCurrencyChangeTap: () -> Unit,
    val onCurrencyInfoDismiss: () -> Unit,
    val onManageAccounts: () -> Unit,
    val onSnackbarShow: (CustomResult) -> Unit,
    val onSignOut: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(state: SettingsScreenState) {
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
        ChangeThemeSwitch(state.isThemeDark, onCheckedChange = state.onThemeChange)
        SettingsButton(textId = R.string.change_currency,
            isEnabled = state.lastTimeCurrencyUpdatedResult !is CustomResult.InProgress,
            onClick = {
            state.onCurrencyChangeResult(CustomResult.Idle)
            state.onCurrencyChangeTap()
            showCurrencySheet = true
        })
        SettingsButton(textId = R.string.manage_categories, onClick = state.onManageCategories)
        SettingsButton(textId = R.string.manage_accounts, onClick = state.onManageAccounts)
        SettingsButton(textId = R.string.privacy_policy, onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/EvgenSuit/PrivacyPolicies/blob/master/MoneyMonocle.md")))
        }, modifier = Modifier.padding(top = 10.dp))
        SettingsButton(textId = R.string.sign_out, textColor = MaterialTheme.colorScheme.error,
            onClick = state.onSignOut)
        Spacer(modifier = Modifier.weight(1f))
        IconsBy()
    }
    if (state.lastTimeCurrencyUpdatedResult is CustomResult.Success && showCurrencySheet) {
        if (state.lastTimeCurrencyUpdated == null) {
            CurrencyInfoBottomSheet(sheetState = currencyInfoSheetState,
                onSheetDismiss = state.onCurrencyInfoDismiss)
        }
        else if (state.lastTimeCurrencyUpdated == -1L || Instant.now().toEpochMilli() - state.lastTimeCurrencyUpdated >= 24*60*60*1000 || currencySheetState.isVisible) {
            ChangeCurrencyBottomSheet(
                currencyChangeResult = state.currencyChangeResult,
                sheetState = currencySheetState,
                currency = state.currency,
                onSheetDismiss = {
                    scope.launch {
                        currencySheetState.hide()
                        showCurrencySheet = false
                    }},
                onNewCurrency = state.onNewCurrency)
        } else if (!currencySheetState.isVisible) {
            state.onSnackbarShow(CustomResult.DynamicError(stringResource(id = R.string.already_changed_currency)))
            showCurrencySheet = false
        }
    }
}

@Composable
fun SettingsButton(@StringRes textId: Int,
                   textColor: Color = MaterialTheme.colorScheme.onBackground,
                   isEnabled: Boolean = true,
                   onClick: () -> Unit,
                   modifier: Modifier = Modifier) {
    val text = stringResource(textId)
    val shape = RoundedCornerShape(dimensionResource(id = R.dimen.button_corner))
    ElevatedButton(onClick = onClick,
        colors = ButtonDefaults.elevatedButtonColors(containerColor = MaterialTheme.colorScheme.background),
        shape = shape,
        enabled = isEnabled,
        modifier = modifier
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
                modifier = Modifier.padding(5.dp),
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
        modifier = Modifier
            .fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .padding(bottom = dimensionResource(id = R.dimen.sheet_bottom_padding)),
            verticalArrangement = Arrangement.spacedBy(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(id = R.string.be_advised),
                style = MaterialTheme.typography.displayMedium)
            Text(stringResource(id = R.string.currency_conversion_warning),
                style = MaterialTheme.typography.displaySmall)
            CommonButton(onClick = onSheetDismiss, textId = R.string.ok)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeCurrencyBottomSheet(
    currencyChangeResult: CustomResult,
    sheetState: SheetState,
    currency: CurrencyEnum,
    onSheetDismiss: () -> Unit,
    onNewCurrency: (CurrencyEnum) -> Unit) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var selectedCurrency by remember { mutableStateOf(currency) }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.success))
    val progress = animateLottieCompositionAsState(composition,
        speed = LOTTIE_SPEED,
        isPlaying = currencyChangeResult is CustomResult.Success)
    ModalBottomSheet(onDismissRequest = onSheetDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .padding(bottom = dimensionResource(id = R.dimen.sheet_bottom_padding)),
            verticalArrangement = Arrangement.spacedBy(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (currencyChangeResult) {
                !is CustomResult.Success -> Column(Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(id = if (currencyChangeResult !is CustomResult.InProgress)
                        R.string.change_currency_from else R.string.progress),
                        style = MaterialTheme.typography.titleMedium)
                    if (currencyChangeResult is CustomResult.InProgress) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
                else -> SuccessLottieAnimation(
                    result = currencyChangeResult,
                    composition = composition,
                    progress = progress,
                    onDismiss = onSheetDismiss)
            }
            if (currencyChangeResult is CustomResult.Idle || currencyChangeResult.isError()) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(currency.name,
                        style = MaterialTheme.typography.displaySmall)
                    Text(stringResource(id = R.string.change_currency_to))
                    CurrencyDropdown(dropdownExpanded = dropdownExpanded, currency = selectedCurrency,
                        onCurrencySelect = {selectedCurrency = it}, onDropdownTap = {dropdownExpanded = it})
                }
                CommonButton(onClick = { onNewCurrency(selectedCurrency) },
                    enabled = selectedCurrency != currency,
                    textId = R.string.confirm
                )
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
                currencyChangeResult = CustomResult.Success,
                sheetState = rememberStandardBottomSheetState(),
                currency = CurrencyEnum.EUR,
                onSheetDismiss = { }) {

            }
        }
    }
}*/

/*@OptIn(ExperimentalMaterial3Api::class)
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
*/

@Preview
@Composable
fun SettingsScreenPreview() {
    val state = SettingsScreenState(
        currencyChangeResult = CustomResult.Success,
        currency = CurrencyEnum.EUR,
        isThemeDark = true,
        onThemeChange = {},
        onSignOut = {},
        onCurrencyChangeResult = {},
        lastTimeCurrencyUpdated = 0,
        lastTimeCurrencyUpdatedResult = CustomResult.Success,
        onCurrencyInfoDismiss = {},
        onCurrencyChangeTap = {},
        onNewCurrency = {},
        onSnackbarShow = {},
        onManageCategories = {},
        onManageAccounts = {}
    )
    MoneyMonocleTheme {
        Surface {
            SettingsScreenContent(state)
        }
    }
}
