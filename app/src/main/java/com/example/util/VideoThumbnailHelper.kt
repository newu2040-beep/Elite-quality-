package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VideoThumbnailHelper {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // 1/8th of available memory
    private val thumbnailCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    suspend fun getThumbnail(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val key = uri.toString()
        thumbnailCache.get(key)?.let { return@withContext it }

        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                retriever.setDataSource(pfd.fileDescriptor)
                pfd.close()
            } else {
                retriever.setDataSource(context, uri)
            }

            // Extract frame at 1 second or first frame
            val rawBitmap = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST)

            if (rawBitmap != null) {
                // Scale down to max 320px width/height for fast UI rendering
                val maxDim = 320
                val scale = minOf(1.0f, maxDim.toFloat() / maxOf(rawBitmap.width, rawBitmap.height))
                val targetW = (rawBitmap.width * scale).toInt().coerceAtLeast(1)
                val targetH = (rawBitmap.height * scale).toInt().coerceAtLeast(1)

                val scaled = if (scale < 1.0f) {
                    Bitmap.createScaledBitmap(rawBitmap, targetW, targetH, true).also {
                        if (it != rawBitmap) rawBitmap.recycle()
                    }
                } else {
                    rawBitmap
                }

                thumbnailCache.put(key, scaled)
                scaled
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            try { retriever?.release() } catch (_: Exception) {}
        }
    }
}
