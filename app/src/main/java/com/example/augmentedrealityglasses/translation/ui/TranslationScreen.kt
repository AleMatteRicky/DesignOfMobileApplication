package com.example.augmentedrealityglasses.translation.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.augmentedrealityglasses.translation.TranslationViewModel

@SuppressLint("MissingPermission")
@Composable
fun TranslationScreen(
    onNavigateToHome: () -> Unit,
    viewModel: TranslationViewModel,
    enabled: Boolean
) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        var recordingButtonText by remember { mutableStateOf("Record") }
        Button(onClick = {
            if (viewModel.uiState.isRecording) {
                viewModel.stopRecording()
                recordingButtonText = "Record"
            } else {
                viewModel.startRecording()
                recordingButtonText = "Stop Recording"
            }
        }, modifier = Modifier.align(Alignment.Center), enabled = enabled) {
            Text(recordingButtonText)
        }

        Text(
            text = viewModel.uiState.recognizedText,
            modifier = Modifier.align(Alignment.TopStart)
        )
        Text(
            text = viewModel.uiState.translatedText,
            modifier = Modifier.align(Alignment.BottomStart)
        )
        Box(
            modifier = Modifier.offset(x = 0.dp, y = 150.dp),
            contentAlignment = Alignment.Center
        ) {
            SelectLanguageButton(enabled, viewModel)
        }

        Button(
            onClick = { viewModel.translate() },
            modifier = Modifier.offset(x = 0.dp, y = 105.dp), enabled = enabled
        ) {
            Text("Translate")
        }
    }

    /*
    todo, check if the device has a microphone otherwise disable the feature

 */

}