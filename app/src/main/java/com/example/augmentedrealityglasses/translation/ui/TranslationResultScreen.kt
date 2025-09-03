package com.example.augmentedrealityglasses.translation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.augmentedrealityglasses.UpdateWrapper
import com.example.augmentedrealityglasses.translation.TranslationViewModel

@Composable
fun TranslationResultScreen(
    viewModel: TranslationViewModel,
    onBack: () -> Boolean,
    onNavigateToLanguageSelection: () -> Unit
) {

    val configuration = LocalConfiguration.current
    val maxHeight = configuration.screenHeightDp.dp
    val maxWidth = configuration.screenWidthDp.dp
    val recordButtonSize = 65.dp
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.errorMessage.collectAsState()

    UpdateWrapper(
        message = message,
        bluetoothUpdateStatus = viewModel.bluetoothUpdateStatus,
        onErrorDismiss = { viewModel.hideErrorMessage() },
        onBluetoothUpdateDismiss = { viewModel.hideBluetoothUpdate() }) {

        Box(Modifier.fillMaxSize()) {

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 8.dp)
                    .zIndex(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onBack() }) {
                    Icon(
                        painter = painterResource(com.example.augmentedrealityglasses.Icon.BACK_ARROW.getID()),
                        contentDescription = "Go back to translation home screen",
                        tint = Color.Black
                    )
                }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(top = 55.dp, start = 24.dp, end = 24.dp)
            ) {
                val sourceLanguage = uiState.sourceLanguage
                ResultTextBox(
                    modifier = Modifier,
                    contentText = uiState.recognizedText,
                    color = Color.Black,
                    language = if (sourceLanguage != null) getFullLengthName(sourceLanguage) else "Select Source Language",
                    onNavigateToLanguageSelection = {
                        viewModel.setSelectingLanguageRole(LanguageRole.SOURCE)
                        onNavigateToLanguageSelection()
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(2.dp)
                            .background(color = Color.Black, shape = RoundedCornerShape(2.dp))
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                val targetLanguage = uiState.targetLanguage

                ResultTextBox(
                    modifier = Modifier.padding(bottom = 50.dp),
                    contentText = uiState.translatedText,
                    color = Color(0xFF0B61A4),
                    language = if (targetLanguage != null) getFullLengthName(targetLanguage) else "Select Target Language",
                    onNavigateToLanguageSelection = {
                        viewModel.setSelectingLanguageRole(LanguageRole.TARGET)
                        onNavigateToLanguageSelection()
                    }
                )

            }

            RecordButton(
                true,
                viewModel,
                Modifier
                    .offset(
                        x = (maxWidth - recordButtonSize * 1.1f) / 2,
                        y = maxHeight - recordButtonSize * 1.2f - 30.dp
                    ), recordButtonSize,
                navigationBarVisible = null
            )

            if (uiState.isDownloadingSourceLanguageModel && uiState.isDownloadingTargetLanguageModel) {
                DisplayModelDownloading("Downloading the detected source language model and the target language model")
            } else if (uiState.isDownloadingSourceLanguageModel) {
                DisplayModelDownloading("Downloading the detected source language model")
            } else if (uiState.isDownloadingTargetLanguageModel) {
                DisplayModelDownloading("Downloading the target language model")
            }

        }

        if (uiState.isResultReady) {
            viewModel.resetResultStatus() //isResultReady is only used to switch screen  when the recording starts in translation home
        }

        BackHandler {
            if (uiState.isRecording) {
                viewModel.stopRecording()
            }
            onBack()
        }
    }
}