package com.example.augmentedrealityglasses.home

import android.content.Context
import com.example.augmentedrealityglasses.settings.ThemeMode

/**
 * It saves the last selected ThemeMode and allows to read it synchronously at app launch.
 */
class BootstrapPrefs(ctx: Context) {

    private val prefs = ctx.getSharedPreferences("bootstrap", Context.MODE_PRIVATE)

    fun getTheme(): ThemeMode? = when (prefs.getString("theme", null)) {
        "DARK" -> ThemeMode.DARK
        "LIGHT" -> ThemeMode.LIGHT
        "SYSTEM" -> ThemeMode.SYSTEM
        else -> null
    }

    fun setTheme(mode: ThemeMode) {
        prefs.edit().putString("theme", mode.name).apply()
    }
}