package com.money.monocle.domain.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.money.monocle.LocalSnackbarController
import com.money.monocle.domain.CustomResult
import com.money.monocle.R

enum class NetworkState {
    CONNECTED,
    DISCONNECTED,
    NONE
}
@Composable
fun NetworkStatus(context: Context) {
    val snackbarController = LocalSnackbarController.current
    var state by remember {
        mutableStateOf(NetworkState.NONE)
    }
    LaunchedEffect(state) {
        if (state == NetworkState.DISCONNECTED) {
            snackbarController.showSnackbar(CustomResult.ResourceError(R.string.no_internet))
        }
    }
    DisposableEffect(Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        state = connectivityManager.getNetworkState()
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                state = connectivityManager.getNetworkState()
            }
            override fun onLost(network: Network) {
                super.onLost(network)
                state = connectivityManager.getNetworkState()
            }
        }
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        onDispose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }
}

fun ConnectivityManager.getNetworkState(): NetworkState {
    val network = this.activeNetwork
    val capabilities = this.getNetworkCapabilities(network)
    val hasInternetCapability = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
    return if (hasInternetCapability) NetworkState.CONNECTED else NetworkState.DISCONNECTED
}