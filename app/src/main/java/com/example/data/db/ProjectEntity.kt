package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceUri: String,
    val thumbnailPath: String? = null,
    val durationMs: Long = 0L,
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Float = 30f,
    val codec: String = "H.264",
    val bitrateBps: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val enhancementPreset: String = "Cinematic 4K",
    val status: String = "Draft", // Draft, Enhanced, Exported
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastExportUri: String? = null,
    val lastExportPath: String? = null
)
