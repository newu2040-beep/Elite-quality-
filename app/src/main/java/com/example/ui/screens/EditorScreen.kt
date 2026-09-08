package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PresetEntity
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProcessing: () -> Unit,
    onNavigateToExport: () -> Unit
) {
    val context = LocalContext.current
    val videoUri by viewModel.currentVideoUri.collectAsState()
    val metadata by viewModel.currentMetadata.collectAsState()
    val colorAdjustment by viewModel.colorAdjustment.collectAsState()
    val config by viewModel.enhancementConfig.collectAsState()
    val comparisonMode by viewModel.comparisonMode.collectAsState()
    val splitFraction by viewModel.splitFraction.collectAsState()
    val activeCategory by viewModel.activeEditingCategory.collectAsState()
    val allPresets by viewModel.allPresets.collectAsState()

    var showInfoCard by remember { mutableStateOf(false) }
    var showEnhanceConfigDialog by remember { mutableStateOf(false) }

    val categories = listOf(
        "Adjust", "Color", "Curves", "Wheels", "Detail", "AI", "Presets", "Crop", "Speed", "Audio"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = metadata?.fileName ?: "Editing Video",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        metadata?.let {
                            Text(
                                text = "${it.resolutionLabel} • ${it.fps.toInt()} FPS • ${it.codec}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    // Info Toggle
                    IconButton(onClick = { showInfoCard = !showInfoCard }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Video Specs",
                            tint = if (showInfoCard) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Comparison Mode Cycle
                    IconButton(onClick = {
                        val nextMode = when (comparisonMode) {
                            ComparisonMode.SPLIT -> ComparisonMode.ENHANCED
                            ComparisonMode.ENHANCED -> ComparisonMode.ORIGINAL
                            ComparisonMode.ORIGINAL -> ComparisonMode.SPLIT
                        }
                        viewModel.setComparisonMode(nextMode)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = "Toggle Comparison",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Save / Export
                    TextButton(
                        onClick = { showEnhanceConfigDialog = true },
                        modifier = Modifier.testTag("editor_export_top_button")
                    ) {
                        Text(
                            text = "Export",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            val compact = LocalCompactUiConfig.current
            // Bottom Action Bar: Signature ELITE ENHANCEMENT CTA
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (compact.isCompact) 12.dp else 16.dp,
                            vertical = if (compact.isCompact) 6.dp else 10.dp
                        )
                ) {
                    Button(
                        onClick = { showEnhanceConfigDialog = true },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(compact.bottomBarButtonHeight)
                            .testTag("elite_enhancement_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(if (compact.isCompact) 18.dp else 20.dp)
                        )
                        Spacer(modifier = Modifier.width(if (compact.isCompact) 6.dp else 10.dp))
                        Text(
                            text = "ELITE ENHANCEMENT",
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = if (compact.isCompact) 0.8.sp else 1.2.sp,
                            fontSize = if (compact.isCompact) 13.sp else 15.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val compact = LocalCompactUiConfig.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Video Preview Viewport (Flexible Height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
                    .background(Color.Black)
            ) {
                VideoPlayerView(
                    videoUri = videoUri,
                    colorAdjustment = colorAdjustment,
                    comparisonMode = comparisonMode,
                    splitFraction = splitFraction,
                    onSplitFractionChange = { viewModel.setSplitFraction(it) },
                    modifier = Modifier.fillMaxSize()
                )

                // Expandable Video Information Card Overlay
                if (showInfoCard) {
                    metadata?.let { meta ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(if (compact.isCompact) 8.dp else 12.dp)
                                .fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(if (compact.isCompact) 10.dp else 14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Video Specifications",
                                        style = if (compact.isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(
                                        onClick = { showInfoCard = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    InfoBadge(text = meta.resolutionLabel, isHighlight = true)
                                    InfoBadge(text = "${meta.fps.toInt()} FPS")
                                    InfoBadge(text = meta.codec)
                                    InfoBadge(text = meta.formattedBitrate)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Dimensions: ${meta.width} × ${meta.height} • Ratio: ${meta.aspectRatioLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = if (compact.isCompact) 11.sp else 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Duration: ${meta.formattedDuration} • Size: ${meta.formattedFileSize}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = if (compact.isCompact) 11.sp else 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Audio: ${meta.audioCodec} • Color: ${meta.colorSpace}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = if (compact.isCompact) 11.sp else 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Category Tabs (Horizontal Pill Selector)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(
                        horizontal = if (compact.isCompact) 8.dp else 12.dp,
                        vertical = if (compact.isCompact) 4.dp else 8.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(if (compact.isCompact) 6.dp else 8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = activeCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { viewModel.setActiveCategory(category) }
                            .padding(
                                horizontal = compact.chipPaddingH,
                                vertical = compact.chipPaddingV
                            )
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = if (compact.isCompact) 11.sp else 12.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.8.dp)

            // Contextual Controls Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(compact.editorPanelHeight)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                when (activeCategory) {
                    "Adjust" -> AdjustPanel(colorAdjustment, viewModel, compact.sliderVerticalPadding)
                    "Color" -> ColorPanel(colorAdjustment, viewModel, compact.sliderVerticalPadding)
                    "Curves" -> CurvesPanel(colorAdjustment, viewModel, compact.curveGraphHeight)
                    "Wheels" -> WheelsPanel(colorAdjustment, viewModel, compact.wheelSize)
                    "Detail" -> DetailPanel(colorAdjustment, viewModel, compact.sliderVerticalPadding)
                    "AI" -> AiEnhancePanel(config, viewModel, compact.sliderVerticalPadding)
                    "Presets" -> PresetsPanel(allPresets, viewModel, compact.isCompact)
                    "Crop" -> CropPanel(config, viewModel)
                    "Speed" -> SpeedPanel(config, viewModel)
                    "Audio" -> AudioPanel(metadata, config, viewModel)
                }
            }
        }
    }

    // Pre-Enhancement Configuration Modal
    if (showEnhanceConfigDialog) {
        EnhanceConfigModal(
            metadata = metadata,
            config = config,
            onDismiss = { showEnhanceConfigDialog = false },
            onUpdateConfig = { update -> viewModel.updateEnhancementConfig(update) },
            onStart = {
                showEnhanceConfigDialog = false
                onNavigateToProcessing()
                viewModel.startEliteEnhancement {
                    onNavigateToExport()
                }
            }
        )
    }
}

@Composable
fun AdjustPanel(adj: ColorAdjustment, viewModel: MainViewModel, sliderPadding: androidx.compose.ui.unit.Dp = 4.dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        ValueSlider("Exposure", adj.exposure, { viewModel.updateColorAdjustment { c -> c.copy(exposure = it) } }, verticalPadding = sliderPadding)
        ValueSlider("Brightness", adj.brightness, { viewModel.updateColorAdjustment { c -> c.copy(brightness = it) } }, verticalPadding = sliderPadding)
        ValueSlider("Contrast", adj.contrast, { viewModel.updateColorAdjustment { c -> c.copy(contrast = it) } }, verticalPadding = sliderPadding)
        ValueSlider("Highlights", adj.highlights, { viewModel.updateColorAdjustment { c -> c.copy(highlights = it) } }, verticalPadding = sliderPadding)
        ValueSlider("Shadows", adj.shadows, { viewModel.updateColorAdjustment { c -> c.copy(shadows = it) } }, verticalPadding = sliderPadding)
        ValueSlider("Whites", adj.whites, { viewModel.updateColorAdjustment { c -> c.copy(whites = it) } }, verticalPadding = sliderPadding)
        ValueSlider("Blacks", adj.blacks, { viewModel.updateColorAdjustment { c -> c.copy(blacks = it) } }, verticalPadding = sliderPadding)
        ValueSlider("Saturation", adj.saturation, { viewModel.updateColorAdjustment { c -> c.copy(saturation = it) } }, verticalPadding = sliderPadding)
        ValueSlider("Vibrance", adj.vibrance, { viewModel.updateColorAdjustment { c -> c.copy(vibrance = it) } }, verticalPadding = sliderPadding)
        ValueSlider("Fade", adj.fade, { viewModel.updateColorAdjustment { c -> c.copy(fade = it) } }, valueRange = 0f..100f, verticalPadding = sliderPadding)
    }
}

@Composable
fun ColorPanel(adj: ColorAdjustment, viewModel: MainViewModel, sliderPadding: androidx.compose.ui.unit.Dp = 4.dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        ValueSlider("Temperature", adj.temperature, { viewModel.updateColorAdjustment { c -> c.copy(temperature = it) } }, verticalPadding = sliderPadding)
        ValueSlider("Tint", adj.tint, { viewModel.updateColorAdjustment { c -> c.copy(tint = it) } }, verticalPadding = sliderPadding)

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.resetColorAdjustment() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(42.dp)
        ) {
            Text("Reset Color Balance", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
        }
    }
}

@Composable
fun CurvesPanel(adj: ColorAdjustment, viewModel: MainViewModel, graphHeight: androidx.compose.ui.unit.Dp = 130.dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        CurvesGraphView(
            curveValue = adj.curveMaster,
            onCurveValueChange = { viewModel.updateColorAdjustment { c -> c.copy(curveMaster = it) } },
            channelColor = Color.White,
            channelName = "RGB Master Curve",
            graphHeight = graphHeight
        )
    }
}

@Composable
fun WheelsPanel(adj: ColorAdjustment, viewModel: MainViewModel, wheelSize: androidx.compose.ui.unit.Dp = 100.dp) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ColorWheelView(
            title = "Shadows",
            hue = adj.shadowTintHue,
            amount = adj.shadowTintAmount,
            onHueAmountChange = { h, a ->
                viewModel.updateColorAdjustment { it.copy(shadowTintHue = h, shadowTintAmount = a) }
            },
            wheelSize = wheelSize
        )
        ColorWheelView(
            title = "Midtones",
            hue = adj.midtoneTintHue,
            amount = adj.midtoneTintAmount,
            onHueAmountChange = { h, a ->
                viewModel.updateColorAdjustment { it.copy(midtoneTintHue = h, midtoneTintAmount = a) }
            },
            wheelSize = wheelSize
        )
        ColorWheelView(
            title = "Highlights",
            hue = adj.highlightTintHue,
            amount = adj.highlightTintAmount,
            onHueAmountChange = { h, a ->
                viewModel.updateColorAdjustment { it.copy(highlightTintHue = h, highlightTintAmount = a) }
            },
            wheelSize = wheelSize
        )
    }
}

@Composable
fun DetailPanel(adj: ColorAdjustment, viewModel: MainViewModel, sliderPadding: androidx.compose.ui.unit.Dp = 4.dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        ValueSlider("Sharpness", adj.sharpness, { viewModel.updateColorAdjustment { c -> c.copy(sharpness = it) } }, valueRange = 0f..100f, verticalPadding = sliderPadding)
        ValueSlider("Clarity", adj.clarity, { viewModel.updateColorAdjustment { c -> c.copy(clarity = it) } }, valueRange = 0f..100f, verticalPadding = sliderPadding)
    }
}

@Composable
fun AiEnhancePanel(cfg: EnhancementConfig, viewModel: MainViewModel, sliderPadding: androidx.compose.ui.unit.Dp = 4.dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("AI Neural Upscale", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("Super-resolution edge reconstruction", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = cfg.aiUpscale,
                onCheckedChange = { viewModel.updateEnhancementConfig { c -> c.copy(aiUpscale = it) } }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        ValueSlider("AI Denoise", cfg.aiDenoise, { viewModel.updateEnhancementConfig { c -> c.copy(aiDenoise = it) } }, valueRange = 0f..100f, verticalPadding = sliderPadding)
        ValueSlider("AI Sharpen", cfg.aiSharpen, { viewModel.updateEnhancementConfig { c -> c.copy(aiSharpen = it) } }, valueRange = 0f..100f, verticalPadding = sliderPadding)
        ValueSlider("AI Deblur", cfg.aiDeblur, { viewModel.updateEnhancementConfig { c -> c.copy(aiDeblur = it) } }, valueRange = 0f..100f, verticalPadding = sliderPadding)
        ValueSlider("Artifact Removal", cfg.artifactRemoval, { viewModel.updateEnhancementConfig { c -> c.copy(artifactRemoval = it) } }, valueRange = 0f..100f, verticalPadding = sliderPadding)
        ValueSlider("Low-Light Enhance", cfg.lowLightEnhance, { viewModel.updateEnhancementConfig { c -> c.copy(lowLightEnhance = it) } }, valueRange = 0f..100f, verticalPadding = sliderPadding)
    }
}

@Composable
fun PresetsPanel(presets: List<PresetEntity>, viewModel: MainViewModel, isCompact: Boolean = false) {
    LazyRow(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isCompact) 8.dp else 14.dp),
        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)
    ) {
        items(presets, key = { it.id }) { preset ->
            Box(
                modifier = Modifier
                    .width(if (isCompact) 130.dp else 160.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .clickable { viewModel.applyPreset(preset) }
                    .padding(if (isCompact) 8.dp else 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                    Column {
                        Text(preset.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, fontSize = if (isCompact) 13.sp else 14.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(preset.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, fontSize = if (isCompact) 10.sp else 11.sp)
                    }
                    InfoBadge(text = preset.targetResolution, isHighlight = true)
                }
            }
        }
    }
}

@Composable
fun CropPanel(cfg: EnhancementConfig, viewModel: MainViewModel) {
    val ratios = listOf("Original", "16:9", "9:16", "4:5", "1:1", "4:3")
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Target Aspect Ratio", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ratios.forEach { r ->
                val isSelected = cfg.cropRatio == r
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.updateEnhancementConfig { c -> c.copy(cropRatio = r) } },
                    label = { Text(r) }
                )
            }
        }
    }
}

@Composable
fun SpeedPanel(cfg: EnhancementConfig, viewModel: MainViewModel) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 4.0f)
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Playback & Processing Speed", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            speeds.forEach { s ->
                val isSelected = cfg.playbackSpeed == s
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.updateEnhancementConfig { c -> c.copy(playbackSpeed = s) } },
                    label = { Text("${s}x") }
                )
            }
        }
    }
}

@Composable
fun AudioPanel(metadata: VideoMetadata?, cfg: EnhancementConfig, viewModel: MainViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Audio Passthrough & Controls", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        metadata?.let {
            Text("Detected Track: ${it.audioCodec} • ${it.audioSampleRate} Hz", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Preserve Original Audio")
            Switch(
                checked = cfg.preserveAudio,
                onCheckedChange = { viewModel.updateEnhancementConfig { c -> c.copy(preserveAudio = it) } }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mute Audio Track")
            Switch(
                checked = cfg.muteAudio,
                onCheckedChange = { viewModel.updateEnhancementConfig { c -> c.copy(muteAudio = it) } }
            )
        }
    }
}

@Composable
fun EnhanceConfigModal(
    metadata: VideoMetadata?,
    config: EnhancementConfig,
    onDismiss: () -> Unit,
    onUpdateConfig: ((EnhancementConfig) -> EnhancementConfig) -> Unit,
    onStart: () -> Unit
) {
    val compact = LocalCompactUiConfig.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "ELITE ENHANCEMENT",
                    style = if (compact.isCompact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Configure offline hardware & AI processing",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = if (compact.isCompact) 11.sp else 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (compact.isCompact) 310.dp else 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Quality Safety Advisory
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(if (compact.isCompact) 8.dp else 12.dp)
                ) {
                    Text(
                        text = "Safety Note: Enhancement improves perceived detail, clarity, and removes noise, but cannot recover data that was never captured.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(if (compact.isCompact) 8.dp else 14.dp))

                // Target Resolution
                Text("Target Resolution", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportResolution.values().forEach { res ->
                        FilterChip(
                            selected = config.targetResolution == res,
                            onClick = { onUpdateConfig { it.copy(targetResolution = res) } },
                            label = { Text(res.label, fontSize = if (compact.isCompact) 11.sp else 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (compact.isCompact) 8.dp else 14.dp))

                // Target FPS
                Text("Target FPS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFps.values().forEach { fps ->
                        FilterChip(
                            selected = config.targetFps == fps,
                            onClick = { onUpdateConfig { it.copy(targetFps = fps) } },
                            label = { Text(fps.label, fontSize = if (compact.isCompact) 11.sp else 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (compact.isCompact) 8.dp else 14.dp))

                // Target Codec
                Text("Encoding Codec", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportCodec.values().forEach { c ->
                        FilterChip(
                            selected = config.targetCodec == c,
                            onClick = { onUpdateConfig { it.copy(targetCodec = c) } },
                            label = { Text(c.label, fontSize = if (compact.isCompact) 11.sp else 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (compact.isCompact) 8.dp else 14.dp))

                // Target Bitrate
                Text("Bitrate Preset", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportBitrate.values().forEach { b ->
                        FilterChip(
                            selected = config.targetBitrate == b,
                            onClick = { onUpdateConfig { it.copy(targetBitrate = b) } },
                            label = { Text(b.label, fontSize = if (compact.isCompact) 11.sp else 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (compact.isCompact) 8.dp else 14.dp))

                // Processing Intensity
                Text("AI Intensity", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EnhancementIntensity.values().forEach { intens ->
                        FilterChip(
                            selected = config.intensity == intens,
                            onClick = { onUpdateConfig { it.copy(intensity = intens) } },
                            label = { Text(intens.name.lowercase().capitalize(), fontSize = if (compact.isCompact) 11.sp else 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onStart,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("start_processing_button")
            ) {
                Text("Start Local Processing", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
