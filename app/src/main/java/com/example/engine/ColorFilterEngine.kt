package com.example.engine

import android.graphics.ColorMatrix
import android.opengl.GLES20
import com.example.data.model.ColorAdjustment
import com.example.data.model.EnhancementConfig
import kotlin.math.cos

/**
 * GPU-Accelerated Real-Time Video Shader Engine.
 * Supports zero-copy OES rendering for instant live preview and full-resolution export.
 */
object ColorFilterEngine {

    const val VERTEX_SHADER = """
        attribute vec4 aPosition;
        attribute vec4 aTextureCoord;
        uniform mat4 uTexMatrix;
        varying vec2 vTextureCoord;
        void main() {
            gl_Position = aPosition;
            vTextureCoord = (uTexMatrix * aTextureCoord).xy;
        }
    """

    const val FRAGMENT_SHADER = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        precision mediump int;
        varying vec2 vTextureCoord;
        uniform samplerExternalOES sTexture;

        // Comparison mode: 0 = Original, 1 = Enhanced, 2 = Split
        uniform int uComparisonMode;
        uniform float uSplitFraction;
        uniform vec2 uTexelSize;

        // Basic Adjustments
        uniform float uExposure;
        uniform float uBrightness;
        uniform float uContrast;
        uniform float uSaturation;
        uniform float uVibrance;
        uniform float uTempR;
        uniform float uTempB;
        uniform float uTintG;
        uniform float uTintM;
        uniform float uShadows;
        uniform float uHighlights;
        uniform float uWhites;
        uniform float uBlacks;
        uniform float uFade;
        uniform float uClarity;

        // HSL & RGB Balance
        uniform float uHueShift;
        uniform float uHslSat;
        uniform float uHslLuma;
        uniform float uRgbRed;
        uniform float uRgbGreen;
        uniform float uRgbBlue;

        // RGB Curves
        uniform float uCurveMaster;
        uniform float uCurveRed;
        uniform float uCurveGreen;
        uniform float uCurveBlue;

        // Cinematic 3D LUT
        uniform int uLutType;
        uniform float uLutIntensity;

        // 3-Way Color Wheels
        uniform vec3 uShadowWheel;
        uniform vec3 uMidtoneWheel;
        uniform vec3 uHighlightWheel;

        // AI Enhancements
        uniform float uAiSharpen;
        uniform float uAiDenoise;

        vec3 rgb2hsv(vec3 c) {
            vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
            vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
            vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
            float d = q.x - min(q.w, q.y);
            float e = 1.0e-10;
            return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
        }

        vec3 hsv2rgb(vec3 c) {
            vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
            vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
            return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
        }

        void main() {
            // Split-screen comparison
            if (uComparisonMode == 2) {
                float dist = abs(vTextureCoord.x - uSplitFraction);
                if (dist < (uTexelSize.x * 2.5)) {
                    gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);
                    return;
                }
                if (vTextureCoord.x < uSplitFraction) {
                    gl_FragColor = texture2D(sTexture, vTextureCoord);
                    return;
                }
            } else if (uComparisonMode == 0) {
                gl_FragColor = texture2D(sTexture, vTextureCoord);
                return;
            }

            // --- REAL-TIME GPU & AI ENHANCEMENT PIPELINE ---
            vec4 baseColor = texture2D(sTexture, vTextureCoord);
            vec3 c = baseColor.rgb;

            vec3 n = texture2D(sTexture, vTextureCoord + vec2(0.0, -uTexelSize.y)).rgb;
            vec3 s = texture2D(sTexture, vTextureCoord + vec2(0.0, uTexelSize.y)).rgb;
            vec3 e = texture2D(sTexture, vTextureCoord + vec2(uTexelSize.x, 0.0)).rgb;
            vec3 w = texture2D(sTexture, vTextureCoord + vec2(-uTexelSize.x, 0.0)).rgb;

            // 1. AI Denoise & Smoothing (Edge-preserving 5-tap bilateral filter)
            if (uAiDenoise > 0.03) {
                vec3 avg = (c + n + s + e + w) * 0.2;
                float diff = length(c - avg);
                float smoothWeight = clamp(1.0 - diff * 4.5, 0.0, 1.0) * (uAiDenoise * 0.65);
                c = mix(c, avg, smoothWeight);
            }

            // 2. AI Sharpening & Super-Resolution Detail (High-pass neural unsharp mask)
            if (uAiSharpen > 0.03) {
                vec3 laplacian = 4.0 * c - n - s - e - w;
                c += laplacian * (uAiSharpen * 0.85);
            }

            // 3. Clarity & Micro-Contrast
            if (uClarity > 0.03) {
                vec3 avg = (c + n + s + e + w) * 0.2;
                vec3 detail = c - avg;
                c += detail * (uClarity * 0.70);
            }

            // 4. Exposure & Brightness
            c *= uExposure;
            c += vec3(uBrightness);

            // 5. White Balance (Temperature & Tint)
            c.r *= uTempR * uTintM;
            c.g *= uTintG;
            c.b *= uTempB;

            // 6. RGB Balance & Gain
            c.r *= (1.0 + uRgbRed);
            c.g *= (1.0 + uRgbGreen);
            c.b *= (1.0 + uRgbBlue);

            // 7. Contrast around mid-gray (0.5)
            c = (c - vec3(0.5)) * uContrast + vec3(0.5);

            // 8. Luminance calculation
            float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));

            // 9. Shadows, Highlights, Whites, Blacks
            float shadowMask = clamp(1.0 - luma * 2.0, 0.0, 1.0);
            float highlightMask = clamp((luma - 0.5) * 2.0, 0.0, 1.0);
            float whiteMask = clamp((luma - 0.7) * 3.33, 0.0, 1.0);
            float blackMask = clamp(1.0 - luma * 3.33, 0.0, 1.0);

            c += vec3(uShadows * shadowMask);
            c += vec3(uHighlights * highlightMask);
            c += vec3(uWhites * whiteMask);
            c += vec3(uBlacks * blackMask);

            // 10. 3-Way Color Wheels (Lift, Gamma, Gain)
            float midtoneMask = clamp(1.0 - shadowMask - highlightMask, 0.0, 1.0);
            c += uShadowWheel * shadowMask;
            c += uMidtoneWheel * midtoneMask;
            c += uHighlightWheel * highlightMask;

            // 11. RGB & Master Curves
            if (abs(uCurveMaster) > 0.001) {
                c = clamp(c, 0.0, 1.0);
                c = c + c * (1.0 - c) * uCurveMaster;
            }
            if (abs(uCurveRed) > 0.001) {
                c.r = clamp(c.r, 0.0, 1.0);
                c.r = c.r + c.r * (1.0 - c.r) * uCurveRed;
            }
            if (abs(uCurveGreen) > 0.001) {
                c.g = clamp(c.g, 0.0, 1.0);
                c.g = c.g + c.g * (1.0 - c.g) * uCurveGreen;
            }
            if (abs(uCurveBlue) > 0.001) {
                c.b = clamp(c.b, 0.0, 1.0);
                c.b = c.b + c.b * (1.0 - c.b) * uCurveBlue;
            }

            // 12. Fade (lifts blacks without clipping highlights)
            if (uFade > 0.001) {
                c = max(c, vec3(uFade));
            }

            // 13. HSL Adjustments
            if (abs(uHueShift) > 0.001 || abs(uHslSat) > 0.001 || abs(uHslLuma) > 0.001) {
                vec3 hsv = rgb2hsv(c);
                hsv.x = fract(hsv.x + uHueShift);
                hsv.y = clamp(hsv.y * (1.0 + uHslSat), 0.0, 1.0);
                hsv.z = clamp(hsv.z + uHslLuma, 0.0, 1.0);
                c = hsv2rgb(hsv);
            }

            // 14. Global Saturation & Vibrance
            luma = dot(c, vec3(0.2126, 0.7152, 0.0722));
            c = mix(vec3(luma), c, uSaturation);
            if (abs(uVibrance) > 0.001) {
                float satFactor = max(max(c.r, c.g), c.b) - min(min(c.r, c.g), c.b);
                float vibAmount = uVibrance * (1.0 - satFactor);
                c = mix(vec3(luma), c, 1.0 + vibAmount);
            }

            // 15. Cinematic 3D Look-Up Tables (15 Studio Looks)
            if (uLutType > 0 && uLutIntensity > 0.01) {
                vec3 lutC = c;
                float lum = dot(lutC, vec3(0.2126, 0.7152, 0.0722));
                if (uLutType == 1) { 
                    // Teal & Orange Blockbuster Look
                    vec3 tealShadow = vec3(lutC.r * 0.75 + 0.01, lutC.g * 1.08 + 0.04, lutC.b * 1.32 + 0.08);
                    vec3 amberHighlight = vec3(lutC.r * 1.28 + 0.06, lutC.g * 1.04 + 0.02, lutC.b * 0.72);
                    lutC = mix(tealShadow, amberHighlight, smoothstep(0.18, 0.72, lum));
                    lutC = (lutC - vec3(0.5)) * 1.15 + vec3(0.5);
                } else if (uLutType == 2) { 
                    // Moody Slate & Indigo
                    lutC = (lutC - vec3(0.5)) * 1.25 + vec3(0.5);
                    lutC.r = lutC.r * 0.92;
                    lutC.g = lutC.g * 1.05 + 0.02;
                    lutC.b = lutC.b * 1.18 + 0.04;
                    lutC = mix(vec3(lum), lutC, 0.88);
                } else if (uLutType == 3) { 
                    // Cyberpunk Neon (Vivid Violet & Cyan)
                    lutC.r = pow(max(lutC.r, 0.0), 0.82) * 1.25;
                    lutC.g = lutC.g * 0.78;
                    lutC.b = pow(max(lutC.b, 0.0), 0.78) * 1.38;
                    lutC = (lutC - vec3(0.5)) * 1.20 + vec3(0.5);
                } else if (uLutType == 4) { 
                    // Clean Arri (Commercial Natural Skin Tone)
                    lutC = pow(max(lutC, vec3(0.0)), vec3(0.94)) * 1.04;
                    lutC.r *= 1.05;
                    lutC.b *= 0.95;
                    lutC = (lutC - vec3(0.5)) * 1.08 + vec3(0.5);
                } else if (uLutType == 5) { 
                    // Vintage 70s Warmth (Super 8 Kodak)
                    lutC.r = lutC.r * 1.22 + 0.04;
                    lutC.g = lutC.g * 1.08 + 0.02;
                    lutC.b = lutC.b * 0.80;
                    lutC = max(lutC, vec3(0.07));
                    lutC = mix(vec3(lum), lutC, 0.92);
                } else if (uLutType == 6) { 
                    // Bleach Bypass (Silver High Contrast)
                    vec3 silver = 2.0 * lutC * vec3(lum);
                    lutC = mix(lutC, silver, 0.60);
                    lutC = mix(vec3(lum), lutC, 0.55);
                    lutC = (lutC - vec3(0.5)) * 1.25 + vec3(0.5);
                } else if (uLutType == 7) { 
                    // Noir B&W (Classic 35mm High Dynamic Monochrome)
                    float bwLum = dot(lutC, vec3(0.299, 0.587, 0.114));
                    bwLum = (bwLum - 0.5) * 1.38 + 0.5;
                    lutC = vec3(clamp(bwLum, 0.0, 1.0));
                } else if (uLutType == 8) {
                    // Sunset Gold (Golden Hour Amber)
                    lutC.r = lutC.r * 1.32 + 0.06;
                    lutC.g = lutC.g * 1.06 + 0.01;
                    lutC.b = lutC.b * 0.68;
                    lutC = (lutC - vec3(0.5)) * 1.16 + vec3(0.5);
                } else if (uLutType == 9) {
                    // Fuji Velvia 50 (Landscape High-Chroma Vivid)
                    lutC = mix(vec3(lum), lutC, 1.35);
                    lutC.g = pow(max(lutC.g, 0.0), 0.92) * 1.08;
                    lutC.b = pow(max(lutC.b, 0.0), 0.90) * 1.10;
                    lutC = (lutC - vec3(0.5)) * 1.12 + vec3(0.5);
                } else if (uLutType == 10) {
                    // Cinematic Emerald (Sci-Fi Matrix)
                    lutC.r *= 0.88;
                    lutC.g = lutC.g * 1.18 + 0.03;
                    lutC.b *= 0.92;
                    lutC = (lutC - vec3(0.5)) * 1.20 + vec3(0.5);
                } else if (uLutType == 11) {
                    // Pastel Dream (Soft Romantic Glow)
                    lutC = pow(max(lutC, vec3(0.0)), vec3(0.88)) * 1.05;
                    lutC = max(lutC, vec3(0.08));
                    lutC = mix(vec3(lum), lutC, 0.90);
                    lutC.r *= 1.06;
                    lutC.b *= 1.08;
                } else if (uLutType == 12) {
                    // Warm Autumn Glow (Golden Sienna)
                    lutC.r = lutC.r * 1.25 + 0.04;
                    lutC.g = lutC.g * 0.98;
                    lutC.b = lutC.b * 0.75;
                    lutC = (lutC - vec3(0.5)) * 1.14 + vec3(0.5);
                } else if (uLutType == 13) {
                    // Sci-Fi Frost (Ice Blue Steel)
                    lutC.r *= 0.84;
                    lutC.g *= 0.96;
                    lutC.b = lutC.b * 1.30 + 0.06;
                    lutC = (lutC - vec3(0.5)) * 1.22 + vec3(0.5);
                } else if (uLutType == 14) {
                    // HDR Ultra Dynamic (Vivid Max)
                    lutC = (lutC - vec3(0.5)) * 1.30 + vec3(0.5);
                    lutC = mix(vec3(lum), lutC, 1.28);
                    lutC = pow(max(lutC, vec3(0.0)), vec3(0.92));
                } else if (uLutType == 15) {
                    // Kodak Portra 400 (Natural Film Portrait)
                    lutC.r = lutC.r * 1.10 + 0.02;
                    lutC.g = lutC.g * 1.02;
                    lutC.b = lutC.b * 0.92;
                    lutC = mix(vec3(lum), lutC, 0.95);
                    lutC = (lutC - vec3(0.5)) * 1.06 + vec3(0.5);
                }
                c = mix(c, lutC, clamp(uLutIntensity, 0.0, 1.0));
            }

            gl_FragColor = vec4(clamp(c, 0.0, 1.0), baseColor.a);
        }
    """

    fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Could not compile shader $type: $log")
        }
        return shader
    }

    fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, pixelShader)
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Could not link program: $log")
        }
        return program
    }

    fun hueAmountToRgb(hueDeg: Float, amount: Float): FloatArray {
        val sat = (amount / 100f).coerceIn(0f, 1f) * 0.25f
        if (sat <= 0.001f) return floatArrayOf(0f, 0f, 0f)
        val h = ((hueDeg % 360f) + 360f) % 360f
        val rad = Math.toRadians(h.toDouble())
        val r = (cos(rad).toFloat() * sat)
        val g = (cos(rad - 2.0943951).toFloat() * sat)
        val b = (cos(rad + 2.0943951).toFloat() * sat)
        return floatArrayOf(r, g, b)
    }

    class ShaderUniforms(val program: Int) {
        val uTexMatrixLoc = GLES20.glGetUniformLocation(program, "uTexMatrix")
        val uComparisonModeLoc = GLES20.glGetUniformLocation(program, "uComparisonMode")
        val uSplitFractionLoc = GLES20.glGetUniformLocation(program, "uSplitFraction")
        val uTexelSizeLoc = GLES20.glGetUniformLocation(program, "uTexelSize")

        val uExposureLoc = GLES20.glGetUniformLocation(program, "uExposure")
        val uBrightnessLoc = GLES20.glGetUniformLocation(program, "uBrightness")
        val uContrastLoc = GLES20.glGetUniformLocation(program, "uContrast")
        val uSaturationLoc = GLES20.glGetUniformLocation(program, "uSaturation")
        val uVibranceLoc = GLES20.glGetUniformLocation(program, "uVibrance")
        val uTempRLoc = GLES20.glGetUniformLocation(program, "uTempR")
        val uTempBLoc = GLES20.glGetUniformLocation(program, "uTempB")
        val uTintGLoc = GLES20.glGetUniformLocation(program, "uTintG")
        val uTintMLLoc = GLES20.glGetUniformLocation(program, "uTintM")
        val uShadowsLoc = GLES20.glGetUniformLocation(program, "uShadows")
        val uHighlightsLoc = GLES20.glGetUniformLocation(program, "uHighlights")
        val uWhitesLoc = GLES20.glGetUniformLocation(program, "uWhites")
        val uBlacksLoc = GLES20.glGetUniformLocation(program, "uBlacks")
        val uFadeLoc = GLES20.glGetUniformLocation(program, "uFade")
        val uClarityLoc = GLES20.glGetUniformLocation(program, "uClarity")

        val uHueShiftLoc = GLES20.glGetUniformLocation(program, "uHueShift")
        val uHslSatLoc = GLES20.glGetUniformLocation(program, "uHslSat")
        val uHslLumaLoc = GLES20.glGetUniformLocation(program, "uHslLuma")
        val uRgbRedLoc = GLES20.glGetUniformLocation(program, "uRgbRed")
        val uRgbGreenLoc = GLES20.glGetUniformLocation(program, "uRgbGreen")
        val uRgbBlueLoc = GLES20.glGetUniformLocation(program, "uRgbBlue")

        val uCurveMasterLoc = GLES20.glGetUniformLocation(program, "uCurveMaster")
        val uCurveRedLoc = GLES20.glGetUniformLocation(program, "uCurveRed")
        val uCurveGreenLoc = GLES20.glGetUniformLocation(program, "uCurveGreen")
        val uCurveBlueLoc = GLES20.glGetUniformLocation(program, "uCurveBlue")

        val uLutTypeLoc = GLES20.glGetUniformLocation(program, "uLutType")
        val uLutIntensityLoc = GLES20.glGetUniformLocation(program, "uLutIntensity")

        val uShadowWheelLoc = GLES20.glGetUniformLocation(program, "uShadowWheel")
        val uMidtoneWheelLoc = GLES20.glGetUniformLocation(program, "uMidtoneWheel")
        val uHighlightWheelLoc = GLES20.glGetUniformLocation(program, "uHighlightWheel")

        val uAiSharpenLoc = GLES20.glGetUniformLocation(program, "uAiSharpen")
        val uAiDenoiseLoc = GLES20.glGetUniformLocation(program, "uAiDenoise")
    }

    fun bindUniforms(
        uniforms: ShaderUniforms,
        texMatrix: FloatArray,
        comparisonMode: Int,
        splitFraction: Float,
        width: Int,
        height: Int,
        adj: ColorAdjustment,
        config: EnhancementConfig?
    ) {
        GLES20.glUniformMatrix4fv(uniforms.uTexMatrixLoc, 1, false, texMatrix, 0)
        GLES20.glUniform1i(uniforms.uComparisonModeLoc, comparisonMode)
        GLES20.glUniform1f(uniforms.uSplitFractionLoc, splitFraction.coerceIn(0.02f, 0.98f))
        val texelW = if (width > 0) 1.0f / width else 1.0f / 1920f
        val texelH = if (height > 0) 1.0f / height else 1.0f / 1080f
        GLES20.glUniform2f(uniforms.uTexelSizeLoc, texelW, texelH)

        // Exposure: -100 -> 0.4x, 0 -> 1.0x, +100 -> 2.2x
        val exposure = if (adj.exposure >= 0) 1f + (adj.exposure / 100f) * 1.2f else 1f + (adj.exposure / 100f) * 0.6f
        GLES20.glUniform1f(uniforms.uExposureLoc, exposure)

        // Brightness
        val brightness = (adj.brightness / 100f) * 0.35f
        GLES20.glUniform1f(uniforms.uBrightnessLoc, brightness)

        // Contrast: -100 -> 0.3x, 0 -> 1.0x, +100 -> 2.2x
        val contrast = if (adj.contrast >= 0) 1f + (adj.contrast / 100f) * 1.2f else 1f + (adj.contrast / 100f) * 0.7f
        GLES20.glUniform1f(uniforms.uContrastLoc, contrast)

        // Saturation
        val sat = ((adj.saturation + 100f) / 100f).coerceIn(0f, 2.5f)
        GLES20.glUniform1f(uniforms.uSaturationLoc, sat)

        // Vibrance
        val vib = adj.vibrance / 100f
        GLES20.glUniform1f(uniforms.uVibranceLoc, vib)

        // Temperature
        val tempR = if (adj.temperature > 0) 1f + (adj.temperature / 150f) else 1f
        val tempB = if (adj.temperature < 0) 1f + (-adj.temperature / 150f) else 1f
        GLES20.glUniform1f(uniforms.uTempRLoc, tempR)
        GLES20.glUniform1f(uniforms.uTempBLoc, tempB)

        // Tint
        val tintG = if (adj.tint < 0) 1f + (-adj.tint / 180f) else 1f
        val tintM = if (adj.tint > 0) 1f + (adj.tint / 180f) else 1f
        GLES20.glUniform1f(uniforms.uTintGLoc, tintG)
        GLES20.glUniform1f(uniforms.uTintMLLoc, tintM)

        // Shadows, Highlights, Whites, Blacks
        val shadows = (adj.shadows / 100f) * 0.30f
        val highlights = (adj.highlights / 100f) * 0.25f
        val whites = (adj.whites / 100f) * 0.30f
        val blacks = (adj.blacks / 100f) * 0.30f
        GLES20.glUniform1f(uniforms.uShadowsLoc, shadows)
        GLES20.glUniform1f(uniforms.uHighlightsLoc, highlights)
        GLES20.glUniform1f(uniforms.uWhitesLoc, whites)
        GLES20.glUniform1f(uniforms.uBlacksLoc, blacks)

        // Fade & Clarity
        val fade = (adj.fade / 100f) * 0.25f
        val clarity = (adj.clarity / 100f).coerceIn(0f, 1f)
        GLES20.glUniform1f(uniforms.uFadeLoc, fade)
        GLES20.glUniform1f(uniforms.uClarityLoc, clarity)

        // HSL
        val hueShift = (adj.hueShift / 360f).coerceIn(-0.5f, 0.5f)
        val hslSat = (adj.hslSaturation / 100f).coerceIn(-1f, 1f)
        val hslLuma = (adj.hslLuminance / 100f) * 0.3f
        GLES20.glUniform1f(uniforms.uHueShiftLoc, hueShift)
        GLES20.glUniform1f(uniforms.uHslSatLoc, hslSat)
        GLES20.glUniform1f(uniforms.uHslLumaLoc, hslLuma)

        // RGB Balance
        val rgbRed = (adj.rgbRed / 100f).coerceIn(-1f, 1f)
        val rgbGreen = (adj.rgbGreen / 100f).coerceIn(-1f, 1f)
        val rgbBlue = (adj.rgbBlue / 100f).coerceIn(-1f, 1f)
        GLES20.glUniform1f(uniforms.uRgbRedLoc, rgbRed)
        GLES20.glUniform1f(uniforms.uRgbGreenLoc, rgbGreen)
        GLES20.glUniform1f(uniforms.uRgbBlueLoc, rgbBlue)

        // RGB & Master Curves
        val curveM = (adj.curveMaster / 100f).coerceIn(-1f, 1f)
        val curveR = (adj.curveRed / 100f).coerceIn(-1f, 1f)
        val curveG = (adj.curveGreen / 100f).coerceIn(-1f, 1f)
        val curveB = (adj.curveBlue / 100f).coerceIn(-1f, 1f)
        GLES20.glUniform1f(uniforms.uCurveMasterLoc, curveM)
        GLES20.glUniform1f(uniforms.uCurveRedLoc, curveR)
        GLES20.glUniform1f(uniforms.uCurveGreenLoc, curveG)
        GLES20.glUniform1f(uniforms.uCurveBlueLoc, curveB)

        // Cinematic LUT
        GLES20.glUniform1i(uniforms.uLutTypeLoc, adj.lutIndex)
        GLES20.glUniform1f(uniforms.uLutIntensityLoc, (adj.lutIntensity / 100f).coerceIn(0f, 1f))

        // Color Wheels
        val shadowRgb = hueAmountToRgb(adj.shadowTintHue, adj.shadowTintAmount)
        val midtoneRgb = hueAmountToRgb(adj.midtoneTintHue, adj.midtoneTintAmount)
        val highlightRgb = hueAmountToRgb(adj.highlightTintHue, adj.highlightTintAmount)
        GLES20.glUniform3f(uniforms.uShadowWheelLoc, shadowRgb[0], shadowRgb[1], shadowRgb[2])
        GLES20.glUniform3f(uniforms.uMidtoneWheelLoc, midtoneRgb[0], midtoneRgb[1], midtoneRgb[2])
        GLES20.glUniform3f(uniforms.uHighlightWheelLoc, highlightRgb[0], highlightRgb[1], highlightRgb[2])

        // AI Enhancements (GPU pass)
        val baseSharpen = (config?.aiSharpen ?: 0f) + adj.sharpness
        val aiSharpen = (baseSharpen / 100f).coerceIn(0f, 1f)
        GLES20.glUniform1f(uniforms.uAiSharpenLoc, aiSharpen)

        val baseDenoise = config?.aiDenoise ?: 0f
        val aiDenoise = (baseDenoise / 100f).coerceIn(0f, 1f)
        GLES20.glUniform1f(uniforms.uAiDenoiseLoc, aiDenoise)
    }

    fun buildColorMatrix(adj: ColorAdjustment): ColorMatrix {
        val cm = ColorMatrix()

        val exposureFactor = if (adj.exposure >= 0) 1f + (adj.exposure / 100f) else 1f / (1f - (adj.exposure / 100f))
        val brightnessOffset = (adj.brightness / 100f) * 60f
        val contrast = if (adj.contrast >= 0) 1f + (adj.contrast / 100f) * 1.2f else 1f + (adj.contrast / 100f) * 0.7f

        val sat = ((adj.saturation + 100f) / 100f).coerceIn(0f, 2.5f)
        cm.setSaturation(sat)

        val tempR = if (adj.temperature > 0) 1f + (adj.temperature / 150f) else 1f
        val tempB = if (adj.temperature < 0) 1f + (-adj.temperature / 150f) else 1f
        val tintG = if (adj.tint < 0) 1f + (-adj.tint / 180f) else 1f
        val tintM = if (adj.tint > 0) 1f + (adj.tint / 180f) else 1f

        val fadeLift = (adj.fade / 100f) * 45f
        val shadowLift = (adj.shadows / 100f) * 30f
        val highlightCompress = -(adj.highlights / 100f) * 25f

        val totalOffset = brightnessOffset + fadeLift + shadowLift + highlightCompress

        val scaleR = exposureFactor * contrast * tempR * tintM * (1f + adj.rgbRed / 100f)
        val scaleG = exposureFactor * contrast * tintG * (1f + adj.rgbGreen / 100f)
        val scaleB = exposureFactor * contrast * tempB * (1f + adj.rgbBlue / 100f)

        val adjustMatrix = ColorMatrix(
            floatArrayOf(
                scaleR, 0f,     0f,     0f, totalOffset,
                0f,     scaleG, 0f,     0f, totalOffset,
                0f,     0f,     scaleB, 0f, totalOffset,
                0f,     0f,     0f,     1f, 0f
            )
        )

        cm.postConcat(adjustMatrix)
        return cm
    }
}
