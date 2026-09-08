package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun EliteQualityTheme(
    themeName: String = "Obsidian",
    darkModeSetting: String = "dark",
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (darkModeSetting) {
        "light" -> false
        "system" -> isSystemDark
        else -> true
    }

    val colorScheme = when {
        !isDark && themeName == "Frost" -> lightColorScheme(
            primary = FrostPrimary,
            onPrimary = Color.White,
            secondary = FrostAccent,
            background = FrostBg,
            surface = FrostSurface,
            surfaceVariant = FrostSurfaceElevated,
            outline = FrostCardBorder,
            onBackground = PearlTextPrimary,
            onSurface = PearlTextPrimary,
            onSurfaceVariant = PearlTextSecondary
        )
        !isDark -> lightColorScheme(
            primary = PearlPrimary,
            onPrimary = Color.White,
            secondary = PearlAccent,
            background = PearlBg,
            surface = PearlSurface,
            surfaceVariant = PearlSurfaceElevated,
            outline = PearlCardBorder,
            onBackground = PearlTextPrimary,
            onSurface = PearlTextPrimary,
            onSurfaceVariant = PearlTextSecondary
        )
        themeName == "Midnight" -> darkColorScheme(
            primary = MidnightPrimary,
            onPrimary = Color.Black,
            secondary = MidnightAccent,
            background = MidnightBg,
            surface = MidnightSurface,
            surfaceVariant = MidnightSurfaceElevated,
            outline = MidnightCardBorder,
            onBackground = ObsidianTextPrimary,
            onSurface = ObsidianTextPrimary,
            onSurfaceVariant = ObsidianTextSecondary
        )
        themeName == "Graphite" -> darkColorScheme(
            primary = GraphitePrimary,
            onPrimary = Color.Black,
            secondary = GraphiteAccent,
            background = GraphiteBg,
            surface = GraphiteSurface,
            surfaceVariant = GraphiteSurfaceElevated,
            outline = GraphiteCardBorder,
            onBackground = ObsidianTextPrimary,
            onSurface = ObsidianTextPrimary,
            onSurfaceVariant = ObsidianTextSecondary
        )
        themeName == "Aurora" -> darkColorScheme(
            primary = AuroraPrimary,
            onPrimary = Color.Black,
            secondary = AuroraAccent,
            background = AuroraBg,
            surface = AuroraSurface,
            surfaceVariant = AuroraSurfaceElevated,
            outline = AuroraCardBorder,
            onBackground = ObsidianTextPrimary,
            onSurface = ObsidianTextPrimary,
            onSurfaceVariant = ObsidianTextSecondary
        )
        else -> darkColorScheme(
            primary = ObsidianPrimary,
            onPrimary = Color.Black,
            secondary = ObsidianAccent,
            background = ObsidianBg,
            surface = ObsidianSurface,
            surfaceVariant = ObsidianSurfaceElevated,
            outline = ObsidianCardBorder,
            onBackground = ObsidianTextPrimary,
            onSurface = ObsidianTextPrimary,
            onSurfaceVariant = ObsidianTextSecondary
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
