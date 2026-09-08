package com.example.data.model

data class ColorAdjustment(
    // Basic Adjustments (-100 to +100, or 0 to 100)
    val exposure: Float = 0f,      // -100 to 100
    val brightness: Float = 0f,    // -100 to 100
    val contrast: Float = 0f,      // -100 to 100
    val highlights: Float = 0f,    // -100 to 100
    val shadows: Float = 0f,       // -100 to 100
    val whites: Float = 0f,        // -100 to 100
    val blacks: Float = 0f,        // -100 to 100
    val saturation: Float = 0f,    // -100 to 100
    val vibrance: Float = 0f,      // -100 to 100
    val temperature: Float = 0f,   // -100 to 100 (Warm <-> Cool)
    val tint: Float = 0f,          // -100 to 100 (Green <-> Magenta)
    val fade: Float = 0f,          // 0 to 100
    val sharpness: Float = 0f,     // 0 to 100
    val clarity: Float = 0f,       // 0 to 100

    // Color Wheels (Hue in degrees 0..360, Saturation/Amount 0..100)
    val shadowTintHue: Float = 220f,
    val shadowTintAmount: Float = 0f,
    val midtoneTintHue: Float = 40f,
    val midtoneTintAmount: Float = 0f,
    val highlightTintHue: Float = 35f,
    val highlightTintAmount: Float = 0f,

    // RGB Curves (Midpoint adjustment -100 to 100)
    val curveMaster: Float = 0f,
    val curveRed: Float = 0f,
    val curveGreen: Float = 0f,
    val curveBlue: Float = 0f,

    // Selected Preset Name if any
    val presetName: String = "Natural"
) {
    val isDefault: Boolean
        get() = exposure == 0f && brightness == 0f && contrast == 0f &&
                highlights == 0f && shadows == 0f && whites == 0f && blacks == 0f &&
                saturation == 0f && vibrance == 0f && temperature == 0f && tint == 0f &&
                fade == 0f && sharpness == 0f && clarity == 0f &&
                shadowTintAmount == 0f && midtoneTintAmount == 0f && highlightTintAmount == 0f
}

data class CinematicPreset(
    val name: String,
    val subtitle: String,
    val adjustments: ColorAdjustment
)
