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

    val colorScheme = if (!isDark) {
        when (themeName) {
            "Frost" -> lightColorScheme(
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
            "Pastel Rose" -> lightColorScheme(
                primary = PastelRoseLightPrimary,
                onPrimary = Color.White,
                secondary = PastelRoseLightAccent,
                background = PastelRoseLightBg,
                surface = PastelRoseLightSurface,
                surfaceVariant = PastelRoseLightElevated,
                outline = PastelRoseLightBorder,
                onBackground = PastelRoseLightText,
                onSurface = PastelRoseLightText,
                onSurfaceVariant = PastelRoseLightTextSec
            )
            "Pastel Lavender" -> lightColorScheme(
                primary = PastelLavenderLightPrimary,
                onPrimary = Color.White,
                secondary = PastelLavenderLightAccent,
                background = PastelLavenderLightBg,
                surface = PastelLavenderLightSurface,
                surfaceVariant = PastelLavenderLightElevated,
                outline = PastelLavenderLightBorder,
                onBackground = PastelLavenderLightText,
                onSurface = PastelLavenderLightText,
                onSurfaceVariant = PastelLavenderLightTextSec
            )
            "Pastel Mint" -> lightColorScheme(
                primary = PastelMintLightPrimary,
                onPrimary = Color.White,
                secondary = PastelMintLightAccent,
                background = PastelMintLightBg,
                surface = PastelMintLightSurface,
                surfaceVariant = PastelMintLightElevated,
                outline = PastelMintLightBorder,
                onBackground = PastelMintLightText,
                onSurface = PastelMintLightText,
                onSurfaceVariant = PastelMintLightTextSec
            )
            "Pastel Peach" -> lightColorScheme(
                primary = PastelPeachLightPrimary,
                onPrimary = Color.White,
                secondary = PastelPeachLightAccent,
                background = PastelPeachLightBg,
                surface = PastelPeachLightSurface,
                surfaceVariant = PastelPeachLightElevated,
                outline = PastelPeachLightBorder,
                onBackground = PastelPeachLightText,
                onSurface = PastelPeachLightText,
                onSurfaceVariant = PastelPeachLightTextSec
            )
            "Pastel Sky" -> lightColorScheme(
                primary = PastelSkyLightPrimary,
                onPrimary = Color.White,
                secondary = PastelSkyLightAccent,
                background = PastelSkyLightBg,
                surface = PastelSkyLightSurface,
                surfaceVariant = PastelSkyLightElevated,
                outline = PastelSkyLightBorder,
                onBackground = PastelSkyLightText,
                onSurface = PastelSkyLightText,
                onSurfaceVariant = PastelSkyLightTextSec
            )
            "Pastel Butter" -> lightColorScheme(
                primary = PastelButterLightPrimary,
                onPrimary = Color.Black,
                secondary = PastelButterLightAccent,
                background = PastelButterLightBg,
                surface = PastelButterLightSurface,
                surfaceVariant = PastelButterLightElevated,
                outline = PastelButterLightBorder,
                onBackground = PastelButterLightText,
                onSurface = PastelButterLightText,
                onSurfaceVariant = PastelButterLightTextSec
            )
            "Pastel Matcha" -> lightColorScheme(
                primary = PastelMatchaLightPrimary,
                onPrimary = Color.White,
                secondary = PastelMatchaLightAccent,
                background = PastelMatchaLightBg,
                surface = PastelMatchaLightSurface,
                surfaceVariant = PastelMatchaLightElevated,
                outline = PastelMatchaLightBorder,
                onBackground = PastelMatchaLightText,
                onSurface = PastelMatchaLightText,
                onSurfaceVariant = PastelMatchaLightTextSec
            )
            else -> lightColorScheme(
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
        }
    } else {
        when (themeName) {
            "Midnight" -> darkColorScheme(
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
            "Graphite" -> darkColorScheme(
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
            "Aurora" -> darkColorScheme(
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
            "Pastel Rose" -> darkColorScheme(
                primary = PastelRoseDarkPrimary,
                onPrimary = Color.Black,
                secondary = PastelRoseDarkAccent,
                background = PastelRoseDarkBg,
                surface = PastelRoseDarkSurface,
                surfaceVariant = PastelRoseDarkElevated,
                outline = PastelRoseDarkBorder,
                onBackground = PastelRoseDarkText,
                onSurface = PastelRoseDarkText,
                onSurfaceVariant = PastelRoseDarkTextSec
            )
            "Pastel Lavender" -> darkColorScheme(
                primary = PastelLavenderDarkPrimary,
                onPrimary = Color.Black,
                secondary = PastelLavenderDarkAccent,
                background = PastelLavenderDarkBg,
                surface = PastelLavenderDarkSurface,
                surfaceVariant = PastelLavenderDarkElevated,
                outline = PastelLavenderDarkBorder,
                onBackground = PastelLavenderDarkText,
                onSurface = PastelLavenderDarkText,
                onSurfaceVariant = PastelLavenderDarkTextSec
            )
            "Pastel Mint" -> darkColorScheme(
                primary = PastelMintDarkPrimary,
                onPrimary = Color.Black,
                secondary = PastelMintDarkAccent,
                background = PastelMintDarkBg,
                surface = PastelMintDarkSurface,
                surfaceVariant = PastelMintDarkElevated,
                outline = PastelMintDarkBorder,
                onBackground = PastelMintDarkText,
                onSurface = PastelMintDarkText,
                onSurfaceVariant = PastelMintDarkTextSec
            )
            "Pastel Peach" -> darkColorScheme(
                primary = PastelPeachDarkPrimary,
                onPrimary = Color.Black,
                secondary = PastelPeachDarkAccent,
                background = PastelPeachDarkBg,
                surface = PastelPeachDarkSurface,
                surfaceVariant = PastelPeachDarkElevated,
                outline = PastelPeachDarkBorder,
                onBackground = PastelPeachDarkText,
                onSurface = PastelPeachDarkText,
                onSurfaceVariant = PastelPeachDarkTextSec
            )
            "Pastel Sky" -> darkColorScheme(
                primary = PastelSkyDarkPrimary,
                onPrimary = Color.Black,
                secondary = PastelSkyDarkAccent,
                background = PastelSkyDarkBg,
                surface = PastelSkyDarkSurface,
                surfaceVariant = PastelSkyDarkElevated,
                outline = PastelSkyDarkBorder,
                onBackground = PastelSkyDarkText,
                onSurface = PastelSkyDarkText,
                onSurfaceVariant = PastelSkyDarkTextSec
            )
            "Pastel Butter" -> darkColorScheme(
                primary = PastelButterDarkPrimary,
                onPrimary = Color.Black,
                secondary = PastelButterDarkAccent,
                background = PastelButterDarkBg,
                surface = PastelButterDarkSurface,
                surfaceVariant = PastelButterDarkElevated,
                outline = PastelButterDarkBorder,
                onBackground = PastelButterDarkText,
                onSurface = PastelButterDarkText,
                onSurfaceVariant = PastelButterDarkTextSec
            )
            "Pastel Matcha" -> darkColorScheme(
                primary = PastelMatchaDarkPrimary,
                onPrimary = Color.Black,
                secondary = PastelMatchaDarkAccent,
                background = PastelMatchaDarkBg,
                surface = PastelMatchaDarkSurface,
                surfaceVariant = PastelMatchaDarkElevated,
                outline = PastelMatchaDarkBorder,
                onBackground = PastelMatchaDarkText,
                onSurface = PastelMatchaDarkText,
                onSurfaceVariant = PastelMatchaDarkTextSec
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
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
