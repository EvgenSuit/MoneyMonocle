package com.money.monocle.ui.screens.auth

import android.content.IntentSender
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.money.monocle.R
import com.money.monocle.domain.Result
import com.money.monocle.ui.screens.components.LoadScreen
import com.money.monocle.ui.screens.components.PrivacyPolicyText
import com.money.monocle.ui.presentation.auth.AuthViewModel
import com.money.monocle.ui.theme.MoneyMonocleTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onSignIn: () -> Unit,
    onError: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState.authResult) {
        if (uiState.authResult is Result.Success) {
            onSignIn()
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.authResultFlow.collectLatest {res ->
            if (res is Result.Error) {
                onError(res.error)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LoadScreen()
        AuthContentColumn(
            authResult = uiState.authResult,
            onSignIn = viewModel::onSignIn,
            onSignInWithIntent = viewModel::onSignInWithIntent,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp)
        )
    }
}

@Composable
fun AuthContentColumn(
    authResult: Result,
    onSignIn: suspend () -> IntentSender?,
    onSignInWithIntent: (ActivityResult) -> Unit,
    modifier: Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp),
        modifier = modifier
    ) {
        GoogleSignInButton(
            enabled = authResult !is Result.InProgress && authResult !is Result.Success,
            onSignIn,
            onSignInWithIntent)
        if (authResult is Result.InProgress) {
            LinearProgressIndicator()
        }
        PrivacyPolicyText()
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
            .size(200.dp, 80.dp)
            .clip(RoundedCornerShape(dimensionResource(R.dimen.button_corner)))
            .testTag("Google sign in")) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(painter = painterResource(R.drawable.google_icon),
                contentDescription = null)
            Text(
                stringResource(R.string.sign_in),
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
            GoogleSignInButton(enabled = true, onSignIn = {null}) {
                
            }
        }
    }
}