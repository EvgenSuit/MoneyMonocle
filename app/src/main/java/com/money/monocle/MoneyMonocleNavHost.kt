package com.money.monocle

import android.util.Log
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.money.monocle.ui.presentation.MoneyMonocleNavHostViewModel
import com.money.monocle.ui.screens.AuthScreen
import com.money.monocle.ui.screens.HomeScreen

private sealed class Screens(val route: String) {
    data object Auth: Screens("Auth")
    data object Home: Screens("Home")
    data object WelcomeScreen: Screens("WelcomeScreen")
}

@Composable
fun MoneyMonocleNavHost(
    onError: (String) -> Unit,
    navController: NavHostController = rememberNavController(),
    viewModel: MoneyMonocleNavHostViewModel = hiltViewModel()
) {
    val isUserNull by viewModel.isUserNullFlow.collectAsState(initial = false)
    LaunchedEffect(isUserNull) {
        if (isUserNull) {
            navController.navigate(Screens.Auth.route) {
                launchSingleTop = true
            }
        }
    }
    LaunchedEffect(Unit) {
        Firebase.auth.signOut()
    }
    Log.d("User", Firebase.auth.currentUser.toString())
    val startScreen by rememberSaveable {
        mutableStateOf(if (Firebase.auth.currentUser == null) Screens.Auth.route
        else Screens.Home.route)
    }
    NavHost(navController = navController,
        startDestination = startScreen,
        exitTransition = { fadeOut(animationSpec = tween(400)) },
        enterTransition = { scaleIn(animationSpec = tween(400),
            initialScale = 0.8f) }) {
            composable(Screens.Auth.route) {
                AuthScreen(onSignIn = {
                    navController.navigate(Screens.Home.route) {
                        popUpTo(0)
                    }
                },
                    onError = onError)
            }
            composable(Screens.Home.route) {
                HomeScreen()
            }
    }
}