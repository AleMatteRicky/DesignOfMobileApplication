package com.example.augmentedrealityglasses

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

//TODO: specify colors and use them in all screens
private val LightColors = lightColorScheme(
    primary = Color.Black,
    // onPrimary = ...,
    // surface = ...,
    // onSurface = ...,
    // outlineVariant = ...,
)

private val DarkColors = darkColorScheme(
    primary = Color.White,
    // onPrimary = ...,
    // surface = ...,
    // onSurface = ...,
    // outlineVariant = ...,
)

@Composable
fun AppTheme(
    isDarkThemeSelected: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkThemeSelected) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}