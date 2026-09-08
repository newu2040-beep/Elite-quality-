package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exports")
data class ExportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long = 0L,
    val title: String,
    val outputUri: String,
    val outputFilePath: String,
    val resolution: String,
    val fps: Int,
    val codec: String,
    val bitrateMbps: Float,
    val fileSizeBytes: Long,
    val timestamp: Long = System.currentTimeMillis()
)
