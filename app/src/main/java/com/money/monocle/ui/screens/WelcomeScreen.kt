package com.money.monocle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(100.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Text(text = "Welcome to Money Monocle")

        Column(
            verticalArrangement = Arrangement.spacedBy(30.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("To start off, specify your current balance")
        }
    }
}