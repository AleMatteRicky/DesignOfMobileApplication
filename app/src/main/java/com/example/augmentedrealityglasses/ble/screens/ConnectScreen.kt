package com.example.augmentedrealityglasses.ble.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.augmentedrealityglasses.ScreenName
import com.example.augmentedrealityglasses.ble.viewmodels.ConnectViewModel

private val TAG = "ConnectScreen"

@Composable
fun ConnectScreen(
    viewModel: ConnectViewModel,
    onNavigateToFeature: (String) -> Unit,
    onNavigateAfterClosingTheConnection: () -> Unit
) {
    Log.d(TAG, "Recomposing the connect screen")
    Box(Modifier.fillMaxSize()) {

        if (!viewModel.uiState.isConnected) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }

        // TODO. Make the UI better
        if (viewModel.uiState.isConnected) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        onNavigateToFeature(ScreenName.WEATHER_SCREEN.name)
                    }
                ) {
                    Text(text = ScreenName.WEATHER_SCREEN.name)
                }
                Button(
                    onClick = {
                        onNavigateToFeature(ScreenName.TRANSLATION_SCREEN.name)
                    }
                ) {
                    Text(text = ScreenName.TRANSLATION_SCREEN.name)
                }

                Text(text = "Received: ${viewModel.uiState.msg}", fontWeight = FontWeight.Bold)
                Button(
                    onClick = {
                        viewModel.closeConnection()
                        onNavigateAfterClosingTheConnection()
                    }
                ) {
                    Text(text = "Close connection")
                }
                Button(
                    onClick = {
                        viewModel.sendData("")//max = 13 char per message
                    }
                ) {
                    Text("send data")
                }

            }
        }
    }
}
