package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.model.ColorAdjustment
import com.example.data.model.EnhancementConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.math.*

/**
 * High-performance CPU fallback color grading and detail engine.
 * Used automatically if hardware OpenGL ES or MediaCodec surface acceleration is unavailable.
 * Uses multi-threaded row chunking across CPU cores.
 */
object CpuColorFilterFallback {

    fun processBitmap(source: Bitmap, adj: ColorAdjustment, config: EnhancementConfig?): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        processPixels(pixels, width, height, adj, config)

        val output = Bitmap.createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    fun processPixels(pixels: IntArray, width: Int, height: Int, adj: ColorAdjustment, config: EnhancementConfig?) {
        val exposureFactor = 2.0.pow((adj.exposure / 50.0).toDouble()).toFloat()
        val brightness = adj.brightness / 100f
        val contrast = (adj.contrast / 100f) + 1.0f
        val saturation = (adj.saturation / 100f) + 1.0f
        val tempR = 1.0f + (adj.temperature / 100f) * 0.35f
        val tempB = 1.0f - (adj.temperature / 100f) * 0.35f
        val tintG = 1.0f - (adj.tint / 100f) * 0.25f
        val tintM = 1.0f + (adj.tint / 100f) * 0.25f
        val shadowShift = (adj.shadows / 100f) * 0.3f
        val highlightShift = (adj.highlights / 100f) * 0.3f
        val whiteShift = (adj.whites / 100f) * 0.3f
        val blackShift = (adj.blacks / 100f) * 0.3f
        val fade = (adj.fade / 100f) * 0.25f
        val curveM = adj.curveMaster / 100f
        val curveR = adj.curveRed / 100f
        val curveG = adj.curveGreen / 100f
        val curveB = adj.curveBlue / 100f
        val rgbR = adj.rgbRed / 100f
        val rgbG = adj.rgbGreen / 100f
        val rgbB = adj.rgbBlue / 100f
        val lutType = adj.lutIndex
        val lutWeight = adj.lutIntensity / 100f

        val aiSharpen = ((config?.aiSharpen ?: 0f) + adj.sharpness).coerceIn(0f, 100f) / 100f
        val aiDenoise = ((config?.aiDenoise ?: 0f)).coerceIn(0f, 100f) / 100f

        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        val chunkSize = height / cores

        runBlocking(Dispatchers.Default) {
            val jobs = (0 until cores).map { coreIdx ->
                async {
                    val startY = coreIdx * chunkSize
                    val endY = if (coreIdx == cores - 1) height else (coreIdx + 1) * chunkSize

                    for (y in startY until endY) {
                        val rowOffset = y * width
                        for (x in 0 until width) {
                            val idx = rowOffset + x
                            val pixel = pixels[idx]

                            var r = Color.red(pixel) / 255f
                            var g = Color.green(pixel) / 255f
                            var b = Color.blue(pixel) / 255f
                            val a = Color.alpha(pixel)

                            // AI Denoise (cross-neighborhood average)
                            if (aiDenoise > 0.05f && x > 0 && x < width - 1 && y > 0 && y < height - 1) {
                                val nP = pixels[idx - width]
                                val sP = pixels[idx + width]
                                val eP = pixels[idx + 1]
                                val wP = pixels[idx - 1]
                                val avgR = (r + Color.red(nP)/255f + Color.red(sP)/255f + Color.red(eP)/255f + Color.red(wP)/255f) * 0.2f
                                val avgG = (g + Color.green(nP)/255f + Color.green(sP)/255f + Color.green(eP)/255f + Color.green(wP)/255f) * 0.2f
                                val avgB = (b + Color.blue(nP)/255f + Color.blue(sP)/255f + Color.blue(eP)/255f + Color.blue(wP)/255f) * 0.2f
                                r = r * (1f - aiDenoise * 0.5f) + avgR * (aiDenoise * 0.5f)
                                g = g * (1f - aiDenoise * 0.5f) + avgG * (aiDenoise * 0.5f)
                                b = b * (1f - aiDenoise * 0.5f) + avgB * (aiDenoise * 0.5f)
                            }

                            // AI Sharpen (laplacian unsharp mask)
                            if (aiSharpen > 0.05f && x > 0 && x < width - 1 && y > 0 && y < height - 1) {
                                val nP = pixels[idx - width]
                                val sP = pixels[idx + width]
                                val eP = pixels[idx + 1]
                                val wP = pixels[idx - 1]
                                val lapR = 4f * r - (Color.red(nP) + Color.red(sP) + Color.red(eP) + Color.red(wP)) / 255f
                                val lapG = 4f * g - (Color.green(nP) + Color.green(sP) + Color.green(eP) + Color.green(wP)) / 255f
                                val lapB = 4f * b - (Color.blue(nP) + Color.blue(sP) + Color.blue(eP) + Color.blue(wP)) / 255f
                                r += lapR * (aiSharpen * 0.6f)
                                g += lapG * (aiSharpen * 0.6f)
                                b += lapB * (aiSharpen * 0.6f)
                            }

                            // Exposure & Brightness
                            r = r * exposureFactor + brightness
                            g = g * exposureFactor + brightness
                            b = b * exposureFactor + brightness

                            // White balance
                            r *= (tempR * tintM)
                            g *= tintG
                            b *= tempB

                            // RGB Balance
                            r *= (1f + rgbR)
                            g *= (1f + rgbG)
                            b *= (1f + rgbB)

                            // Contrast
                            r = (r - 0.5f) * contrast + 0.5f
                            g = (g - 0.5f) * contrast + 0.5f
                            b = (b - 0.5f) * contrast + 0.5f

                            val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b

                            // Shadows, Highlights, Whites, Blacks
                            val shadowMask = (1f - luma * 2f).coerceIn(0f, 1f)
                            val highlightMask = ((luma - 0.5f) * 2f).coerceIn(0f, 1f)
                            val whiteMask = ((luma - 0.7f) * 3.33f).coerceIn(0f, 1f)
                            val blackMask = (1f - luma * 3.33f).coerceIn(0f, 1f)

                            r += shadowShift * shadowMask + highlightShift * highlightMask + whiteShift * whiteMask + blackShift * blackMask
                            g += shadowShift * shadowMask + highlightShift * highlightMask + whiteShift * whiteMask + blackShift * blackMask
                            b += shadowShift * shadowMask + highlightShift * highlightMask + whiteShift * whiteMask + blackShift * blackMask

                            // Curves
                            if (abs(curveM) > 0.001f) {
                                r += r * (1f - r) * curveM
                                g += g * (1f - g) * curveM
                                b += b * (1f - b) * curveM
                            }
                            if (abs(curveR) > 0.001f) r += r * (1f - r) * curveR
                            if (abs(curveG) > 0.001f) g += g * (1f - g) * curveG
                            if (abs(curveB) > 0.001f) b += b * (1f - b) * curveB

                            // Fade
                            if (fade > 0.001f) {
                                r = max(r, fade)
                                g = max(g, fade)
                                b = max(b, fade)
                            }

                            // Saturation
                            val postLuma = 0.2126f * r + 0.7152f * g + 0.0722f * b
                            r = postLuma + (r - postLuma) * saturation
                            g = postLuma + (g - postLuma) * saturation
                            b = postLuma + (b - postLuma) * saturation

                            // LUTs
                            if (lutType > 0 && lutWeight > 0.01f) {
                                var lutR = r
                                var lutG = g
                                var lutB = b
                                when (lutType) {
                                    1 -> { // Teal & Orange
                                        val lum = (0.2126f * r + 0.7152f * g + 0.0722f * b).coerceIn(0f, 1f)
                                        lutR = r * (1f - lum) * 0.1f + 1.0f * lum
                                        lutG = g * (1f - lum) * 0.45f + 0.55f * lum
                                        lutB = b * (1f - lum) * 0.75f + 0.15f * lum
                                    }
                                    2 -> { // Moody Film
                                        val lum = 0.2126f * r + 0.7152f * g + 0.0722f * b
                                        lutR = (r - 0.5f) * 1.2f + 0.5f
                                        lutG = (g - 0.5f) * 1.15f + 0.5f
                                        lutB = (b - 0.5f) * 1.3f + 0.53f
                                    }
                                    3 -> { // Cyberpunk
                                        lutR = (r.coerceAtLeast(0f)).pow(0.85f) * 1.15f
                                        lutG *= 0.8f
                                        lutB = (b.coerceAtLeast(0f)).pow(0.8f) * 1.3f
                                    }
                                    4 -> { // Clean Arri
                                        lutR = (r - 0.5f) * 1.1f + 0.5f
                                        lutG = (g - 0.5f) * 1.08f + 0.5f
                                        lutB = (b - 0.5f) * 1.06f + 0.5f
                                    }
                                    5 -> { // Vintage 70s
                                        lutR = max(r * 1.12f, 0.06f)
                                        lutG = max(g * 1.04f, 0.05f)
                                        lutB = max(b * 0.84f, 0.04f)
                                    }
                                    6 -> { // Bleach Bypass
                                        val lum = 0.2126f * r + 0.7152f * g + 0.0722f * b
                                        val blendR = 2f * r * lum
                                        val blendG = 2f * g * lum
                                        val blendB = 2f * b * lum
                                        lutR = r * 0.4f + blendR * 0.6f
                                        lutG = g * 0.4f + blendG * 0.6f
                                        lutB = b * 0.4f + blendB * 0.6f
                                    }
                                    7 -> { // Noir B&W
                                        val lum = 0.299f * r + 0.587f * g + 0.114f * b
                                        val cLum = ((lum - 0.5f) * 1.38f + 0.5f).coerceIn(0f, 1f)
                                        lutR = cLum
                                        lutG = cLum
                                        lutB = cLum
                                    }
                                    8 -> { // Sunset Gold
                                        lutR = (r * 1.30f + 0.05f).coerceIn(0f, 1f)
                                        lutG = (g * 1.06f + 0.01f).coerceIn(0f, 1f)
                                        lutB = (b * 0.70f).coerceIn(0f, 1f)
                                        lutR = ((lutR - 0.5f) * 1.15f + 0.5f).coerceIn(0f, 1f)
                                        lutG = ((lutG - 0.5f) * 1.15f + 0.5f).coerceIn(0f, 1f)
                                        lutB = ((lutB - 0.5f) * 1.15f + 0.5f).coerceIn(0f, 1f)
                                    }
                                }
                                r = r * (1f - lutWeight) + lutR * lutWeight
                                g = g * (1f - lutWeight) + lutG * lutWeight
                                b = b * (1f - lutWeight) + lutB * lutWeight
                            }

                            val ir = (r.coerceIn(0f, 1f) * 255f).toInt()
                            val ig = (g.coerceIn(0f, 1f) * 255f).toInt()
                            val ib = (b.coerceIn(0f, 1f) * 255f).toInt()

                            pixels[idx] = Color.argb(a, ir, ig, ib)
                        }
                    }
                }
            }
            jobs.awaitAll()
        }
    }
}
