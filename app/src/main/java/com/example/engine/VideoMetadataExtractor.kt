package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoMetadataExtractor(private val context: Context) {

    suspend fun extractMetadata(uri: Uri): VideoMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var fileName = "video_${System.currentTimeMillis()}.mp4"
        var fileSize = 0L

        try {
            // Get real filename and size from ContentResolver
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) fileName = name
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {}

        if (fileSize == 0L) {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    fileSize = pfd.statSize
                }
            } catch (_: Exception) {}
        }

        var durationMs = 0L
        var width = 1920
        var height = 1080
        var fps = 30f
        var bitrate = 0L
        var rotation = 0
        var audioCodec = "None"
        var audioSampleRate = 44100
        var audioChannels = 2
        var isHdr = false
        var colorSpace = "BT.709 Standard"
        var codec = "H.264 (AVC)"
        var dateFormatted = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date())

        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                retriever.setDataSource(pfd.fileDescriptor)
                pfd.close()
            } else {
                retriever.setDataSource(context, uri)
            }

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (!durationStr.isNullOrEmpty()) durationMs = durationStr.toLongOrNull() ?: 0L

            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            if (!widthStr.isNullOrEmpty()) width = widthStr.toIntOrNull() ?: 1920

            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            if (!heightStr.isNullOrEmpty()) height = heightStr.toIntOrNull() ?: 1080

            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            if (!rotationStr.isNullOrEmpty()) rotation = rotationStr.toIntOrNull() ?: 0

            val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            if (!bitrateStr.isNullOrEmpty()) bitrate = bitrateStr.toLongOrNull() ?: 0L

            val captureFpsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
            if (!captureFpsStr.isNullOrEmpty()) {
                val parsedFps = captureFpsStr.toFloatOrNull()
                if (parsedFps != null && parsedFps > 0f) fps = parsedFps
            }

            val mimeStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            if (!mimeStr.isNullOrEmpty()) {
                codec = when {
                    mimeStr.contains("hevc", ignoreCase = true) -> "H.265 (HEVC)"
                    mimeStr.contains("avc", ignoreCase = true) -> "H.264 (AVC)"
                    mimeStr.contains("vp9", ignoreCase = true) -> "VP9"
                    mimeStr.contains("av01", ignoreCase = true) -> "AV1"
                    else -> mimeStr.substringAfter("/")
                }
            }

            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            if (hasAudio == "yes") {
                audioCodec = "AAC Stereo"
            }

            // Real date from video metadata
            val dateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
            if (!dateStr.isNullOrEmpty()) {
                try {
                    // Formats like "20240315T103000.000Z" or "yyyyMMdd'T'HHmmss"
                    val parsedDate = if (dateStr.length >= 8) {
                        val year = dateStr.substring(0, 4)
                        val month = dateStr.substring(4, 6)
                        val day = dateStr.substring(6, 8)
                        "$year-$month-$day"
                    } else dateStr
                    dateFormatted = parsedDate
                } catch (_: Exception) {}
            }

            val colorStandard = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_STANDARD)
            val colorTransfer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_TRANSFER)
            if (colorTransfer == "6" || colorTransfer == "7" || (colorStandard != null && colorStandard == "6")) {
                isHdr = true
                colorSpace = "BT.2020 HDR (HLG/PQ)"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        // Secondary check with MediaExtractor to get precise track info
        try {
            val extractor = MediaExtractor()
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                extractor.setDataSource(pfd.fileDescriptor)
                pfd.close()
            } else {
                extractor.setDataSource(context, uri, null)
            }

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        fps = format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat()
                    }
                    if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE).toLong()
                    }
                } else if (mime.startsWith("audio/")) {
                    audioCodec = mime.substringAfter("/")
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        audioSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        audioChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                }
            }
            extractor.release()
        } catch (_: Exception) {}

        // Calculate real bitrate from filesize and duration if not provided in headers
        if (bitrate == 0L && durationMs > 0 && fileSize > 0) {
            bitrate = ((fileSize * 8) / (durationMs / 1000.0)).toLong()
        }

        val finalWidth = if (rotation == 90 || rotation == 270) height else width
        val finalHeight = if (rotation == 90 || rotation == 270) width else height

        VideoMetadata(
            uriString = uri.toString(),
            fileName = fileName,
            durationMs = durationMs,
            width = finalWidth,
            height = finalHeight,
            fps = fps,
            codec = codec,
            bitrateBps = bitrate,
            fileSizeBytes = fileSize,
            audioCodec = audioCodec,
            audioSampleRate = audioSampleRate,
            audioChannels = audioChannels,
            isHdr = isHdr,
            colorSpace = colorSpace,
            rotation = rotation,
            dateCreated = dateFormatted
        )
    }

    suspend fun extractThumbnail(uri: Uri): String? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                retriever.setDataSource(pfd.fileDescriptor)
                pfd.close()
            } else {
                retriever.setDataSource(context, uri)
            }
            val bitmap = retriever.getFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
            if (bitmap != null) {
                val thumbFile = File(context.cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
                FileOutputStream(thumbFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                return@withContext thumbFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
        null
    }
}
