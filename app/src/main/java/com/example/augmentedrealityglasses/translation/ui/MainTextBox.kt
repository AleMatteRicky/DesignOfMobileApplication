package com.example.augmentedrealityglasses.translation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.augmentedrealityglasses.translation.TranslationViewModel

@Composable
fun MainTextBox(viewModel: TranslationViewModel, modifier: Modifier) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(14.dp),
                clip = false
            )
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 16.dp)
    ) {
        Text(
            text = "Record text",
            color = Color(0xFF717070),
            style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp)
        )
    }

}