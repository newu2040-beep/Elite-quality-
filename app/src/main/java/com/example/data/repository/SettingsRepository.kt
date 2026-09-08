package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("elite_quality_settings", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(prefs.getString("theme", "Obsidian") ?: "Obsidian")
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _darkMode = MutableStateFlow(prefs.getString("dark_mode", "dark") ?: "dark")
    val darkMode: StateFlow<String> = _darkMode.asStateFlow()

    private val _compactMode = MutableStateFlow(prefs.getString("compact_mode", "auto") ?: "auto")
    val compactMode: StateFlow<String> = _compactMode.asStateFlow()

    private val _preferGpu = MutableStateFlow(prefs.getBoolean("prefer_gpu", true))
    val preferGpu: StateFlow<Boolean> = _preferGpu.asStateFlow()

    private val _hardwareEncoding = MutableStateFlow(prefs.getBoolean("hw_encoding", true))
    val hardwareEncoding: StateFlow<Boolean> = _hardwareEncoding.asStateFlow()

    private val _thermalProtection = MutableStateFlow(prefs.getBoolean("thermal_protection", true))
    val thermalProtection: StateFlow<Boolean> = _thermalProtection.asStateFlow()

    private val _defaultResolution = MutableStateFlow(prefs.getString("default_res", "1080p Full HD") ?: "1080p Full HD")
    val defaultResolution: StateFlow<String> = _defaultResolution.asStateFlow()

    private val _defaultCodec = MutableStateFlow(prefs.getString("default_codec", "H.264 / AVC") ?: "H.264 / AVC")
    val defaultCodec: StateFlow<String> = _defaultCodec.asStateFlow()

    fun setTheme(themeName: String) {
        prefs.edit().putString("theme", themeName).apply()
        _theme.value = themeName
    }

    fun setDarkMode(mode: String) {
        prefs.edit().putString("dark_mode", mode).apply()
        _darkMode.value = mode
    }

    fun setCompactMode(mode: String) {
        prefs.edit().putString("compact_mode", mode).apply()
        _compactMode.value = mode
    }

    fun setPreferGpu(enabled: Boolean) {
        prefs.edit().putBoolean("prefer_gpu", enabled).apply()
        _preferGpu.value = enabled
    }

    fun setHardwareEncoding(enabled: Boolean) {
        prefs.edit().putBoolean("hw_encoding", enabled).apply()
        _hardwareEncoding.value = enabled
    }

    fun setThermalProtection(enabled: Boolean) {
        prefs.edit().putBoolean("thermal_protection", enabled).apply()
        _thermalProtection.value = enabled
    }

    fun setDefaultResolution(res: String) {
        prefs.edit().putString("default_res", res).apply()
        _defaultResolution.value = res
    }

    fun setDefaultCodec(codec: String) {
        prefs.edit().putString("default_codec", codec).apply()
        _defaultCodec.value = codec
    }
}
