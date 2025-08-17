package com.example.augmentedrealityglasses.translation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.augmentedrealityglasses.Icon
import com.example.augmentedrealityglasses.translation.TranslationViewModel

@Composable
fun LanguageSelectionBox(
    enabled: Boolean,
    viewModel: TranslationViewModel,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .width(123.dp)
                .fillMaxHeight()
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(14.dp),
                    clip = false

                )
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(
                    start = 16.dp,
                    top = 8.dp,
                    end = 16.dp,
                    bottom = 8.dp
                )
                .align(Alignment.CenterStart)
        ) {
            Text(
                "Detect Language",
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
        Image(
            painter = painterResource(id = Icon.RIGHT_ARROW.getID()),
            contentDescription = "An arrow from the source language to the target language",
            Modifier.align(Alignment.Center)
        )

        SelectLanguageButton(
            enabled = enabled,
            viewModel = viewModel,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(123.dp)
                .fillMaxHeight(), onClick = onClick
        )
    }
}