package com.example.engine

import androidx.compose.ui.graphics.ColorMatrix
import com.example.data.model.ColorAdjustment
import kotlin.math.cos
import kotlin.math.sin

object ColorFilterEngine {

    fun createColorMatrix(adj: ColorAdjustment): ColorMatrix {
        // Exposure factor: e.g. -100 -> 0.5x, 0 -> 1.0x, +100 -> 2.0x
        val exposureFactor = if (adj.exposure >= 0) 1f + (adj.exposure / 100f) else 1f / (1f - (adj.exposure / 100f))

        // Brightness offset in [-255, 255] normalized to [-1, 1]
        val brightnessOffset = (adj.brightness / 100f) * 60f

        // Contrast: 0 -> 1.0, -100 -> 0.3, +100 -> 2.2
        val contrast = if (adj.contrast >= 0) 1f + (adj.contrast / 100f) * 1.2f else 1f + (adj.contrast / 100f) * 0.7f
        val contrastOffset = (1f - contrast) * 128f

        // Saturation factor: -100 -> 0.0 (B&W), 0 -> 1.0, +100 -> 2.0
        val sat = ((adj.saturation + 100f) / 100f).coerceIn(0f, 2.5f)

        // Temperature (-100 cool, +100 warm)
        val tempR = if (adj.temperature > 0) 1f + (adj.temperature / 200f) else 1f
        val tempB = if (adj.temperature < 0) 1f + (-adj.temperature / 200f) else 1f

        // Tint (-100 green, +100 magenta)
        val tintG = if (adj.tint < 0) 1f + (-adj.tint / 250f) else 1f
        val tintM = if (adj.tint > 0) 1f + (adj.tint / 250f) else 1f

        // Fade: lifts black floor
        val fadeLift = (adj.fade / 100f) * 45f

        // Shadows & Highlights approximation
        val shadowLift = (adj.shadows / 100f) * 30f
        val highlightCompress = -(adj.highlights / 100f) * 25f

        // Compute combined standard RGB luminance weights
        val lr = 0.2126f
        val lg = 0.7152f
        val lb = 0.0722f

        val invSat = 1f - sat
        val rSat = invSat * lr
        val gSat = invSat * lg
        val bSat = invSat * lb

        val scaleR = exposureFactor * contrast * tempR * tintM
        val scaleG = exposureFactor * contrast * tintG
        val scaleB = exposureFactor * contrast * tempB

        val netOffsetR = brightnessOffset + contrastOffset + fadeLift + shadowLift + highlightCompress
        val netOffsetG = brightnessOffset + contrastOffset + fadeLift + shadowLift * 0.8f + highlightCompress * 0.8f
        val netOffsetB = brightnessOffset + contrastOffset + fadeLift + shadowLift * 0.6f + highlightCompress * 0.6f

        val matrix = floatArrayOf(
            (rSat + sat) * scaleR, gSat * scaleR, bSat * scaleR, 0f, netOffsetR,
            rSat * scaleG, (gSat + sat) * scaleG, bSat * scaleG, 0f, netOffsetG,
            rSat * scaleB, gSat * scaleB, (bSat + sat) * scaleB, 0f, netOffsetB,
            0f, 0f, 0f, 1f, 0f
        )

        return ColorMatrix(matrix)
    }

    fun getGlslFragmentShader(adj: ColorAdjustment): String {
        return """
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform sampler2D sTexture;
            void main() {
                vec4 color = texture2D(sTexture, vTextureCoord);
                // Exposure & Brightness
                color.rgb *= ${1f + adj.exposure / 100f};
                color.rgb += vec3(${adj.brightness / 255f});
                // Contrast
                color.rgb = (color.rgb - 0.5) * ${1f + adj.contrast / 100f} + 0.5;
                // Saturation
                float luma = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
                color.rgb = mix(vec3(luma), color.rgb, ${(adj.saturation + 100f) / 100f});
                gl_FragColor = color;
            }
        """.trimIndent()
    }
}
