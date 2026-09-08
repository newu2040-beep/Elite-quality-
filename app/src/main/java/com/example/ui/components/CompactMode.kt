package com.example.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class CompactUiConfig(
    val isCompact: Boolean,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val cardPadding: Dp,
    val contentSpacing: Dp,
    val heroButtonHeight: Dp,
    val secondaryButtonHeight: Dp,
    val editorPanelHeight: Dp,
    val bottomBarButtonHeight: Dp,
    val wheelSize: Dp,
    val curveGraphHeight: Dp,
    val sliderVerticalPadding: Dp,
    val chipPaddingH: Dp,
    val chipPaddingV: Dp,
    val videoPreviewMaxHeight: Dp
)

val LocalCompactUiConfig = compositionLocalOf {
    CompactUiConfig(
        isCompact = false,
        screenWidthDp = 411,
        screenHeightDp = 891,
        cardPadding = 20.dp,
        contentSpacing = 20.dp,
        heroButtonHeight = 52.dp,
        secondaryButtonHeight = 48.dp,
        editorPanelHeight = 230.dp,
        bottomBarButtonHeight = 52.dp,
        wheelSize = 100.dp,
        curveGraphHeight = 130.dp,
        sliderVerticalPadding = 6.dp,
        chipPaddingH = 16.dp,
        chipPaddingV = 8.dp,
        videoPreviewMaxHeight = 240.dp
    )
}

@Composable
fun rememberCompactUiConfig(compactModeSetting: String): CompactUiConfig {
    val config = LocalConfiguration.current
    val screenWidthDp = config.screenWidthDp
    val screenHeightDp = config.screenHeightDp

    val isCompact = remember(compactModeSetting, screenWidthDp, screenHeightDp) {
        when (compactModeSetting) {
            "compact" -> true
            "standard" -> false
            else -> screenHeightDp < 730 || screenWidthDp < 375
        }
    }

    return remember(isCompact, screenWidthDp, screenHeightDp) {
        if (isCompact) {
            CompactUiConfig(
                isCompact = true,
                screenWidthDp = screenWidthDp,
                screenHeightDp = screenHeightDp,
                cardPadding = 12.dp,
                contentSpacing = 12.dp,
                heroButtonHeight = 42.dp,
                secondaryButtonHeight = 38.dp,
                editorPanelHeight = 175.dp,
                bottomBarButtonHeight = 44.dp,
                wheelSize = 64.dp,
                curveGraphHeight = 90.dp,
                sliderVerticalPadding = 2.dp,
                chipPaddingH = 10.dp,
                chipPaddingV = 4.dp,
                videoPreviewMaxHeight = 165.dp
            )
        } else {
            CompactUiConfig(
                isCompact = false,
                screenWidthDp = screenWidthDp,
                screenHeightDp = screenHeightDp,
                cardPadding = 20.dp,
                contentSpacing = 20.dp,
                heroButtonHeight = 52.dp,
                secondaryButtonHeight = 48.dp,
                editorPanelHeight = 230.dp,
                bottomBarButtonHeight = 52.dp,
                wheelSize = 100.dp,
                curveGraphHeight = 130.dp,
                sliderVerticalPadding = 6.dp,
                chipPaddingH = 16.dp,
                chipPaddingV = 8.dp,
                videoPreviewMaxHeight = 240.dp
            )
        }
    }
}
