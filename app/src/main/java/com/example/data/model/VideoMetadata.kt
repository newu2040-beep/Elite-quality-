package com.example.data.model

import android.net.Uri

data class DeviceVideoItem(
    val uri: Uri,
    val displayName: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val dateModifiedSec: Long = 0L
) {
    val resolutionBadge: String
        get() = when {
            width >= 3800 || height >= 3800 -> "4K"
            width >= 1900 || height >= 1900 -> "1080p"
            width >= 1200 || height >= 1200 -> "720p"
            else -> "${width}p"
        }

    val formattedDuration: String
        get() {
            val sec = (durationMs / 1000) % 60
            val min = (durationMs / (1000 * 60)) % 60
            return String.format("%02d:%02d", min, sec)
        }

    val formattedSize: String
        get() {
            val mb = sizeBytes.toDouble() / (1024 * 1024)
            return String.format("%.1f MB", mb)
        }
}

data class VideoMetadata(
    val uriString: String,
    val fileName: String,
    val durationMs: Long = 0L,
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Float = 30f,
    val codec: String = "H.264 (AVC)",
    val bitrateBps: Long = 12_000_000L,
    val fileSizeBytes: Long = 0L,
    val audioCodec: String = "AAC",
    val audioSampleRate: Int = 48000,
    val audioChannels: Int = 2,
    val isHdr: Boolean = false,
    val colorSpace: String = "BT.709 Standard",
    val rotation: Int = 0,
    val dateCreated: String = ""
) {
    val resolutionLabel: String
        get() = when {
            width >= 3800 || height >= 3800 -> "4K UHD"
            width >= 2500 || height >= 2500 -> "2K QHD"
            width >= 1900 || height >= 1900 -> "1080p FHD"
            width >= 1200 || height >= 1200 -> "720p HD"
            else -> "${width}×${height}"
        }

    val formattedDuration: String
        get() {
            val totalSec = durationMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            val hours = min / 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, min % 60, sec)
            } else {
                String.format("%02d:%02d", min, sec)
            }
        }

    val formattedBitrate: String
        get() {
            val mbps = bitrateBps.toDouble() / 1_000_000.0
            return if (mbps > 0) String.format("%.1f Mbps", mbps) else "Auto Bitrate"
        }

    val formattedFileSize: String
        get() {
            val mb = fileSizeBytes.toDouble() / (1024 * 1024)
            return if (mb >= 1024) {
                String.format("%.2f GB", mb / 1024.0)
            } else {
                String.format("%.1f MB", mb)
            }
        }

    val aspectRatioLabel: String
        get() {
            if (width == 0 || height == 0) return "16:9"
            val gcd = gcd(width, height)
            val w = width / gcd
            val h = height / gcd
            return when {
                (w == 16 && h == 9) || (w == 9 && h == 16) -> "$w:$h"
                (w == 4 && h == 3) || (w == 3 && h == 4) -> "$w:$h"
                (w == 1 && h == 1) -> "1:1"
                (w == 21 && h == 9) -> "21:9"
                else -> {
                    val ratio = width.toFloat() / height.toFloat()
                    if (ratio in 1.7f..1.85f) "16:9"
                    else if (ratio in 0.54f..0.58f) "9:16"
                    else if (ratio in 1.3f..1.36f) "4:3"
                    else if (ratio in 0.78f..0.82f) "4:5"
                    else if (ratio in 0.98f..1.02f) "1:1"
                    else String.format("%.2f:1", ratio)
                }
            }
        }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}
