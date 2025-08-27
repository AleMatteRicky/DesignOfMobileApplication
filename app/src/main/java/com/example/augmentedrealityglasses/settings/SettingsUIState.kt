package com.example.augmentedrealityglasses.settings

import kotlinx.serialization.Serializable

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
data class SettingsUIState(
    val theme: ThemeMode
)