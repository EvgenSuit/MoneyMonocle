package com.money.monocle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.money.monocle.ui.presentation.MoneyMonocleNavHostViewModel
import com.money.monocle.ui.screens.auth.AuthScreen
import com.money.monocle.ui.screens.history.TransactionHistoryScreen
import com.money.monocle.ui.screens.record.AddRecordScreen
import com.money.monocle.ui.screens.home.HomeScreen
import com.money.monocle.ui.screens.record.AddCategoryScreen
import com.money.monocle.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String, val name: Int = 0) {
    data object Auth: Screen("Auth")
    data object Home: Screen("Home", R.string.home)
    data object Settings: Screen("Settings", R.string.settings)
    data object AddRecord: Screen("AddRecord")
    data object AddCategory: Screen("AddCategory")
    data object TransactionHistory: Screen("TransactionHistory")
}
private val bottomBarScreens = listOf(Screen.Home, Screen.Settings)

@Composable
fun MoneyMonocleNavHost(
    navController: NavHostController = rememberNavController(),
    viewModel: MoneyMonocleNavHostViewModel = hiltViewModel()
) {
    val isUserNull by viewModel.isUserNullFlow.collectAsState(initial = false)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isAccountLoaded by viewModel.isAccountLoadedFlow.collectAsState(initial = false)
    val isWelcomeScreenShown by viewModel.isWelcomeScreenShownFlow.collectAsState(initial = false)
    val startScreen by rememberSaveable {
        mutableStateOf(if (viewModel.currentUser == null) Screen.Auth.route
        else Screen.Home.route)
    }
    val showBottomNavBar by remember(backStackEntry, isAccountLoaded, isWelcomeScreenShown) {
        mutableStateOf( isAccountLoaded && !isWelcomeScreenShown &&
                listOf(Screen.Home.route, Screen.Settings.route).contains(backStackEntry?.destination?.route))
    }
    LaunchedEffect(isUserNull) {
        // check destination to account for a case where start destination is already Auth
        if (isUserNull && navController.currentDestination?.route != Screen.Auth.route) {
            navController.navigate(Screen.Auth.route) {
                popUpTo(0)
            }
        }
    }
    Scaffold(
        bottomBar = {
            AnimatedVisibility(showBottomNavBar,
                enter = fadeIn(),
                exit = ExitTransition.None
            ) {
                CustomBottomNavBar(selectedScreen = bottomBarScreens.firstOrNull { it.route == backStackEntry?.destination?.route },
                    onNavigate = {route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            restoreState = true
                            launchSingleTop = true
                        }
                    })
            }
        }
    ) {padding ->
        NavHost(navController = navController,
            startDestination = startScreen,
            enterTransition = { slideInVertically { it } },
            exitTransition = { fadeOut(animationSpec = tween(200)) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)) {
            composable(Screen.Auth.route, exitTransition = {ExitTransition.None}) {
                AuthScreen(onSignIn = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0)
                    }
                })
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToAddRecord = {currency, isExpense ->
                        navController.navigate("${Screen.AddRecord.route}/$currency/$isExpense") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToHistory = {currency ->
                        navController.navigate("${Screen.TransactionHistory.route}/$currency") {
                            launchSingleTop = true
                        }
                    })
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable("${Screen.TransactionHistory.route}/{currency}",
                arguments = listOf(navArgument("currency") {type = NavType.StringType})
            ) {
                TransactionHistoryScreen(
                    onBackClick = {navController.navigateUp() })
            }
            composable("${Screen.AddRecord.route}/{currency}/{isExpense}",
                arguments = listOf(
                    navArgument("currency") {type = NavType.StringType},
                    navArgument("isExpense") {type = NavType.BoolType})) {
                AddRecordScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onAddCategory = { isExpense ->
                        navController.navigate("${Screen.AddCategory}/$isExpense")
                    })
            }
            composable("${Screen.AddCategory}/{isExpense}",
                arguments = listOf(navArgument("isExpense") { type = NavType.BoolType })
            ) {
                AddCategoryScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }

        }
    }
}

@Composable
fun CustomBottomNavBar(
    selectedScreen: Screen?,
    onNavigate: (String) -> Unit,
) {
    val gradient = Brush.verticalGradient(colors = listOf(
        MaterialTheme.colorScheme.primary.copy(0.05f),
        MaterialTheme.colorScheme.background.copy(1f)
    ))
    val selectedIndex = bottomBarScreens.indexOf(selectedScreen)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient)
            .testTag("BottomNavBar")
    ) {
        TabRow(selectedTabIndex = selectedIndex,
            containerColor = Color.Transparent,
            indicator = {tabPos ->
                if (selectedIndex < tabPos.size && bottomBarScreens.contains(selectedScreen)) {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPos[selectedIndex])
                    )
                }
            },
            divider = {
                HorizontalDivider(color = Color.Transparent)
            },
            modifier = Modifier.padding(10.dp)) {
            for (screen in bottomBarScreens) {
                Tab(selected = selectedScreen == screen,
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onBackground,
                    text = { Text(
                        stringResource(id = screen.name),
                        style = MaterialTheme.typography.labelSmall) },
                    onClick = { onNavigate(screen.route) },
                    modifier = Modifier.testTag(screen.route))
            }
        }
    }
}