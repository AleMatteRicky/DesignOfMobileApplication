package com.example.augmentedrealityglasses.translation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.augmentedrealityglasses.ui.theme.Icon
import com.example.augmentedrealityglasses.translation.TranslationViewModel

@Composable
fun LanguageSelectionBox(
    enabled: Boolean,
    viewModel: TranslationViewModel,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        SelectLanguageButton(
            enabled = enabled,
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            onClick = {
                viewModel.setSelectingLanguageRole(LanguageRole.SOURCE)
                onClick()
            },
            languageRole = LanguageRole.SOURCE
        )

        Spacer(Modifier.width(16.dp))

        Icon(
            painter = painterResource(id = Icon.RIGHT_ARROW.getID()),
            contentDescription = "An arrow from the source language to the target language",
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.width(16.dp))

        SelectLanguageButton(
            enabled = enabled,
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            onClick = {
                viewModel.setSelectingLanguageRole(LanguageRole.TARGET)
                onClick()
            },
            languageRole = LanguageRole.TARGET
        )
    }
}