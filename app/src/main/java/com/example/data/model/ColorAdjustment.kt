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

    // HSL Adjustments (-100 to 100, Hue in degrees -180 to 180)
    val hueShift: Float = 0f,       // -180 to 180
    val hslSaturation: Float = 0f,  // -100 to 100
    val hslLuminance: Float = 0f,   // -100 to 100

    // RGB Balance & Gain (-100 to 100)
    val rgbRed: Float = 0f,         // -100 to 100
    val rgbGreen: Float = 0f,       // -100 to 100
    val rgbBlue: Float = 0f,        // -100 to 100

    // Cinematic Look-Up Table (LUT)
    val lutIndex: Int = 0,          // 0: None, 1: Teal & Orange, 2: Moody Film, 3: Cyberpunk, 4: Clean Arri, 5: Vintage Warm, 6: Bleach Bypass, 7: Noir B&W
    val lutIntensity: Float = 100f, // 0 to 100

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
                hueShift == 0f && hslSaturation == 0f && hslLuminance == 0f &&
                rgbRed == 0f && rgbGreen == 0f && rgbBlue == 0f &&
                lutIndex == 0 &&
                shadowTintAmount == 0f && midtoneTintAmount == 0f && highlightTintAmount == 0f &&
                curveMaster == 0f && curveRed == 0f && curveGreen == 0f && curveBlue == 0f
}

data class CinematicPreset(
    val name: String,
    val subtitle: String,
    val adjustments: ColorAdjustment
)
