package com.money.monocle.ui.screens.components

import android.content.Intent
import android.net.Uri
import android.view.ViewTreeObserver
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.money.monocle.R
import com.money.monocle.data.CurrencyEnum
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomErrorSnackbar(snackbarHostState: SnackbarHostState,
                        swipeToDismissBoxState: SwipeToDismissBoxState) {
    SwipeToDismissBox(state = swipeToDismissBoxState, backgroundContent = {}) {
        SnackbarHost(hostState = snackbarHostState,
                    snackbar = {data ->
                Snackbar(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(data.visuals.message,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            })
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
fun CurrencyButton(
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
                .clip(RoundedCornerShape(dimensionResource(R.dimen.button_corner)))) {
            Text(currency.name)
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
                        text = { Text(entry.name) },
                        onClick = {
                            onCurrencySelect(entry)
                            onDropdownTap(false)
                        })
                }
            }
        }
    }
}

@Composable
fun LoadScreen() {
    Image(painter = painterResource(R.drawable.splash_screen),
        contentScale = ContentScale.FillBounds,
        contentDescription = null,
        modifier = Modifier.fillMaxSize())
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