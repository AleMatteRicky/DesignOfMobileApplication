package com.example.augmentedrealityglasses.translation.ui

import android.view.textclassifier.TextLanguage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.augmentedrealityglasses.translation.TranslationViewModel

@Composable
fun ResultTextBox(
    modifier: Modifier,
    language: String,
    contentText: String
) { //todo add selection language screen if target
    Column(modifier = modifier.fillMaxWidth()) {

        Text(
            text = language,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = contentText,
            softWrap = true,
            fontSize = 30.sp,
            lineHeight = 38.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}