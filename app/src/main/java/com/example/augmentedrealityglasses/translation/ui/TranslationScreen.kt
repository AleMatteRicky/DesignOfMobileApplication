package com.example.augmentedrealityglasses.translation.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
        viewModel.uiState
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

        if (viewModel.uiState.isDownloadingLanguageModel) {
            DisplayModelDownloading()
        } else {
            if (viewModel.uiState.isModelNotAvailable) {
                DisplayModelMissing { viewModel.downloadLanguageModel() }
            }
        }

    }

    /*
    todo, check if the device has a microphone otherwise disable the feature
    */

}

@Composable
private fun DisplayModelDownloading() {
    Dialog(onDismissRequest = { /* leaving this function empty avoids that the user close the dialog only by clicking outside it */ }) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .width(IntrinsicSize.Min),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Downloading the model")
                Spacer(modifier = Modifier.height(8.dp)) //could be removed
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun DisplayModelMissing(onClickDownload: () -> Unit) {
    var showDialog by remember { mutableStateOf(true) }
    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .width(IntrinsicSize.Min),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("This model is not available on the device, download it in order to complete the translation")
                    Spacer(modifier = Modifier.height(8.dp)) //could be removed
                    Button(onClickDownload) {
                        Text("Download")
                    }
                }
            }
        }
    }
}