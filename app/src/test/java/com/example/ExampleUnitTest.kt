package com.example

import com.example.data.model.ColorAdjustment
import com.example.data.model.ExportResolution
import com.example.engine.ColorFilterEngine
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun colorFilterEngine_hueAmountToRgb() {
        val zeroRgb = ColorFilterEngine.hueAmountToRgb(180f, 0f)
        assertEquals(0f, zeroRgb[0], 0.001f)
        assertEquals(0f, zeroRgb[1], 0.001f)
        assertEquals(0f, zeroRgb[2], 0.001f)

        val activeRgb = ColorFilterEngine.hueAmountToRgb(0f, 50f)
        assertTrue("Red channel should be positive for 0 deg hue", activeRgb[0] > 0f)
    }

    @Test
    fun colorAdjustment_defaultsAndParameters() {
        val adj = ColorAdjustment()
        assertTrue("Default adjustment should be isDefault", adj.isDefault)

        val modified = adj.copy(hueShift = 45f, rgbRed = 20f, lutIndex = 1, curveRed = 15f)
        assertFalse("Modified adjustment should not be isDefault", modified.isDefault)
        assertEquals(45f, modified.hueShift, 0.001f)
        assertEquals(20f, modified.rgbRed, 0.001f)
        assertEquals(1, modified.lutIndex)
        assertEquals(15f, modified.curveRed, 0.001f)
    }

    @Test
    fun resolutionCalculation_portraitAndLandscape() {
        val targetRes = ExportResolution.RES_1080P // 1920 x 1080

        // Test Portrait video (720 x 1280)
        val portraitWidth = 720
        val portraitHeight = 1280
        val isPortrait = portraitHeight > portraitWidth
        val (calcW, calcH) = if (isPortrait) {
            min(targetRes.width, targetRes.height) to max(targetRes.width, targetRes.height)
        } else {
            max(targetRes.width, targetRes.height) to min(targetRes.width, targetRes.height)
        }
        assertEquals(1080, calcW)
        assertEquals(1920, calcH)

        // Test Landscape video (1280 x 720)
        val landscapeWidth = 1280
        val landscapeHeight = 720
        val isLandscapePortrait = landscapeHeight > landscapeWidth
        val (calcLandW, calcLandH) = if (isLandscapePortrait) {
            min(targetRes.width, targetRes.height) to max(targetRes.width, targetRes.height)
        } else {
            max(targetRes.width, targetRes.height) to min(targetRes.width, targetRes.height)
        }
        assertEquals(1920, calcLandW)
        assertEquals(1080, calcLandH)
    }
}
