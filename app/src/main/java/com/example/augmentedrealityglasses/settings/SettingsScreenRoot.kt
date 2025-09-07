package com.example.augmentedrealityglasses.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

@Composable
fun SettingsScreenRoot(
    viewModel: SettingsViewModel,
    isChangeThemeClicked: MutableState<Boolean>
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape =
        configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        SettingsScreenLandscape(viewModel, isChangeThemeClicked)
    } else {
        SettingsScreenPortrait(viewModel, isChangeThemeClicked)
    }
}
