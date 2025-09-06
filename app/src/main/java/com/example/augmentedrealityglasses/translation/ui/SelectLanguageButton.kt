package com.example.augmentedrealityglasses.translation.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.augmentedrealityglasses.translation.TranslationViewModel
import com.google.mlkit.nl.translate.TranslateLanguage
import java.util.Locale

@Composable
fun SelectLanguageButton(
    enabled: Boolean,
    viewModel: TranslationViewModel,
    modifier: Modifier,
    onClick: () -> Unit,
    languageRole: LanguageRole
) {
    var expanded by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    Button(
        onClick = {
            if (!uiState.isRecording) {
                onClick()
            }
        },
        enabled = enabled,
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(14.dp),
                clip = false
            ),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
    ) {
        val buttonLanguage =
            if (languageRole == LanguageRole.TARGET) uiState.targetLanguage else uiState.sourceLanguage

        val textContent: String = if (buttonLanguage == null) {
            "Select Language"
        } else {
            getFullLengthName(buttonLanguage)
        }
        Text(
            textContent,
            color = Color.Black,
            softWrap = true,
            maxLines = 2,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )

    }
    //todo unused deleted

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {

        DropdownMenuItem(
            text = { Text("-") },
            onClick = {
                viewModel.selectTargetLanguage(null)
                expanded = false
            }
        )

        val sortedTagList = TranslateLanguage.getAllLanguages().sortedBy { tag ->
            getFullLengthName(tag)
        }

        for (languageTag in sortedTagList) {
            DropdownMenuItem(
                text = { Text(getFullLengthName(languageTag)) },
                onClick = {
                    if (languageRole == LanguageRole.TARGET) {
                        viewModel.selectTargetLanguage(languageTag)
                    } else {
                        viewModel.selectSourceLanguage(languageTag)
                    }

                    expanded = false
                }
            )
        }
    }
}

fun getFullLengthName(tag: String): String {
    val locale = Locale.forLanguageTag(tag)

    return locale.getDisplayLanguage(Locale.ENGLISH).replaceFirstChar { it.uppercase() }
}

enum class LanguageRole {
    SOURCE,
    TARGET;
}


