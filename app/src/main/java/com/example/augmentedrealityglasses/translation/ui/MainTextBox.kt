package com.example.augmentedrealityglasses.translation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.augmentedrealityglasses.translation.TranslationViewModel

@Composable
fun MainTextBox(viewModel: TranslationViewModel, modifier: Modifier) {

    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val contentText = uiState.recognizedText
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxWidth()
//            .shadow(
//                elevation = 4.dp,
//                shape = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp),
//                clip = false
//            )
            .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
            .background(colorScheme.tertiaryContainer)
            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            if (contentText.isEmpty()) {
                Text(
                    text = "Record text",
                    color = colorScheme.secondary,
                    style = MaterialTheme.typography.titleSmall
                )
            } else {
                Text(
                    text = contentText,
                    color = colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }

    LaunchedEffect(contentText) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

}