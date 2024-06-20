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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.money.monocle.domain.datastore.DataStoreManager
import com.money.monocle.domain.network.NetworkStatus
import com.money.monocle.ui.screens.components.CustomErrorSnackbar
import com.money.monocle.ui.screens.components.SnackbarController
import com.money.monocle.ui.theme.MoneyMonocleTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

val LocalSnackbarController = compositionLocalOf<SnackbarController> {
    error("No snackbar host state provided")
}
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val isThemeDark = mutableStateOf(true)
    private val isAccountLoaded = mutableStateOf(false)
    private val isWelcomeScreenShown = mutableStateOf(false)
    private val currentRoute = mutableStateOf<String?>(null)
    @Inject
    lateinit var dataStoreManager: DataStoreManager

    private fun collectThemeMode() {
        lifecycleScope.launch {
            dataStoreManager.themeFlow().collectLatest {
                isThemeDark.value = it
            }
        }
    }

    private fun collectAccountState() = lifecycleScope.launch {
        dataStoreManager.accountStateFlow().collectLatest {
            isAccountLoaded.value = it
        }
    }
    private fun collectIsWelcomeScreenShown() = lifecycleScope.launch {
        dataStoreManager.isWelcomeScreenShownFlow().collectLatest {
            isWelcomeScreenShown.value = it
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().setKeepOnScreenCondition {
            !isAccountLoaded.value && currentRoute.value != Screen.Auth.route
        }
        runBlocking {
            dataStoreManager.changeAccountState(false)
            dataStoreManager.isWelcomeScreenShown(false)
        }
        collectThemeMode()
        collectIsWelcomeScreenShown()
        collectAccountState()
        setContent {
            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentEntry = backStackEntry?.destination?.route
            LaunchedEffect(currentEntry) {
                currentRoute.value = currentEntry
            }
            val snackbarHostState = remember {
                SnackbarHostState()
            }
            val swipeToDismissBoxState = rememberSwipeToDismissBoxState(confirmValueChange = {value ->
                if (value != SwipeToDismissBoxValue.Settled) {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        true
                } else false
            })
            val snackbarController by remember(snackbarHostState) {
                mutableStateOf(SnackbarController(snackbarHostState, lifecycleScope, applicationContext))
            }
            // since it's impossible to invoke reset inside of confirmValueChange, do that in LaunchedEffect
            LaunchedEffect(swipeToDismissBoxState.currentValue) {
                if (swipeToDismissBoxState.currentValue != SwipeToDismissBoxValue.Settled) {
                        swipeToDismissBoxState.reset()
                }
            }
            MoneyMonocleTheme(darkTheme = isThemeDark.value) {
                CompositionLocalProvider(LocalSnackbarController provides snackbarController) {
                    NetworkStatus(applicationContext)
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
                                navController = navController
                            )
                            CustomErrorSnackbar(snackbarHostState = snackbarHostState,
                                swipeToDismissBoxState = swipeToDismissBoxState)
                        }
                    }
                }

            }
        }
    }
}


