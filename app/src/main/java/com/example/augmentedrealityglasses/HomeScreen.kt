package com.example.augmentedrealityglasses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import okhttp3.Connection

@Composable
fun HomeScreen(
    onNavigateToTranslation: () -> Unit,
    onNavigateToWeather: () -> Unit,
    onNavigateToBLE: () -> Unit,
    onNavigateToConnect: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = {
            onNavigateToTranslation()
        }) {
            Text("Translation Screen")
        }
        Button(
            onClick = {
                onNavigateToWeather()
            },
        ) {
            Text(text = "Weather")
        }
        Button(
            onClick = {
                onNavigateToBLE()
            },
        ) {
            Text(text = "Ble")
        }
        Button(
            onClick = {
                onNavigateToConnect()
            }
        ) {
            Text("Connect Screen")
        }
    }
}
