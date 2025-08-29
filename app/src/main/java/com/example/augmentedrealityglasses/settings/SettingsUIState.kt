package com.example.augmentedrealityglasses.settings

import kotlinx.serialization.Serializable

@Serializable
data class SettingsUIState(
    val theme: ThemeMode,
    val notificationEnabled: Map<NotificationSource, Boolean>
)