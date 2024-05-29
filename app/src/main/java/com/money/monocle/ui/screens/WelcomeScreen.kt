package com.money.monocle.ui.screens

import android.util.Log
import android.widget.Space
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.google.type.Money
import com.money.monocle.R
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.ui.screens.components.rememberImeState
import com.money.monocle.ui.theme.MoneyMonocleTheme
import java.text.DecimalFormatSymbols

@Preview
@Composable
fun WelcomeScreenPreview() {
    MoneyMonocleTheme {
        Surface {
            WelcomeScreen(isSubmitEnabled = true) {_, _ ->
                
            }
        }
    }
}

@Composable
fun WelcomeScreen(
    isSubmitEnabled: Boolean,
    onBalance: (CurrencyEnum, Float) -> Unit
) {
    var dropdownExpanded by remember {
        mutableStateOf(false)
    }
    var amount by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    var currency by rememberSaveable {
        mutableStateOf(CurrencyEnum.USD)
    }
    val scrollState = rememberScrollState()
    val imeState by rememberImeState()
    LaunchedEffect(imeState) {
        if (imeState) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(30.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(10.dp)
    ) {
        Text(text = stringResource(id = R.string.welcome),
            style = MaterialTheme.typography.titleLarge)
        Column(
            verticalArrangement = Arrangement.spacedBy(40.dp,
                Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight()
                .weight(1f)
        ) {
            Text(
                stringResource(id = R.string.specify_current_balance),
                style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = (amount ?: "").toString(),
                onValueChange = {newValue ->
                    if (newValue.isEmpty() || newValue.toFloatOrNull() != null) {
                        amount = newValue
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(20.dp),
                suffix = {
                    CurrencyButton(dropdownExpanded = dropdownExpanded,
                        currency = currency,
                        onCurrency = { currency = it },
                        onDropdownTap = { dropdownExpanded = it })
                },
               placeholder = { if (amount == null) Text("0.0") },
                modifier = Modifier.testTag("Welcome screen text field")
            )
            ElevatedButton(onClick = {
                onBalance(currency, if (amount?.isNotBlank() == true) amount!!.toFloat() else 0f)
            },
                enabled = amount?.isNotBlank() == true && amount!!.toFloat() >= 0f && isSubmitEnabled,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = Color.Blue
                ),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.button_corner)),
                modifier = Modifier
                    .clip(RoundedCornerShape(dimensionResource(id = R.dimen.button_corner)))
                    .size(200.dp, 80.dp)
                    .testTag("Welcome screen submit button")
            ) {
                Text(
                    stringResource(id = R.string.submit),
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun CurrencyButton(
    dropdownExpanded: Boolean,
    currency: CurrencyEnum,
    onCurrency: (CurrencyEnum) -> Unit,
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
                            onCurrency(entry)
                            onDropdownTap(false)
                        })
                }
            }
        }
    }
}