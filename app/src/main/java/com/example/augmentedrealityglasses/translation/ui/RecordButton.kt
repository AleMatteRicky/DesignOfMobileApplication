package com.example.augmentedrealityglasses.translation.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.example.augmentedrealityglasses.Icon
import com.example.augmentedrealityglasses.translation.TranslationViewModel

@SuppressLint("MissingPermission")
@Composable
fun RecordButton(
    enabled: Boolean,
    viewModel: TranslationViewModel,
    modifier: Modifier,
    size: Dp,
    navigationBarVisible: MutableState<Boolean>
) {

    var recordingSymbol by remember { mutableStateOf(Icon.MICROPHONE) }

    Box(modifier, contentAlignment = Alignment.Center) {
        var waveSoundRippleEffectVisible by remember { mutableStateOf(false) }
        WaveSoundRippleEffect(Modifier.size(size * 1.1f).alpha(if (waveSoundRippleEffectVisible) 1f else 0f), viewModel)

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    color = if (enabled) Color.Black else Color.Gray
                )
                .size(size)
                .clickable(onClick = {
                    if (enabled) { // add an asking for permissions message
                        if (viewModel.uiState.isRecording) {
                            viewModel.stopRecording()
                        } else {
                            viewModel.startRecording()
                        }
                    }
                })
        ) {

            if (viewModel.uiState.isRecording) {
                recordingSymbol = Icon.STOP
                navigationBarVisible.value = false
                waveSoundRippleEffectVisible = true
            } else {
                recordingSymbol = Icon.MICROPHONE
                navigationBarVisible.value = true
                waveSoundRippleEffectVisible = false
            }

            Image(
                painter = painterResource(id = recordingSymbol.getID()),
                contentDescription = "Recording status icon"
                //modifier = Modifier.size(24.dp)
            )
        }
    }
}
