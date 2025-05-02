package com.example.augmentedrealityglasses.translation.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.augmentedrealityglasses.translation.TranslationViewModel
import com.google.mlkit.nl.translate.TranslateLanguage
import java.util.Locale

@Composable
fun SelectLanguageButton(enabled: Boolean, viewModel: TranslationViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val uiState = viewModel.uiState

    Button(
        onClick = { expanded = true },
        enabled = enabled,
        modifier = Modifier.size(
            width = 160.dp,
            height = 40.dp
        )
    ) {
        if (uiState.targetLanguage == null) {
            Text("Select Language")
        } else {
            Text(getFullLengthName(uiState.targetLanguage))
        }

    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        for (language in TranslateLanguage.getAllLanguages()) {
            DropdownMenuItem(
                text = { Text(getFullLengthName(language)) },
                onClick = {
                    viewModel.selectTargetLanguage(language)
                    expanded = false
                }
            )
        }

    }
}

private fun getFullLengthName(tag: String): String {
    val locale = Locale.forLanguageTag(tag)

    return locale.displayName
}


