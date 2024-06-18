package com.money.monocle.ui.screens.auth

import android.content.IntentSender
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.money.monocle.LocalSnackbarController
import com.money.monocle.R
import com.money.monocle.domain.Result
import com.money.monocle.domain.useCases.AuthType
import com.money.monocle.domain.useCases.FieldType
import com.money.monocle.ui.presentation.auth.AuthViewModel
import com.money.monocle.ui.screens.components.PrivacyPolicyText
import com.money.monocle.ui.theme.MoneyMonocleTheme
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onSignIn: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManger = LocalFocusManager.current
    val snackbarController = LocalSnackbarController.current
    LaunchedEffect(uiState.authResult) {
        if (uiState.authResult is Result.Success) {
            focusManger.clearFocus(true)
            onSignIn()
        }
    }
    LaunchedEffect(uiState.authResult) {
        snackbarController.showErrorSnackbar(uiState.authResult)
    }
    AuthContentColumn(
        uiState = uiState,
        onUsername = viewModel::onUsername,
        onEmail = viewModel::onEmail,
        onPassword = viewModel::onPassword,
        onChangeAuthType = viewModel::changeAuthType,
        onCustomAuth = viewModel::onCustomAuth,
        onSignGoogleSignIn = viewModel::onGoogleSignIn,
        onSignInWithIntent = viewModel::onSignInWithIntent,
    )
}

@Composable
fun AuthContentColumn(
    uiState: AuthViewModel.UiState,
    onUsername: (String) -> Unit,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onChangeAuthType: () -> Unit,
    onCustomAuth: () -> Unit,
    onSignGoogleSignIn: suspend () -> IntentSender?,
    onSignInWithIntent: (ActivityResult) -> Unit) {
    val authResult = uiState.authResult
    val authEnabled = authResult !is Result.InProgress && authResult !is Result.Success
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(10.dp)
    ) {
        Title()
        AuthFieldsColumn(
            uiState = uiState,
            authEnabled = authEnabled,
            onUsername = onUsername,
            onEmail = onEmail,
            onPassword = onPassword,
            onChangeAuthType = onChangeAuthType,
            onAuth = onCustomAuth)
        Text("or")
        GoogleSignInButton(
            enabled = authEnabled,
            onSignGoogleSignIn,
            onSignInWithIntent)
        Spacer(modifier = Modifier.weight(1f))
        PrivacyPolicyText()
    }
}

@Composable
fun AuthFieldsColumn(
    uiState: AuthViewModel.UiState,
    authEnabled: Boolean,
    onUsername: (String) -> Unit,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onChangeAuthType: () -> Unit,
    onAuth: () -> Unit
) {
    val shape = RoundedCornerShape(dimensionResource(id = R.dimen.auth_corner))
    val validationState = uiState.validationState
    val authState = uiState.authState
    val authResult = uiState.authResult
    val context = LocalContext.current
    val usernameValidationError = validationState.usernameValidationError.asString(context)
    val emailValidationError = validationState.emailValidationError.asString(context)
    val passwordValidationError = validationState.passwordValidationError.asString(context)
    val authButtonEnabled = (usernameValidationError.isEmpty() && authState.username != null || uiState.authType != AuthType.SIGN_UP) &&
            (emailValidationError.isEmpty() && authState.email != null) &&
            (passwordValidationError.isEmpty() && authState.password != null) && authEnabled
    ElevatedCard(
        modifier = Modifier
            .width(350.dp)
            .shadow(
                dimensionResource(id = R.dimen.shadow_elevation),
                shape = shape,
                spotColor = MaterialTheme.colorScheme.primary
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.onBackground.copy(0.1f))
            .border(1.dp, MaterialTheme.colorScheme.primary, shape)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            AnimatedVisibility(uiState.authType == AuthType.SIGN_UP) {
                CustomInputField(
                    fieldType = FieldType.USERNAME,
                    enabled = authEnabled,
                    value = authState.username,
                    error = usernameValidationError,
                    onValueChange = onUsername)
            }
            CustomInputField(
                fieldType = FieldType.EMAIL,
                enabled = authEnabled,
                value = authState.email,
                error = emailValidationError,
                onValueChange = onEmail)
            CustomInputField(
                fieldType = FieldType.PASSWORD,
                enabled = authEnabled,
                value = authState.password,
                error = passwordValidationError,
                onValueChange = onPassword)
            CustomAuthButton(
                authType = uiState.authType,
                enabled = authButtonEnabled,
                onClick = onAuth)
            if (authResult is Result.InProgress) {
                LinearProgressIndicator()
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (uiState.authType == AuthType.SIGN_IN) {
                    Text(stringResource(id = R.string.dont_have_an_account),
                        style = MaterialTheme.typography.labelSmall)
                }
                GoToText(authType = uiState.authType,
                    enabled = authEnabled,
                    onClick = onChangeAuthType)
            }
        }
    }
}

@Composable
fun CustomInputField(
    enabled: Boolean,
    fieldType: FieldType,
    value: String?,
    error: String,
    onValueChange: (String) -> Unit) {
    val shape = RoundedCornerShape(dimensionResource(id = R.dimen.auth_corner))
    val fieldTypeString = stringResource(id = when(fieldType) {
        FieldType.USERNAME -> R.string.username
        FieldType.EMAIL -> R.string.email
        FieldType.PASSWORD -> R.string.password
    })
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value ?: "",
            isError = error.isNotEmpty(),
            onValueChange = onValueChange,
            enabled = enabled,
            shape = shape,
            keyboardOptions = KeyboardOptions(imeAction =
            if (fieldType == FieldType.USERNAME || fieldType == FieldType.EMAIL) ImeAction.Next else ImeAction.Done),
            placeholder = {
                if (value.isNullOrBlank())
                    Text(fieldTypeString)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag(fieldTypeString)
        )
        if (error.isNotEmpty()) {
            Text(error,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(error))
        }
    }
}

@Composable
fun CustomAuthButton(
    authType: AuthType,
    enabled: Boolean,
    onClick: () -> Unit) {
    val label = stringResource(
        id = when (authType) {
            AuthType.SIGN_IN -> R.string.sign_in
            AuthType.SIGN_UP -> R.string.sign_up
        }
    )
    ElevatedButton(onClick = onClick,
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.button_corner)),
        colors = ButtonDefaults.buttonColors(),
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(10.dp))
    }
}

@Composable
fun Title() {
    val gradientColors = listOf(MaterialTheme.colorScheme.onBackground,
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.onPrimary.copy(0.5f))
    var offsetX by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = tween(3000)) {value, _ ->
            offsetX = value
        }
    }
    val brush = Brush.linearGradient(
        colors = gradientColors,
        start = Offset(offsetX, 0f),
        end = Offset(offsetX + 200f, 100f)
    )
    Text(stringResource(id = R.string.app_name),
        style = MaterialTheme.typography.titleLarge.copy(brush = brush))
}

@Composable
fun GoToText(authType: AuthType,
             enabled: Boolean,
             onClick: () -> Unit) {
    val label = stringResource(
        id = if (authType == AuthType.SIGN_IN) R.string.go_to_signup else R.string.go_to_signin
    )
    TextButton(onClick = onClick,
        enabled = enabled) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(
            textDecoration = TextDecoration.Underline
        ))
    }
}

@Composable
fun GoogleSignInButton(
    enabled: Boolean,
    onSignIn: suspend () -> IntentSender?,
    onSignInWithIntent: (ActivityResult) -> Unit) {
    val launcher = googleSignInLauncher(onSignInWithIntent = onSignInWithIntent)
    val scope = rememberCoroutineScope()
    ElevatedButton(onClick = {
            scope.launch {
                val signInIntentSender = onSignIn()
                if (signInIntentSender != null) {
                    launcher.launch(IntentSenderRequest.Builder(signInIntentSender).build())
                }
            }
    },
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Blue
        ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.button_corner)),
        modifier = Modifier
            .height(IntrinsicSize.Max)
            .width(IntrinsicSize.Max)
            .clip(RoundedCornerShape(dimensionResource(R.dimen.button_corner)))
            .testTag("Google sign in")) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp)
        ) {
            Image(painter = painterResource(R.drawable.google_icon),
                contentDescription = null)
            Text(
                stringResource(R.string.sign_in_with_google),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

@Composable
fun googleSignInLauncher(onSignInWithIntent: (ActivityResult) -> Unit): ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult> {
    return rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        onSignInWithIntent(res)
    }
}


@Preview
@Composable
fun AuthScreenPreview() {
    MoneyMonocleTheme {
        Surface {
            AuthContentColumn(
                uiState = AuthViewModel.UiState(
                    authType = AuthType.SIGN_UP
                ),
                onUsername = {},
                onEmail = {},
                onPassword = {},
                onChangeAuthType = {},
                onCustomAuth = {},
                onSignGoogleSignIn = { null }) {

            }
        }
    }
}