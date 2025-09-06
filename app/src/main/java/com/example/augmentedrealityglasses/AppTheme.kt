package com.example.augmentedrealityglasses

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.core.graphics.ColorUtils

private val LightColors = lightColorScheme(
    primary = Color.hsl(0f, 0f, 0.05f),   // primary text color
    secondary = Color.hsl(0f, 0f, 0.30f), // secondary text color
    onBackground = Color.hsl(0f, 0f, 0.93f), // close to white used for background
    onPrimaryContainer = Color.hsl(0f, 0f, 1f), // primary info panel
    onSurface = Color.hsl(0f, 0f, 0.05f), // used for selected or interactable components (could be used also black 0)
    inversePrimary = Color.hsl(0f, 0f, 1f) //used for download track
)

private val DarkColors = darkColorScheme(
    primary = Color.hsl(0f, 0f, 0.95f),   // primary text color
    secondary = Color.hsl(0f, 0f, 0.70f), // secondary text color
    onBackground = Color.hsl(0f, 0f, 0f), // close to white used for background
    onPrimaryContainer = Color.hsl(0f, 0f, 0.07f), // primary info panel
    onSurface = Color.hsl(0f, 0f, 0.15f), // used for selected or interactable components
    inversePrimary = Color.hsl(0f, 0f, 0f) //used for download track
)


private val WorkSans = FontFamily(
    Font(R.font.worksans_black, FontWeight.Black),
    Font(R.font.worksans_bold, FontWeight.Bold),
    Font(R.font.worksans_extrabold, FontWeight.ExtraBold),
    Font(R.font.worksans_extralight, FontWeight.ExtraLight),
    Font(R.font.worksans_light, FontWeight.Light),
    Font(R.font.worksans_medium, FontWeight.Medium),
    Font(R.font.worksans_regular, FontWeight.Normal),
    Font(R.font.worksans_semibold, FontWeight.SemiBold),
    Font(R.font.worksans_thin, FontWeight.Thin)
)

private val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = WorkSans,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 0.sp
    ),

    bodyMedium = TextStyle(
        fontFamily = WorkSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        letterSpacing = 0.sp,
    ),

    bodySmall = TextStyle(
        fontFamily = WorkSans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        letterSpacing = 0.sp,
    ),

    titleLarge = TextStyle(
        fontFamily = WorkSans,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        fontSize = 28.sp
    ),

    titleMedium = TextStyle(
        fontFamily = WorkSans,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        fontSize = 24.sp
    ),

    titleSmall = TextStyle(
        fontFamily = WorkSans,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        fontSize = 24.sp
    ),

    labelMedium = TextStyle(
        fontFamily = WorkSans,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        fontSize = 14.sp
    ),

)

@Composable
fun AppTheme(
    isDarkThemeSelected: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkThemeSelected) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
