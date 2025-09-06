package com.example.augmentedrealityglasses.translation.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.example.augmentedrealityglasses.Icon
import com.example.augmentedrealityglasses.internet.ConnectivityStatus
import com.example.augmentedrealityglasses.translation.TranslationViewModel

@SuppressLint("MissingPermission")
@Composable
fun RecordButton(
    enabled: Boolean,
    viewModel: TranslationViewModel,
    modifier: Modifier,
    size: Dp,
    navigationBarVisible: MutableState<Boolean>?
) {

    val uiState by viewModel.uiState.collectAsState()
    var isButtonActive by remember { mutableStateOf(true) }
    var recordingSymbol by remember { mutableStateOf(Icon.MICROPHONE) }
    val colorScheme = MaterialTheme.colorScheme

    val notValidSourceLanguage = uiState.sourceLanguage.isNullOrBlank()
    val notValidInternetConnection =
        viewModel.internetConnectionManager.status != ConnectivityStatus.ValidatedInternet
    val microphoneNotAvailable =
        !viewModel.isMicrophoneAvailable()

    isButtonActive =
        !(notValidSourceLanguage || notValidInternetConnection || microphoneNotAvailable) //add missing permission check

    Box(modifier, contentAlignment = Alignment.Center) {
        var waveSoundRippleEffectVisible by remember { mutableStateOf(false) }
        WaveSoundRippleEffect(
            Modifier
                .size(size * 1.1f)
                .alpha(if (waveSoundRippleEffectVisible) 1f else 0f), viewModel
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    color = if (enabled && isButtonActive) colorScheme.onSurface else colorScheme.secondary
                )
                .size(size)
                .clickable(onClick = {
                    if (enabled) { // add an asking for permissions message
                        if (isButtonActive) {
                            if (uiState.isRecording) {
                                viewModel.stopRecording()
                            } else {
                                viewModel.startRecording()
                            }
                        } else {
                            val message = when {
                                microphoneNotAvailable -> "A microphone is required to use this feature."
                                notValidInternetConnection -> "This feature is unavailable offline. Please connect to the internet and try again."
                                notValidSourceLanguage -> "Please specify a source language before starting audio recording."
                                else -> "An unexpected error occurred. Please try again."
                            }
                            viewModel.errorMessage.value = message
                        }
                    }
                })
        ) {

            if (uiState.isRecording) {
                recordingSymbol = Icon.STOP
                if (navigationBarVisible != null) {
                    navigationBarVisible.value = false
                }
                waveSoundRippleEffectVisible = true
            } else {
                recordingSymbol = Icon.MICROPHONE
                if (navigationBarVisible != null) {
                    navigationBarVisible.value = true
                }
                waveSoundRippleEffectVisible = false
            }

            Icon(
                painter = painterResource(id = recordingSymbol.getID()),
                contentDescription = "Recording status icon",
                tint = colorScheme.inversePrimary //todo check why it does not work
            )
        }
    }
}
