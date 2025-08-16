package com.example.augmentedrealityglasses.translation.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.augmentedrealityglasses.translation.TranslationViewModel

@Composable
fun TranslationResultScreen(viewModel: TranslationViewModel, onBackClick: () -> Boolean) {

    val configuration = LocalConfiguration.current
    val maxHeight = configuration.screenHeightDp.dp
    val maxWidth = configuration.screenWidthDp.dp
    val recordButtonSize = 65.dp
    val uiState = viewModel.uiState

    Box(Modifier.fillMaxSize()) {

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, start = 8.dp)
                .zIndex(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBackClick() }) {
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
                .padding(top = 28.dp, start = 24.dp, end = 24.dp)
        ) {
            ResultTextBox(
                modifier = Modifier,
                contentText = uiState.recognizedText,
                language = ""
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

            if (uiState.targetLanguage != null) {
                ResultTextBox(
                    modifier = Modifier,
                    contentText = uiState.translatedText,
                    language = getFullLengthName(uiState.targetLanguage)
                )
            }
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

    }

    if (uiState.isResultReady) {
        viewModel.resetResultStatus() //isResultReady is only used to switch screen  when the recording starts in translation home
    }
}