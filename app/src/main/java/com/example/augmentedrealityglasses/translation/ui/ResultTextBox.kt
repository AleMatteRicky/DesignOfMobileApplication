package com.example.augmentedrealityglasses.translation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ResultTextBox(
    modifier: Modifier,
    language: String,
    contentText: String,
    color: Color,
    onNavigateToLanguageSelection: () -> Unit
) { //todo add selection language screen if target
    Column(modifier = modifier.fillMaxWidth()) {

        Text(
            text = language,
            color = color,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp),
            modifier = Modifier.padding(bottom = 8.dp).clickable{ onNavigateToLanguageSelection() }
        )

        Text(
            text = contentText,
            color = color,
            softWrap = true,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            lineHeight = 33.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}