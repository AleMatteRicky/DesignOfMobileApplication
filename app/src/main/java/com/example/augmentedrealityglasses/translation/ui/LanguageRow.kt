package com.example.augmentedrealityglasses.translation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.augmentedrealityglasses.Icon
import com.example.augmentedrealityglasses.translation.TranslationViewModel


@Composable
fun LanguageRow(
    modifier: Modifier,
    viewModel: TranslationViewModel,
    languageTag: String?,
    onBack: () -> Boolean,
    isDownloaded: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()

    val downloadingLanguageTags =
        uiState.currentlyDownloadingLanguageTags.collectAsState()
    var isSelected = false
    var modelMissingAlertVisible by remember { mutableStateOf(false) }

    val selectedLanguage =
        if (uiState.selectingLanguageRole == LanguageRole.TARGET) uiState.targetLanguage else uiState.sourceLanguage
    if (languageTag.equals(selectedLanguage)) {
        isSelected = true
    }

    var isLanguageDownloading = false
    if (downloadingLanguageTags.value.contains(languageTag)) {
        isLanguageDownloading = true
    }

    if (modelMissingAlertVisible && languageTag != null) {
        DisplayModelMissing(
            onClickDownload = {
                viewModel.downloadLanguageModel(languageTag)
                modelMissingAlertVisible = false
            }, resetVisibility = { modelMissingAlertVisible = false }
        )
    }

    Row(
        modifier
            .padding(horizontal = 10.dp)
            .background(
                color = if (isSelected) Color.Black else Color(0xFFFAFAFA),
                shape = RoundedCornerShape(25.dp)
            )
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }) {
                if (!isLanguageDownloading) {
                    isSelected = true
                    if (isDownloaded) {
                        if (uiState.selectingLanguageRole == LanguageRole.TARGET) {
                            viewModel.selectTargetLanguage(languageTag)
                        } else {
                            viewModel.selectSourceLanguage(languageTag)
                        }
                        onBack()
                    } else {
                        modelMissingAlertVisible = true
                    }
                }
            }
            .padding(
                horizontal = 16.dp,
                vertical = 14.dp
            ), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (languageTag != null) getFullLengthName(languageTag) else "-",
            color = if (isSelected) Color.White else Color.Black,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.weight(1f)) //Spacer takes all the space available


        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
            if (languageTag != null) {
                if (!isLanguageDownloading) {
                    val icon =
                        if (isDownloaded) Icon.DOWNLOAD_COMPLETED.getID() else Icon.DOWNLOAD.getID()

                    IconButton(
                        onClick = {
                            if (!isDownloaded) {
                                viewModel.downloadLanguageModel(languageTag)
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = "Download language",
                            tint = if (isSelected) Color.White else Color.Black
                        )
                    }
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(27.dp),
                        strokeWidth = 3.dp,
                        color = Color.Black,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )

                }
            }
        }
    }
}



