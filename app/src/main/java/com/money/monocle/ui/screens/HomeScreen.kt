package com.money.monocle.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.money.monocle.domain.Result
import com.money.monocle.ui.LoadScreen
import com.money.monocle.ui.presentation.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    when (uiState.dataFetchResult) {
        is Result.Error -> {
            if (uiState.showWelcomeScreen) {
                WelcomeScreen()
            }
        }
        is Result.Success -> {
            MainContent()
        }
        is Result.InProgress -> {
            LoadScreen()
        }
        else -> {}
    }
}

@Composable
fun MainContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Hello",
            fontSize = 40.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}