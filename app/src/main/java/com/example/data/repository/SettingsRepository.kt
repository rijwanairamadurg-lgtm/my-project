package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme {
    SYSTEM, DARK, LIGHT
}

enum class AppLanguage {
    ENGLISH, KANNADA
}

data class UserSettings(
    val theme: AppTheme = AppTheme.DARK,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val defaultBodmas: Boolean = true,
    val hapticFeedback: Boolean = true,
    val decimalPrecision: Int = 4, // 2, 4, 6, 8 or -1 for auto
    val autoSaveHistory: Boolean = true
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("calculator_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        val themeStr = prefs.getString("theme", AppTheme.DARK.name) ?: AppTheme.DARK.name
        val langStr = prefs.getString("language", AppLanguage.ENGLISH.name) ?: AppLanguage.ENGLISH.name
        val theme = runCatching { AppTheme.valueOf(themeStr) }.getOrDefault(AppTheme.DARK)
        val language = runCatching { AppLanguage.valueOf(langStr) }.getOrDefault(AppLanguage.ENGLISH)
        val defaultBodmas = prefs.getBoolean("default_bodmas", true)
        val haptic = prefs.getBoolean("haptic_feedback", true)
        val precision = prefs.getInt("decimal_precision", 4)
        val autoSave = prefs.getBoolean("auto_save_history", true)

        return UserSettings(
            theme = theme,
            language = language,
            defaultBodmas = defaultBodmas,
            hapticFeedback = haptic,
            decimalPrecision = precision,
            autoSaveHistory = autoSave
        )
    }

    fun updateTheme(theme: AppTheme) {
        prefs.edit().putString("theme", theme.name).apply()
        _settings.value = _settings.value.copy(theme = theme)
    }

    fun updateLanguage(language: AppLanguage) {
        prefs.edit().putString("language", language.name).apply()
        _settings.value = _settings.value.copy(language = language)
    }

    fun toggleLanguage() {
        val next = if (_settings.value.language == AppLanguage.ENGLISH) AppLanguage.KANNADA else AppLanguage.ENGLISH
        updateLanguage(next)
    }

    fun updateDefaultBodmas(enabled: Boolean) {
        prefs.edit().putBoolean("default_bodmas", enabled).apply()
        _settings.value = _settings.value.copy(defaultBodmas = enabled)
    }

    fun updateHapticFeedback(enabled: Boolean) {
        prefs.edit().putBoolean("haptic_feedback", enabled).apply()
        _settings.value = _settings.value.copy(hapticFeedback = enabled)
    }

    fun updateDecimalPrecision(precision: Int) {
        prefs.edit().putInt("decimal_precision", precision).apply()
        _settings.value = _settings.value.copy(decimalPrecision = precision)
    }

    fun updateAutoSaveHistory(enabled: Boolean) {
        prefs.edit().putBoolean("auto_save_history", enabled).apply()
        _settings.value = _settings.value.copy(autoSaveHistory = enabled)
    }

    // Persist last opened screen destination
    fun getLastScreen(): String {
        return prefs.getString("last_screen_destination", "CALCULATOR") ?: "CALCULATOR"
    }

    fun setLastScreen(screen: String) {
        prefs.edit().putString("last_screen_destination", screen).apply()
    }

    // Persist last active calculator page index
    fun getLastCalculatorPage(): Int {
        return prefs.getInt("last_calculator_page_index", 0)
    }

    fun setLastCalculatorPage(pageIndex: Int) {
        prefs.edit().putInt("last_calculator_page_index", pageIndex).apply()
    }

    // Persist last active notebook page index
    fun getLastNotebookPage(): Int {
        return prefs.getInt("last_notebook_page_index", 0)
    }

    fun setLastNotebookPage(pageIndex: Int) {
        prefs.edit().putInt("last_notebook_page_index", pageIndex).apply()
    }
}
