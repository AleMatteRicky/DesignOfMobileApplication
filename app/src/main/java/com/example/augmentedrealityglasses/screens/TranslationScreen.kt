package com.example.augmentedrealityglasses.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TranslationScreen(
    translationViewModel: TranslationViewModel,
    onSendingData: (String) -> Unit
) {

    // TODO. add logic to handle connection state changes
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // todo why does the system not notifying me about possible null pointer reference?
            if (translationViewModel.uiState.isSending) {
                Text(
                    text = "Sending message ${translationViewModel.uiState.msg} ..."
                )
            }
            val msg = "Hello world"
            Button(
                enabled = !translationViewModel.uiState.isSending,
                onClick = {
                    onSendingData(msg)
                }
            ) {
                Text(text = "Sent message: $msg")
            }
        }
    }
}