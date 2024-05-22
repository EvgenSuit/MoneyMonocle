package com.money.monocle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.money.monocle.ui.CustomErrorSnackbar
import com.money.monocle.ui.theme.MoneyMonocleTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContent {
            val snackbarHostState = remember {
                SnackbarHostState()
            }
            val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
            val scope = rememberCoroutineScope()
            MoneyMonocleTheme {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)) {
                        MoneyMonocleNavHost(
                            onError = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(it)
                                }
                            }
                        )
                        CustomErrorSnackbar(snackbarHostState = snackbarHostState,
                            swipeToDismissBoxState = swipeToDismissBoxState,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(20.dp))
                    }
                }
            }
        }
    }