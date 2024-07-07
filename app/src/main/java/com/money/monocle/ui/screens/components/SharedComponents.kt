package com.money.monocle.ui.screens.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewTreeObserver
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieAnimationState
import com.airbnb.lottie.compose.LottieCompositionResult
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.money.monocle.R
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.domain.CustomResult
import com.money.monocle.domain.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SnackbarController(
    private val snackbarHostState: SnackbarHostState,
    private val coroutineScope: CoroutineScope,
    private val context: Context,
) {
    fun showSnackbar(result: CustomResult) {
        if (result is CustomResult.DynamicError || result is CustomResult.ResourceError) {
            coroutineScope.launch {
                // give time for fetch result to update to "InProgress" if retrying
                delay(100)
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(result.error.asString(context))
            }
        }
    }
}


@Composable
fun CustomTopBar(text: String,
                 isInProgress: Boolean,
                 onNavigateBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,

                ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "BackButton")
                }
                Text(text = text,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f))
            }
            if (isInProgress) {
                LinearProgressIndicator(modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomErrorSnackbar(snackbarHostState: SnackbarHostState,
                        swipeToDismissBoxState: SwipeToDismissBoxState) {
    SwipeToDismissBox(state = swipeToDismissBoxState, backgroundContent = {}) {
        SnackbarHost(hostState = snackbarHostState,
                    snackbar = {data ->
                Snackbar(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(data.visuals.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.testTag(stringResource(id = R.string.error_snackbar)))
                    }
                }
            })
        }
}

@Composable
fun InProgressLinearIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(id = R.string.progress),
            style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}
@Composable
fun CategoryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    val maxLength = integerResource(id = R.integer.max_custom_category_name_length)
    OutlinedTextField(value = value,
        enabled = enabled,
        singleLine = true,
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        onValueChange = {
            if (it.length < maxLength) onValueChange(it)
        },
        modifier = modifier
            .fillMaxWidth()
            .testTag(stringResource(id = R.string.text_field)))
}

const val LOTTIE_SPEED = 1.5f

@Composable
fun SuccessLottieAnimation(
    composition: LottieComposition?,
    progress: LottieAnimationState,
    result: CustomResult,
    onDismiss: () -> Unit) {
    LaunchedEffect(progress.isAtEnd) {
        if (result.isSuccess() && progress.isAtEnd) onDismiss()
    }
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR_FILTER,
            value = SimpleColorFilter(Color.Green.toArgb()),
            keyPath = arrayOf("**")))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        LottieAnimation(composition = composition,
            progress = { progress.value },
            dynamicProperties = dynamicProperties)
    }
}

@Composable
fun NothingToShowText() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(stringResource(id = R.string.nothing_to_show),
            style = MaterialTheme.typography.displaySmall)
    }
}

@Composable
fun PrivacyPolicyText() {
    val context = LocalContext.current
    Text(stringResource(R.string.privacy_policy),
        color = MaterialTheme.colorScheme.onBackground,
        style = TextStyle(
            textDecoration = TextDecoration.Underline
        ),
        modifier = Modifier.clickable {
            val uri = Uri.parse("https://github.com/EvgenSuit/PrivacyPolicies/blob/master/MoneyMonocle.md")
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    )
}
@Composable
fun CommonButton(
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String) {
    ElevatedButton(onClick = onClick,
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.button_corner)),
        colors = ButtonDefaults.buttonColors(),
        enabled = enabled,
        modifier = modifier.fillMaxWidth()) {
        Text(text, style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(10.dp))
    }
}
@Composable
fun CurrencyDropdown(
    dropdownExpanded: Boolean,
    currency: CurrencyEnum,
    onCurrencySelect: (CurrencyEnum) -> Unit,
    onDropdownTap: (Boolean) -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
    ) {
        ElevatedButton(onClick = { onDropdownTap(!dropdownExpanded) },
            shape = RoundedCornerShape(dimensionResource(R.dimen.button_corner)),
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(dimensionResource(R.dimen.button_corner)))
                .testTag(currency.name)) {
            Text(currency.name,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(10.dp))
        }
        DropdownMenu(expanded = dropdownExpanded,
            onDismissRequest = { onDropdownTap(false) },
            properties = PopupProperties(focusable = false)
        ) {
            Column(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .verticalScroll(rememberScrollState())
            ) {
                for (entry in CurrencyEnum.entries) {
                    DropdownMenuItem(
                        text = { Text(entry.name,
                            style = MaterialTheme.typography.displaySmall) },
                        onClick = {
                            onCurrencySelect(entry)
                            onDropdownTap(false)
                        }, modifier = Modifier.testTag(entry.name))
                }
            }
        }
    }
}

@Composable
fun AnimatedItem(
    content: @Composable () -> Unit
) {
    // rememberSaveable is necessary, since using just remember makes an item not appear
    // after it was not displayed or visible on the screen
    var isVisible by rememberSaveable {
        mutableStateOf(false)
    }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    AnimatedVisibility(isVisible, enter = fadeIn(tween(integerResource(id = R.integer.list_item_enter_duration)))) {
        content()
    }
}

@Composable
fun rememberImeState(): State<Boolean> {
    val imeState = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val view = LocalView.current
    DisposableEffect(key1 = view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            coroutineScope.launch {
                val isKeyboardOpen = ViewCompat.getRootWindowInsets(view)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) ?: true
                imeState.value = isKeyboardOpen
            }
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)

        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }
    return imeState
}