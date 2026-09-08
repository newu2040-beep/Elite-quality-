package com.example.data.repository

import com.example.data.db.PresetDao
import com.example.data.db.PresetEntity
import kotlinx.coroutines.flow.Flow

class PresetRepository(private val presetDao: PresetDao) {
    val allPresets: Flow<List<PresetEntity>> = presetDao.getAllPresets()

    suspend fun savePreset(preset: PresetEntity): Long = presetDao.insertPreset(preset)

    suspend fun deletePreset(id: Long) = presetDao.deletePresetById(id)

    suspend fun initDefaultPresetsIfEmpty() {
        if (presetDao.getPresetCount() == 0) {
            val defaults = listOf(
                PresetEntity(
                    name = "Cinematic 4K",
                    subtitle = "High dynamic range & subtle anamorphic contrast",
                    isCustom = false,
                    exposure = 2f,
                    contrast = 14f,
                    highlights = -8f,
                    shadows = 10f,
                    saturation = 6f,
                    vibrance = 12f,
                    temperature = 4f,
                    tint = -2f,
                    sharpness = 30f,
                    clarity = 20f,
                    aiDenoise = 45f,
                    aiSharpen = 35f,
                    aiUpscale = true,
                    targetResolution = "4K Ultra HD"
                ),
                PresetEntity(
                    name = "Natural Clean",
                    subtitle = "Balanced true-to-life tones with crisp detail",
                    isCustom = false,
                    exposure = 0f,
                    contrast = 5f,
                    highlights = 0f,
                    shadows = 4f,
                    saturation = 4f,
                    vibrance = 6f,
                    temperature = 0f,
                    tint = 0f,
                    sharpness = 25f,
                    clarity = 15f,
                    aiDenoise = 40f,
                    aiSharpen = 30f,
                    aiUpscale = true,
                    targetResolution = "1080p Full HD"
                ),
                PresetEntity(
                    name = "Film Grain & Warmth",
                    subtitle = "Kodak aesthetic with golden highlights & soft rolloff",
                    isCustom = false,
                    exposure = -2f,
                    contrast = 18f,
                    highlights = -12f,
                    shadows = 14f,
                    saturation = -4f,
                    vibrance = 8f,
                    temperature = 16f,
                    tint = 4f,
                    sharpness = 20f,
                    clarity = 10f,
                    aiDenoise = 25f,
                    aiSharpen = 20f,
                    aiUpscale = false,
                    targetResolution = "4K Ultra HD"
                ),
                PresetEntity(
                    name = "Moody Noir",
                    subtitle = "Deep dramatic shadows with icy teal undertones",
                    isCustom = false,
                    exposure = -6f,
                    contrast = 24f,
                    highlights = 6f,
                    shadows = -16f,
                    saturation = -18f,
                    vibrance = -10f,
                    temperature = -14f,
                    tint = -6f,
                    sharpness = 35f,
                    clarity = 28f,
                    aiDenoise = 50f,
                    aiSharpen = 40f,
                    aiUpscale = true,
                    targetResolution = "4K Ultra HD"
                ),
                PresetEntity(
                    name = "Night & Low-Light",
                    subtitle = "Shadow recovery with intense chroma de-noising",
                    isCustom = false,
                    exposure = 14f,
                    contrast = 8f,
                    highlights = -15f,
                    shadows = 32f,
                    saturation = 8f,
                    vibrance = 16f,
                    temperature = -4f,
                    tint = 2f,
                    sharpness = 40f,
                    clarity = 22f,
                    aiDenoise = 75f,
                    aiSharpen = 45f,
                    aiUpscale = true,
                    targetResolution = "4K Ultra HD"
                ),
                PresetEntity(
                    name = "Portrait Studio",
                    subtitle = "Skin-preserving clarity & delicate highlight soft-bloom",
                    isCustom = false,
                    exposure = 4f,
                    contrast = 6f,
                    highlights = -4f,
                    shadows = 8f,
                    saturation = 5f,
                    vibrance = 10f,
                    temperature = 6f,
                    tint = 6f,
                    sharpness = 20f,
                    clarity = 12f,
                    aiDenoise = 35f,
                    aiSharpen = 25f,
                    aiUpscale = true,
                    targetResolution = "1080p Full HD"
                ),
                PresetEntity(
                    name = "Vibrant HDR",
                    subtitle = "Expanded saturation and high-impact micro-contrast",
                    isCustom = false,
                    exposure = 0f,
                    contrast = 16f,
                    highlights = -10f,
                    shadows = 18f,
                    saturation = 28f,
                    vibrance = 32f,
                    temperature = 2f,
                    tint = 0f,
                    sharpness = 45f,
                    clarity = 30f,
                    aiDenoise = 40f,
                    aiSharpen = 50f,
                    aiUpscale = true,
                    targetResolution = "4K Ultra HD"
                ),
                PresetEntity(
                    name = "Black & White Cinema",
                    subtitle = "Monochrome silver gelatin with punchy midtones",
                    isCustom = false,
                    exposure = 0f,
                    contrast = 30f,
                    highlights = -10f,
                    shadows = -5f,
                    saturation = -100f,
                    vibrance = -100f,
                    temperature = 0f,
                    tint = 0f,
                    sharpness = 35f,
                    clarity = 35f,
                    aiDenoise = 30f,
                    aiSharpen = 35f,
                    aiUpscale = true,
                    targetResolution = "4K Ultra HD"
                )
            )
            for (p in defaults) {
                presetDao.insertPreset(p)
            }
        }
    }
}
