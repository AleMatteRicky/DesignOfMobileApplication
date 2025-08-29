package com.example.augmentedrealityglasses.notifications

import android.content.Context
import com.example.augmentedrealityglasses.App
import com.example.augmentedrealityglasses.cache.DefaultTimeProvider
import com.example.augmentedrealityglasses.settings.NotificationSource
import com.example.augmentedrealityglasses.settings.SettingsUIState

object NotificationFilters {

    suspend fun isEnabled(context: Context, source: NotificationSource): Boolean {
        val container = (context.applicationContext as App).container
        val state = container.settingsCache.getIfValid(
            key = "settings",
            policy = container.settingsCachePolicy,
            serializer = SettingsUIState.serializer(),
            timeProvider = DefaultTimeProvider
        )
        return state?.notificationEnabled?.get(source) ?: false
    }
}