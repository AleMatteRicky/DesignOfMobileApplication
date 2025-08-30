package com.example.augmentedrealityglasses.translation.ui

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.augmentedrealityglasses.translation.TranslationViewModel

//todo check if with animation is solved, selecting a target downloaded language and then switching immediately to recording lead to show for an instant DisplayModelMissing
//todo before starting to record verify that the source language is not null
//todo disable offline
//todo verify tag compatibility


@Composable
fun TranslationLanguageSelectionScreen(viewModel: TranslationViewModel, onBack: () -> Boolean) {

    Column(Modifier.fillMaxSize()) {

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

            Text(
                text = "Select " + (if (viewModel.uiState.selectingLanguageRole == LanguageRole.TARGET) "target" else "source") + " language",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )


        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            item {
                Text(
                    text = "Downloaded Languages",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }

            item {
                LanguageRow(Modifier, viewModel, null, onBack, true)
            }

            items(viewModel.uiState.downloadedLanguageTags) { tag ->
                LanguageRow(Modifier, viewModel, tag, onBack, true)
            }

            item {
                Text(
                    text = "All Languages",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }

            items(viewModel.uiState.notDownloadedLanguageTags) { tag ->
                LanguageRow(Modifier, viewModel, tag, onBack, false)
            }

        }
    }
}