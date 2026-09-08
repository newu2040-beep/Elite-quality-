package com.example.data.repository

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File

data class StorageStats(
    val totalDeviceStorageBytes: Long,
    val availableDeviceStorageBytes: Long,
    val appCacheBytes: Long,
    val appExportsBytes: Long,
    val appTempBytes: Long
) {
    val availableStorageGbText: String
        get() {
            val gb = availableDeviceStorageBytes.toDouble() / (1024 * 1024 * 1024)
            return String.format("%.1f GB", gb)
        }

    val totalStorageGbText: String
        get() {
            val gb = totalDeviceStorageBytes.toDouble() / (1024 * 1024 * 1024)
            return String.format("%.1f GB", gb)
        }

    val appCacheMbText: String
        get() {
            val mb = appCacheBytes.toDouble() / (1024 * 1024)
            return String.format("%.1f MB", mb)
        }

    val appExportsMbText: String
        get() {
            val mb = appExportsBytes.toDouble() / (1024 * 1024)
            return String.format("%.1f MB", mb)
        }
}

class StorageRepository(private val context: Context) {

    fun getStorageStats(): StorageStats {
        val internalDir = Environment.getDataDirectory()
        val stat = StatFs(internalDir.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalDeviceStorage = totalBlocks * blockSize
        val availableDeviceStorage = availableBlocks * blockSize

        val cacheBytes = getDirectorySize(context.cacheDir)
        val exportsDir = File(context.filesDir, "exports")
        val exportsBytes = getDirectorySize(exportsDir)
        val tempDir = File(context.cacheDir, "temp")
        val tempBytes = getDirectorySize(tempDir)

        return StorageStats(
            totalDeviceStorageBytes = totalDeviceStorage,
            availableDeviceStorageBytes = availableDeviceStorage,
            appCacheBytes = cacheBytes,
            appExportsBytes = exportsBytes,
            appTempBytes = tempBytes
        )
    }

    fun clearCache(): Boolean {
        return deleteDirectoryContents(context.cacheDir)
    }

    fun deleteTempFiles(): Boolean {
        val tempDir = File(context.cacheDir, "temp")
        return deleteDirectoryContents(tempDir)
    }

    fun deleteExport(filePath: String): Boolean {
        val file = File(filePath)
        return if (file.exists()) file.delete() else false
    }

    private fun getDirectorySize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            size += if (f.isDirectory) getDirectorySize(f) else f.length()
        }
        return size
    }

    private fun deleteDirectoryContents(dir: File?): Boolean {
        if (dir == null || !dir.exists()) return true
        val files = dir.listFiles() ?: return true
        var allDeleted = true
        for (f in files) {
            if (f.isDirectory) {
                if (!deleteDirectoryContents(f) || !f.delete()) allDeleted = false
            } else {
                if (!f.delete()) allDeleted = false
            }
        }
        return allDeleted
    }
}
