package com.money.monocle

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.money.monocle.ui.presentation.MoneyMonocleNavHostViewModel
import com.money.monocle.ui.screens.auth.AuthScreen
import com.money.monocle.ui.screens.history.TransactionHistoryScreen
import com.money.monocle.ui.screens.home.AddRecordScreen
import com.money.monocle.ui.screens.home.HomeScreen

sealed class Screens(val route: String) {
    data object Auth: Screens("Auth")
    data object Home: Screens("Home")
    data object AddRecord: Screens("AddRecord")
    data object TransactionHistory: Screens("TransactionHistory")
}

@Composable
fun MoneyMonocleNavHost(
    onError: (String) -> Unit,
    navController: NavHostController = rememberNavController(),
    viewModel: MoneyMonocleNavHostViewModel = hiltViewModel()
) {
    val isUserNull by viewModel.isUserNullFlow.collectAsState(initial = false)
    val startScreen by rememberSaveable {
        mutableStateOf(if (viewModel.currentUser == null) Screens.Auth.route
        else Screens.Home.route)
    }
    LaunchedEffect(isUserNull) {
        // check destination to account for a case where start destination is already Auth
        if (isUserNull && navController.currentDestination?.route != Screens.Auth.route) {
            navController.navigate(Screens.Auth.route) {
                popUpTo(0)
            }
        }
    }
    NavHost(navController = navController,
        startDestination = startScreen,
        enterTransition = { slideInVertically { it } },
        exitTransition = { slideOutVertically { it } } ) {
            composable(Screens.Auth.route,
                enterTransition = { slideInVertically { it }}) {
                AuthScreen(onSignIn = {
                    navController.navigate(Screens.Home.route) {
                        popUpTo(0)
                    }
                }, onError = onError)
            }
            composable(Screens.Home.route,
                enterTransition = { EnterTransition.None }) {
                HomeScreen(
                    onNavigateToAddRecord = {currency, isExpense ->
                       navController.navigate("${Screens.AddRecord.route}/$currency/$isExpense") {
                           launchSingleTop = true
                       }
                    },
                    onNavigateToHistory = {currency ->
                       navController.navigate("${Screens.TransactionHistory.route}/$currency") {
                           launchSingleTop = true
                       }
                    },
                    onError = onError)
            }
            composable("${Screens.TransactionHistory.route}/{currency}",
                arguments = listOf(navArgument("currency") {type = NavType.StringType})
            ) { backStackEntry ->
                TransactionHistoryScreen(
                    currency = backStackEntry.arguments?.getString("currency")!!,
                    onError = onError,
                    onBackClick = {navController.navigateUp() })
            }
            composable("${Screens.AddRecord.route}/{currency}/{isExpense}",
                arguments = listOf(
                    navArgument("currency") {type = NavType.StringType},
                    navArgument("isExpense") {type = NavType.BoolType}),
                enterTransition = {slideInVertically(animationSpec = tween(400)) { it }},
                exitTransition = { slideOutVertically { it } }) {backStackEntry ->
                val arguments = backStackEntry.arguments
                AddRecordScreen(
                    onNavigateBack = { navController.navigateUp() },
                    currency = arguments?.getString("currency")!!,
                    isExpense = arguments.getBoolean("isExpense"))
            }
    }
}