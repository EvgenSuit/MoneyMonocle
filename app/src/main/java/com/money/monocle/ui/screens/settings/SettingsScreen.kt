package com.money.monocle.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.money.monocle.R
import com.money.monocle.ui.presentation.settings.SettingsViewModel
import com.money.monocle.ui.theme.MoneyMonocleTheme

private typealias isThemeDark = Boolean
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isThemeDark = uiState.isThemeDark
    if (isThemeDark != null) {
        SettingsScreenContent(
            isThemeDark = isThemeDark,
            onThemeChange = viewModel::changeThemeMode,
            onSignOut = viewModel::signOut)
    }
}

@Composable
fun SettingsScreenContent(isThemeDark: Boolean,
                          onThemeChange: (isThemeDark) -> Unit,
                          onSignOut: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ChangeThemeSwitch(isThemeDark, onCheckedChange = onThemeChange)
        PrivacyPolicy()
        SignOut(onSignOut = onSignOut)
        Spacer(modifier = Modifier.weight(1f))
        IconsBy()
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
@Composable
fun SignOut(onSignOut: () -> Unit) {
    TextButton(onClick = onSignOut,
        modifier = Modifier.testTag("SignOut")) {
        Text(stringResource(id = R.string.sign_out),
            style = MaterialTheme.typography.displaySmall.copy(
                MaterialTheme.colorScheme.error
            ))
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

@Composable
fun PrivacyPolicy() {
    val context = LocalContext.current
    TextButton(onClick = {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/EvgenSuit/PrivacyPolicies/blob/master/MoneyMonocle.md")))
    }) {
        Text(stringResource(id = R.string.privacy_policy),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    MoneyMonocleTheme {
        Surface {
            SettingsScreenContent(
                isThemeDark = true,
                onThemeChange = {},
                onSignOut = {}
            )
        }
    }
}