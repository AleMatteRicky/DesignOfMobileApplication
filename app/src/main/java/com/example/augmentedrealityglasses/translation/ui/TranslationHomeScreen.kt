package com.example.augmentedrealityglasses.translation.ui

import android.annotation.SuppressLint
import androidx.compose.runtime.getValue
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.example.augmentedrealityglasses.UpdateWrapper
import com.example.augmentedrealityglasses.translation.TranslationViewModel


@SuppressLint("MissingPermission", "UnusedBoxWithConstraintsScope") //todo remove second suppress
@Composable
fun TranslationHomeScreen(
    onNavigateToHome: () -> Unit,
    viewModel: TranslationViewModel,
    enabled: Boolean,
    navigationBarVisible: MutableState<Boolean>,
    navigationBarHeight: Dp,
    onNavigateToResult: () -> Unit,
    onNavigateToLanguageSelection: () -> Unit,
    onBack: () -> Unit,
    onScreenComposition: () -> Unit
) {

    LaunchedEffect(Unit) {
        onScreenComposition()
    }

    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.errorMessage.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    UpdateWrapper(
        message = message,
        bluetoothUpdateStatus = viewModel.bluetoothUpdateStatus,
        onErrorDismiss = { viewModel.hideErrorMessage() },
        onBluetoothUpdateDismiss = { viewModel.hideBluetoothUpdate() }) {
        BoxWithConstraints(
            //todo check if it does support vertical scrolling
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
        ) {
            val recordButtonSize = 65.dp
            var newMaxHeight = recordButtonSize * 1.2f + 10.dp

            RecordButton(
                enabled,
                viewModel,
                Modifier
                    .offset(
                        x = (maxWidth - recordButtonSize * 1.1f) / 2,
                        y = maxHeight - newMaxHeight
                    ), recordButtonSize,
                navigationBarVisible = navigationBarVisible
            )

            val languageSelectionBoxWidth = 0.8 * maxWidth //fills 80% of the parent width
            val languageSelectionBoxHeight =
                0.07 * (maxHeight + navigationBarHeight) //fills 7% of the parent width
            newMaxHeight = newMaxHeight + languageSelectionBoxHeight + 20.dp

            LanguageSelectionBox(
                enabled, viewModel, modifier = Modifier
                    .offset(
                        x = (maxWidth - languageSelectionBoxWidth) / 2,
                        y = maxHeight - newMaxHeight
                    )
                    .height(languageSelectionBoxHeight)
                    .width(languageSelectionBoxWidth), onClick = onNavigateToLanguageSelection
            )

            val mainTextBoxHeight = maxHeight - newMaxHeight - 15.dp

            newMaxHeight = newMaxHeight + mainTextBoxHeight + 15.dp
            MainTextBox(
                viewModel,
                Modifier
                    .height(mainTextBoxHeight)
            )

            if (uiState.isDownloadingSourceLanguageModel && uiState.isDownloadingTargetLanguageModel) {
                DisplayModelDownloading("Downloading the detected source language model and the target language model")
            } else if (uiState.isDownloadingSourceLanguageModel) {
                DisplayModelDownloading("Downloading the detected source language model")
            } else if (uiState.isDownloadingTargetLanguageModel) {
                DisplayModelDownloading("Downloading the target language model")
            }

        }

        LaunchedEffect(uiState.isResultReady) {
            if (uiState.isResultReady) {
                onNavigateToResult()
                viewModel.resetResultStatus()//should reset also after recording in the other screen
            }
        }

        BackHandler(uiState.isRecording) {
            viewModel.stopRecording()
            onBack()
        }

        /*
    todo, check if the device has a microphone otherwise disable the feature
    */

    }
}

