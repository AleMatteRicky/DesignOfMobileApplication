package com.example.augmentedrealityglasses.translation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.augmentedrealityglasses.Icon
import com.example.augmentedrealityglasses.UpdateWrapper
import com.example.augmentedrealityglasses.translation.TranslationViewModel
import com.example.augmentedrealityglasses.translation.ui.LanguageRole
import com.example.augmentedrealityglasses.translation.ui.LanguageRow

//todo check if with animation is solved, selecting a target downloaded language and then switching immediately to recording lead to show for an instant DisplayModelMissing
//todo before starting to record verify that the source language is not null
//todo disable offline
//todo verify tag compatibility


@Composable
fun TranslationLanguageSelectionScreen(viewModel: TranslationViewModel, onBack: () -> Boolean) {

    val message by viewModel.errorMessage.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val downloadedLanguageTags by uiState.downloadedLanguageTags.collectAsState()
    val notDownloadedLanguageTags by uiState.notDownloadedLanguageTags.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    UpdateWrapper(
        message = message,
        bluetoothUpdateStatus = viewModel.bluetoothUpdateStatus,
        onErrorDismiss = { viewModel.hideErrorMessage() },
        onBluetoothUpdateDismiss = { viewModel.hideBluetoothUpdate() }) {

        Column(Modifier.fillMaxSize().background(colorScheme.onBackground)) {

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 8.dp)
                    .zIndex(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onBack() }) {
                    Icon(
                        painter = painterResource(Icon.BACK_ARROW.getID()),
                        contentDescription = "Go back to translation home screen",
                        tint = colorScheme.primary
                    )
                }

                Text(
                    text = "Select " + (if (uiState.selectingLanguageRole == LanguageRole.TARGET) "target" else "source") + " language",
                    color = colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp)
                )


            }


            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {

                item {
                    Text(
                        text = "Downloaded Languages",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        color = colorScheme.primary
                    )
                }

                item {
                    LanguageRow(Modifier, viewModel, null, onBack, true)
                }

                items(downloadedLanguageTags) { tag ->
                    LanguageRow(Modifier, viewModel, tag, onBack, true)
                }

                item {
                    Text(
                        text = "All Languages",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        color = colorScheme.primary
                    )
                }

                items(notDownloadedLanguageTags) { tag ->
                    LanguageRow(Modifier, viewModel, tag, onBack, false)
                }

            }
        }
    }
}