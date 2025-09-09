package com.example.augmentedrealityglasses.translation.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.platform.LocalConfiguration
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
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Button(
        onClick = {
            if (!uiState.isRecording) {
                onClick()
            }
        },
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(if (isLandscape) 10.dp else 16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.tertiaryContainer),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = if (isLandscape) 4.dp else 8.dp
        )
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
            color = colorScheme.primary,
            softWrap = true,
            maxLines = 2,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )

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


