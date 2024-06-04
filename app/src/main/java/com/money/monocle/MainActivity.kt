package com.money.monocle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.ui.screens.components.CustomErrorSnackbar
import com.money.monocle.ui.theme.MoneyMonocleTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val isThemeDark = mutableStateOf(true)
    @Inject
    lateinit var dataStoreManager: DataStoreManager

    private fun collectThemeMode() {
        lifecycleScope.launch {
            dataStoreManager.themeFlow().collectLatest {
                isThemeDark.value = it
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        runBlocking {
            dataStoreManager.changeAccountState(false)
        }
        collectThemeMode()
        setContent {
            val snackbarHostState = remember {
                SnackbarHostState()
            }
            var isSnackbarShown by rememberSaveable {
                mutableStateOf(false)
            }
            var error by rememberSaveable {
                mutableStateOf("")
            }
            val swipeToDismissBoxState = rememberSwipeToDismissBoxState(confirmValueChange = {value ->
                if (value != SwipeToDismissBoxValue.Settled) {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        true
                } else false
            })
            val scope = rememberCoroutineScope()
            // since it's impossible to invoke reset inside of confirmValueChange, do that in LaunchedEffect
            LaunchedEffect(swipeToDismissBoxState.currentValue) {
                if (swipeToDismissBoxState.currentValue != SwipeToDismissBoxValue.Settled) {
                        swipeToDismissBoxState.reset()
                }
            }
            MoneyMonocleTheme(darkTheme = isThemeDark.value) {
                Surface(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .imePadding()
                    ) {
                        MoneyMonocleNavHost(
                            onError = {
                                error = it
                                if (!isSnackbarShown) {
                                    isSnackbarShown = true
                                    scope.launch {
                                        snackbarHostState.showSnackbar(it)
                                        isSnackbarShown = false
                                    }
                                }
                            }
                        )
                        CustomErrorSnackbar(snackbarHostState = snackbarHostState,
                            swipeToDismissBoxState = swipeToDismissBoxState)
                    }
                }

            }
        }
    }
}
