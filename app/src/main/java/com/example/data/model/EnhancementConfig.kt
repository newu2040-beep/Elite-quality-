package com.example.data.model

enum class EnhancementIntensity {
    SUBTLE,
    BALANCED,
    STRONG
}

enum class ExportResolution(val label: String, val width: Int, val height: Int) {
    ORIGINAL("Original", 0, 0),
    RES_720P("720p HD", 1280, 720),
    RES_1080P("1080p Full HD", 1920, 1080),
    RES_1440P("1440p Quad HD", 2560, 1440),
    RES_2K("2K DCI", 2048, 1080),
    RES_4K("4K Ultra HD", 3840, 2160)
}

enum class ExportFps(val label: String, val fps: Int) {
    ORIGINAL("Original", 0),
    FPS_24("24 FPS (Cinema)", 24),
    FPS_25("25 FPS (PAL)", 25),
    FPS_30("30 FPS (Standard)", 30),
    FPS_50("50 FPS", 50),
    FPS_60("60 FPS (Smooth)", 60)
}

enum class ExportBitrate(val label: String, val mbps: Float) {
    EFFICIENT("Efficient (6 Mbps)", 6f),
    STANDARD("Standard (12 Mbps)", 12f),
    HIGH("High (24 Mbps)", 24f),
    VERY_HIGH("Very High (45 Mbps)", 45f),
    MAXIMUM("Maximum (80 Mbps)", 80f),
    CUSTOM("Custom", 20f)
}

enum class ExportCodec(val label: String, val mimeType: String) {
    H264("H.264 / AVC (Most Compatible)", "video/avc"),
    H265("H.265 / HEVC (High Efficiency)", "video/hevc"),
    AV1("AV1 Next-Gen (If Supported)", "video/av01")
}

enum class ProcessingQuality(val label: String, val passes: Int) {
    FAST("Fast (1 Pass)", 1),
    BALANCED("Balanced (2 Passes)", 2),
    HIGH_QUALITY("High Quality (3 Passes)", 3),
    MAXIMUM_QUALITY("Maximum Quality (Deep AI)", 4)
}

data class EnhancementConfig(
    // AI Modules
    val aiUpscale: Boolean = true,
    val aiDenoise: Float = 45f,         // 0 to 100
    val aiSharpen: Float = 35f,         // 0 to 100
    val aiDeblur: Float = 30f,          // 0 to 100
    val faceEnhance: Boolean = true,
    val faceEnhanceAmount: Float = 40f, // 0 to 100
    val artifactRemoval: Float = 50f,   // 0 to 100 (compression deblocking)
    val lowLightEnhance: Float = 25f,   // 0 to 100
    val intensity: EnhancementIntensity = EnhancementIntensity.BALANCED,

    // Export & Encoding
    val targetResolution: ExportResolution = ExportResolution.RES_1080P,
    val targetFps: ExportFps = ExportFps.ORIGINAL,
    val targetBitrate: ExportBitrate = ExportBitrate.HIGH,
    val customBitrateMbps: Float = 25f,
    val targetCodec: ExportCodec = ExportCodec.H264,
    val quality: ProcessingQuality = ProcessingQuality.HIGH_QUALITY,

    // Audio
    val preserveAudio: Boolean = true,
    val muteAudio: Boolean = false,

    // Transform & Crop
    val cropRatio: String = "Original", // Original, 16:9, 9:16, 4:5, 1:1, 4:3
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val playbackSpeed: Float = 1.0f
)
