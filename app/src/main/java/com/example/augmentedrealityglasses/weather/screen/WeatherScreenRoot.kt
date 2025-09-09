package com.example.augmentedrealityglasses.weather.screen

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.example.augmentedrealityglasses.weather.viewmodel.WeatherViewModel

@Composable
fun WeatherScreenRoot(
    viewModel: WeatherViewModel,
    onTextFieldClick: () -> Unit,
    onScreenComposition: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        WeatherScreenLandscape(
            viewModel = viewModel,
            onTextFieldClick = onTextFieldClick,
            onScreenComposition = onScreenComposition
        )
    } else {
        WeatherScreenPortrait(
            viewModel = viewModel,
            onTextFieldClick = onTextFieldClick,
            onScreenComposition = onScreenComposition
        )
    }
}