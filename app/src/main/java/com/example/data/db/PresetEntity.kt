package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val subtitle: String = "",
    val isCustom: Boolean = true,
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val saturation: Float = 0f,
    val vibrance: Float = 0f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val sharpness: Float = 25f,
    val clarity: Float = 15f,
    val aiDenoise: Float = 40f,
    val aiSharpen: Float = 35f,
    val aiUpscale: Boolean = true,
    val targetResolution: String = "4K Ultra HD",
    val targetBitrate: String = "High",
    val createdAt: Long = System.currentTimeMillis()
)
