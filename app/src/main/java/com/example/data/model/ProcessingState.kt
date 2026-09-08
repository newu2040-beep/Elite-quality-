package com.example.data.model

enum class ProcessingStage(val displayName: String) {
    IDLE("Ready"),
    PREPARING("Preparing Engine..."),
    ANALYZING("Analyzing Footage & Noise Profile..."),
    DETAIL_RECOVERY("Detail Recovery & Deblurring..."),
    NOISE_REDUCTION("AI Denoising & Deblocking..."),
    COLOR_OPTIMIZATION("Color & Dynamic Range Optimization..."),
    AI_ENHANCEMENT("Neural Super-Resolution & Upscaling..."),
    ENCODING("Hardware Video Encoding..."),
    FINALIZING("Muxing Audio & Finalizing MP4..."),
    COMPLETED("Enhancement Complete"),
    CANCELLED("Operation Cancelled"),
    FAILED("Processing Interrupted")
}

data class ProcessingProgress(
    val stage: ProcessingStage = ProcessingStage.IDLE,
    val progressPercent: Int = 0,
    val currentFrame: Long = 0L,
    val totalFrames: Long = 0L,
    val estimatedRemainingSeconds: Long = 0L,
    val inputResolutionText: String = "",
    val outputResolutionText: String = "",
    val estimatedStorageMb: Long = 0L,
    val thermalStatus: String = "Optimal (32°C)",
    val isFinished: Boolean = false,
    val outputFileUri: String? = null,
    val outputFilePath: String? = null,
    val errorMessage: String? = null
)
