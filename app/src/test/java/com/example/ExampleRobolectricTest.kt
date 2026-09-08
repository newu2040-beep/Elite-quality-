package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.ProjectEntity
import com.example.data.model.ColorAdjustment
import com.example.data.model.VideoMetadata
import com.example.engine.ColorFilterEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ELITE QUALITY", appName)
    }

    @Test
    fun `color filter engine creates valid color matrix`() {
        val adj = ColorAdjustment(
            exposure = 15f,
            contrast = 20f,
            saturation = -10f,
            temperature = 10f
        )
        val matrix = ColorFilterEngine.createColorMatrix(adj)
        assertNotNull(matrix)
        assertEquals(20, matrix.values.size)
    }

    @Test
    fun `video metadata resolution labels format properly`() {
        val meta4k = VideoMetadata(
            uriString = "content://media/1",
            fileName = "clip.mp4",
            width = 3840,
            height = 2160,
            durationMs = 65000L
        )
        assertEquals("4K UHD", meta4k.resolutionLabel)
        assertEquals("16:9", meta4k.aspectRatioLabel)
        assertEquals("01:05", meta4k.formattedDuration)

        val meta1080p = VideoMetadata(
            uriString = "content://media/2",
            fileName = "reel.mp4",
            width = 1080,
            height = 1920
        )
        assertEquals("1080p FHD", meta1080p.resolutionLabel)
        assertEquals("9:16", meta1080p.aspectRatioLabel)
    }

    @Test
    fun `room database project insertion and retrieval`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val projectDao = db.projectDao()

        val project = ProjectEntity(
            title = "Test Cinema Footage",
            sourceUri = "file:///storage/test.mp4",
            durationMs = 12000L,
            width = 1920,
            height = 1080,
            enhancementPreset = "Cinematic 4K"
        )
        val id = projectDao.insertProject(project)
        assertTrue(id > 0)

        val retrieved = projectDao.getProjectById(id)
        assertNotNull(retrieved)
        assertEquals("Test Cinema Footage", retrieved?.title)
        assertEquals("Cinematic 4K", retrieved?.enhancementPreset)
    }
}
