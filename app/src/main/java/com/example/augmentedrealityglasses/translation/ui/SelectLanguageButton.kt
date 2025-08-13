package com.example.augmentedrealityglasses.translation.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.augmentedrealityglasses.translation.TranslationViewModel
import com.google.mlkit.nl.translate.TranslateLanguage
import java.util.Locale

@Composable
fun SelectLanguageButton(enabled: Boolean, viewModel: TranslationViewModel, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val uiState = viewModel.uiState

    Button(
        onClick = { expanded = true },
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
        val textContent: String = if (uiState.targetLanguage == null) {
            "Select Language"
        } else {
            getFullLengthName(uiState.targetLanguage)
        }
        Text(
            textContent,
            color = Color.Black,
            softWrap = true,
            maxLines = 2,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        )

    }
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
                    viewModel.selectTargetLanguage(languageTag)
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


