package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*

data class SocialMediaPreset(
    val id: String,
    val platformName: String,
    val formatName: String,
    val icon: ImageVector,
    val badge: String,
    val resolutionText: String,
    val targetResolution: ExportResolution,
    val targetFps: ExportFps,
    val targetBitrate: ExportBitrate,
    val targetCodec: ExportCodec,
    val cropRatio: String,
    val tip: String
)

val SOCIAL_MEDIA_PRESETS = listOf(
    SocialMediaPreset(
        id = "ig_reels",
        platformName = "Instagram",
        formatName = "Reels & Stories (9:16)",
        icon = Icons.Default.CameraAlt,
        badge = "Most Popular",
        resolutionText = "1080 × 1920",
        targetResolution = ExportResolution.RES_1080P,
        targetFps = ExportFps.FPS_60,
        targetBitrate = ExportBitrate.HIGH, // 24 Mbps
        targetCodec = ExportCodec.H264,
        cropRatio = "9:16",
        tip = "Instagram re-compresses videos above 25 Mbps. 18-24 Mbps with H.264 High Profile ensures zero compression pixelation."
    ),
    SocialMediaPreset(
        id = "yt_4k",
        platformName = "YouTube",
        formatName = "4K Ultra HD Cinema (16:9)",
        icon = Icons.Default.SmartDisplay,
        badge = "Highest Quality",
        resolutionText = "3840 × 2160",
        targetResolution = ExportResolution.RES_4K,
        targetFps = ExportFps.FPS_60,
        targetBitrate = ExportBitrate.VERY_HIGH, // 45 Mbps
        targetCodec = ExportCodec.H264,
        cropRatio = "16:9",
        tip = "Uploading in 4K forces YouTube's high-tier VP9/AV1 transcoding algorithm even when viewed on 1080p screens."
    ),
    SocialMediaPreset(
        id = "yt_shorts",
        platformName = "YouTube Shorts",
        formatName = "Shorts HDR (9:16)",
        icon = Icons.Default.PlayCircle,
        badge = "60 FPS HDR",
        resolutionText = "1080 × 1920",
        targetResolution = ExportResolution.RES_1080P,
        targetFps = ExportFps.FPS_60,
        targetBitrate = ExportBitrate.HIGH,
        targetCodec = ExportCodec.H264,
        cropRatio = "9:16",
        tip = "60 FPS delivers silky-smooth motion for fast transitions and gameplay clips."
    ),
    SocialMediaPreset(
        id = "tiktok_hd",
        platformName = "TikTok",
        formatName = "TikTok HD Pro (9:16)",
        icon = Icons.Default.MovieFilter,
        badge = "Trending",
        resolutionText = "1080 × 1920",
        targetResolution = ExportResolution.RES_1080P,
        targetFps = ExportFps.FPS_60,
        targetBitrate = ExportBitrate.HIGH,
        targetCodec = ExportCodec.H264,
        cropRatio = "9:16",
        tip = "Disable TikTok's in-app 'Data Saver' on upload to preserve the original enhanced clarity."
    ),
    SocialMediaPreset(
        id = "x_cinema",
        platformName = "X / Twitter",
        formatName = "HD Video Post",
        icon = Icons.Default.Share,
        badge = "Crisp Text",
        resolutionText = "1080 × 1920 / 1920 × 1080",
        targetResolution = ExportResolution.RES_1080P,
        targetFps = ExportFps.FPS_30,
        targetBitrate = ExportBitrate.STANDARD, // 12 Mbps
        targetCodec = ExportCodec.H264,
        cropRatio = "16:9",
        tip = "1080p @ 30 FPS / 12 Mbps guarantees fast feed autoplay without buffer stutters."
    ),
    SocialMediaPreset(
        id = "wa_lossless",
        platformName = "WhatsApp / Telegram",
        formatName = "HD Document Mode",
        icon = Icons.Default.Send,
        badge = "Efficient",
        resolutionText = "1080p / 720p",
        targetResolution = ExportResolution.RES_1080P,
        targetFps = ExportFps.FPS_30,
        targetBitrate = ExportBitrate.STANDARD,
        targetCodec = ExportCodec.H264,
        cropRatio = "Original",
        tip = "Send as 'Document' or enable WhatsApp 'HD' upload toggle to prevent double-compression."
    )
)

@Composable
fun SocialMediaAndCapabilitiesDialog(
    currentMetadata: VideoMetadata?,
    onDismiss: () -> Unit,
    onApplySocialPreset: (SocialMediaPreset) -> Unit
) {
    val compact = LocalCompactUiConfig.current
    var activeTab by remember { mutableStateOf(0) } // 0 = Social Presets, 1 = Device & Video Specs

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (activeTab == 0) "Social Upload Presets" else "Supported Video Specs",
                        style = if (compact.isCompact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    InfoBadge(text = "OFFLINE ENGINE", isHighlight = true)
                }
                Spacer(modifier = Modifier.height(6.dp))
                // Tab switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { activeTab = 0 }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Best Social Settings",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (activeTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTab == 1) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { activeTab = 1 }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Video & Specs",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (activeTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (activeTab == 0) {
                    // Social Upload Recommendations
                    Text(
                        text = "Tap any platform preset to auto-apply optimal resolution, bitrate, FPS & codec for maximum sharpness without platform compression loss:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    SOCIAL_MEDIA_PRESETS.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clickable {
                                    onApplySocialPreset(preset)
                                    onDismiss()
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = preset.icon,
                                                contentDescription = preset.platformName,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = preset.platformName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = preset.formatName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    InfoBadge(text = preset.badge, isHighlight = true)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    InfoBadge(text = preset.resolutionText)
                                    InfoBadge(text = "${preset.targetFps.fps} FPS")
                                    InfoBadge(text = "${preset.targetBitrate.mbps.toInt()} Mbps")
                                    InfoBadge(text = preset.targetCodec.label.split(" ")[0])
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = preset.tip,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Apply Settings →",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Video & Device Capabilities
                    if (currentMetadata != null) {
                        Text("Active Video Inspection", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                SpecRow("File Name:", currentMetadata.fileName)
                                SpecRow("Resolution:", "${currentMetadata.width} × ${currentMetadata.height} (${currentMetadata.resolutionLabel})")
                                SpecRow("Frame Rate:", "${currentMetadata.fps.toInt()} FPS")
                                SpecRow("Video Bitrate:", "${(currentMetadata.bitrateBps / 1_000_000.0).let { String.format("%.2f", it) }} Mbps")
                                SpecRow("Video Codec:", currentMetadata.codec)
                                SpecRow("Color Space:", currentMetadata.colorSpace)
                                SpecRow("Audio Codec:", "${currentMetadata.audioCodec} • ${currentMetadata.audioSampleRate} Hz • ${currentMetadata.audioChannels} ch")
                                SpecRow("Duration:", currentMetadata.formattedDuration)
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Text("Device Hardware Capabilities", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SpecRow("Supported Resolutions:", "4K UHD, 2K QHD, 1080p FHD, 720p HD")
                            SpecRow("Supported Frame Rates:", "24 FPS, 25 FPS, 30 FPS, 50 FPS, 60 FPS")
                            SpecRow("Bitrate Output Range:", "4 Mbps (Efficient) to 80 Mbps (Master Pro)")
                            SpecRow("Hardware Acceleration:", "OpenGL ES 2.0 / 3.0 + SurfaceTexture EGL")
                            SpecRow("Hardware Codecs:", "MediaCodec H.264 (AVC High Profile) & H.265 (HEVC)")
                            SpecRow("Color Grading Engine:", "16 3D Studio LUTs, RGB Curves, Real-Time GPU Shaders")
                            SpecRow("AI Processing Modules:", "Super-Resolution, Denoising, Deblur, Clarity, HDR Tone")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(50)
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}
