package com.money.monocle.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.money.monocle.R
import com.money.monocle.data.CurrencyEnum
import com.money.monocle.ui.screens.components.CurrencyDropdown
import com.money.monocle.ui.screens.components.rememberImeState
import com.money.monocle.ui.theme.MoneyMonocleTheme

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
    val imeVisible by rememberImeState()
    val maxBalanceLength = integerResource(id = R.integer.max_init_balance_length)
    LaunchedEffect(imeVisible) {
        if (imeVisible) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(100.dp),
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
                .fillMaxWidth(0.9f)
                .fillMaxHeight()
        ) {
            Text(
                stringResource(id = R.string.specify_current_balance),
                style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = (amount ?: "").toString(),
                onValueChange = {newValue ->
                    if ((newValue.isEmpty() || newValue.toFloatOrNull() != null)
                        && newValue.length <= maxBalanceLength) {
                        amount = newValue
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(20.dp),
                suffix = {
                    CurrencyDropdown(dropdownExpanded = dropdownExpanded,
                        currency = currency,
                        onCurrencySelect = { currency = it },
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

