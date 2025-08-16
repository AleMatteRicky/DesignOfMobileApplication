package com.example.augmentedrealityglasses.translation.ui

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.Dialog
import com.example.augmentedrealityglasses.translation.TranslationViewModel


//TODO fix if the user goes back with gesture during recording, the following screen does not have the navbar visible

@SuppressLint("MissingPermission")
@Composable
fun TranslationHomeScreen(
    onNavigateToHome: () -> Unit,
    viewModel: TranslationViewModel,
    enabled: Boolean,
    navigationBarVisible: MutableState<Boolean>,
    navigationBarHeight: Dp,
    onNavigateToResult: () -> Unit
) {

    val uiState = viewModel.uiState

    BoxWithConstraints(
        //todo check if it does support vertical scrolling
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
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



        Text(
            text = uiState.recognizedText,
            modifier = Modifier.align(Alignment.TopStart)
        )
        Text(
            text = uiState.translatedText,
            modifier = Modifier.align(Alignment.BottomStart)
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
                .width(languageSelectionBoxWidth)
        )

        val mainTextBoxHeight = maxHeight - newMaxHeight - 15.dp

        newMaxHeight = newMaxHeight + mainTextBoxHeight + 15.dp
        MainTextBox(
            viewModel,
            Modifier
                .height(mainTextBoxHeight)
        )

//        Button(
//            onClick = { viewModel.translate() },
//            modifier = Modifier.offset(x = 0.dp, y = 105.dp), enabled = enabled
//        ) {
//            Text("Translate")
//        }

        if (uiState.isDownloadingLanguageModel) {
            DisplayModelDownloading()
        } else {
            if (uiState.isModelNotAvailable) {
                DisplayModelMissing { viewModel.downloadLanguageModel() }
            }
        }

    }

    LaunchedEffect(uiState.isResultReady) {
        if (uiState.isResultReady) {
            onNavigateToResult()
            viewModel.resetResultStatus()//should reset also after recording in the other screen
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