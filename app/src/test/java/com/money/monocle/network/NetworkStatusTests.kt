package com.money.monocle.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.money.monocle.R
import com.money.monocle.assertSnackbarIsNotDisplayed
import com.money.monocle.assertSnackbarTextEquals
import com.money.monocle.domain.network.NetworkState
import com.money.monocle.domain.network.NetworkStatus
import com.money.monocle.getString
import com.money.monocle.setContentWithSnackbar
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NetworkStatusTests {
    @get: Rule
    val composeRule = createComposeRule()
    private val snackbarScope = TestScope()
    private lateinit var context: Context

    @Before
    fun setup() {
        mockConnectivityManager(NetworkState.NONE)
    }
    private fun mockConnectivityManager(state: NetworkState) {
        val mockedConnectivityManager = mockk<ConnectivityManager>(relaxed = true) {
            every { getNetworkCapabilities(any())?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns (state == NetworkState.CONNECTED)
        }
        context = mockk<Context> {
            every { getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager } returns mockedConnectivityManager
        }
    }
    @Test
    fun onNoInternet_errorSnackbarShown() {
        mockConnectivityManager(NetworkState.DISCONNECTED)
        composeRule.apply {
            setContentWithSnackbar(snackbarScope) { NetworkStatus(context) }
            assertSnackbarTextEquals(snackbarScope, getString(R.string.no_internet))
        }

    }
    @Test
    fun onInternet_errorSnackbarNotShown() {
        mockConnectivityManager(NetworkState.CONNECTED)
        composeRule.apply {
            setContentWithSnackbar(snackbarScope) { NetworkStatus(context) }
            assertSnackbarIsNotDisplayed(snackbarScope)
        }

    }
}