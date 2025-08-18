package com.example.augmentedrealityglasses.translation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ResultTextBox(
    modifier: Modifier,
    language: String,
    contentText: String,
    onNavigateToLanguageSelection: () -> Unit
) { //todo add selection language screen if target
    Column(modifier = modifier.fillMaxWidth()) {

        Text(
            text = language,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp).clickable{ onNavigateToLanguageSelection() }
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