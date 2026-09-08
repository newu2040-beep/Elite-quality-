package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PresetEntity
import com.example.data.model.*
import com.example.engine.AdjustmentSnapshot
import com.example.ui.components.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

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

    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val undoStack by viewModel.undoStack.collectAsState()
    val redoStack by viewModel.redoStack.collectAsState()

    var showInfoCard by remember { mutableStateOf(false) }
    var showEnhanceConfigDialog by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }

    val categories = listOf(
        "Adjust", "LUTs", "Color", "HSL", "RGB", "Curves", "Wheels", "Detail", "AI", "Presets", "Crop", "Speed", "Audio"
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
                    // GPU Pipeline Undo Action
                    IconButton(
                        onClick = { viewModel.undoAdjustment() },
                        enabled = canUndo,
                        modifier = Modifier.testTag("editor_undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Undo,
                            contentDescription = "Undo GPU Adjustment",
                            tint = if (canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                    }

                    // GPU Pipeline Redo Action
                    IconButton(
                        onClick = { viewModel.redoAdjustment() },
                        enabled = canRedo,
                        modifier = Modifier.testTag("editor_redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Redo,
                            contentDescription = "Redo GPU Adjustment",
                            tint = if (canRedo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                    }

                    // Adjustment History Timeline
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier.testTag("editor_history_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (undoStack.size > 1) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text("${undoStack.size - 1}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = "Adjustment History Log",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

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
                    enhancementConfig = config,
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
                                        style = MaterialTheme.typography.titleSmall,
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
                    "LUTs" -> LutsPanel(colorAdjustment, viewModel)
                    "Color" -> ColorPanel(colorAdjustment, viewModel, compact.sliderVerticalPadding)
                    "HSL" -> HslPanel(colorAdjustment, viewModel, compact.sliderVerticalPadding)
                    "RGB" -> RgbPanel(colorAdjustment, viewModel, compact.sliderVerticalPadding)
                    "Curves" -> CurvesPanel(colorAdjustment, viewModel, compact.curveGraphHeight)
                    "Wheels" -> WheelsPanel(colorAdjustment, viewModel, compact.wheelSize)
                    "Detail" -> DetailPanel(colorAdjustment, config, viewModel, compact.sliderVerticalPadding)
                    "AI" -> AiEnhancePanel(config, viewModel, compact.sliderVerticalPadding)
                    "Presets" -> PresetsPanel(allPresets, viewModel, compact.isCompact)
                    "Crop" -> CropPanel(config, viewModel)
                    "Speed" -> SpeedPanel(config, viewModel)
                    "Audio" -> AudioPanel(metadata, config, viewModel)
                }
            }
        }
    }

    // Adjustment History Modal Bottom Sheet
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "GPU Adjustment History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Revert or jump back to any step in real-time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(onClick = { viewModel.resetColorAdjustment() }) {
                        Text("Reset All", color = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Parameter Revert Chips
                Text("Revert Specific Parameter", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val revertableParams = listOf(
                        "Exposure" to "exposure",
                        "Contrast" to "contrast",
                        "Saturation" to "saturation",
                        "LUT" to "lut",
                        "Temperature" to "temperature",
                        "Curves" to "curves",
                        "Color Wheels" to "colorwheels"
                    )
                    revertableParams.forEach { (label, key) ->
                        OutlinedButton(
                            onClick = {
                                viewModel.revertParameter(key)
                                Toast.makeText(context, "Reverted $label to default", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(label, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Text("Timeline (${undoStack.size} Steps)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(undoStack.reversed()) { revIdx, snapshot ->
                        val originalIndex = undoStack.size - 1 - revIdx
                        val isCurrent = revIdx == 0
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.jumpToHistoryStep(originalIndex)
                                    showHistorySheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = snapshot.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = timeFormat.format(Date(snapshot.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isCurrent) {
                                    InfoBadge(text = "Active State", isHighlight = true)
                                } else {
                                    Text(
                                        text = "Jump",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
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
        ValueSlider(
            "Exposure",
            adj.exposure,
            { viewModel.updateColorAdjustment(paramName = "exposure", description = "Exposure: ${it.toInt()}%") { c -> c.copy(exposure = it) } },
            verticalPadding = sliderPadding
        )
        ValueSlider(
            "Brightness",
            adj.brightness,
            { viewModel.updateColorAdjustment(paramName = "brightness", description = "Brightness: ${it.toInt()}%") { c -> c.copy(brightness = it) } },
            verticalPadding = sliderPadding
        )
        ValueSlider(
            "Contrast",
            adj.contrast,
            { viewModel.updateColorAdjustment(paramName = "contrast", description = "Contrast: ${it.toInt()}%") { c -> c.copy(contrast = it) } },
            verticalPadding = sliderPadding
        )
        ValueSlider(
            "Highlights",
            adj.highlights,
            { viewModel.updateColorAdjustment(paramName = "highlights", description = "Highlights: ${it.toInt()}%") { c -> c.copy(highlights = it) } },
            verticalPadding = sliderPadding
        )
        ValueSlider(
            "Shadows",
            adj.shadows,
            { viewModel.updateColorAdjustment(paramName = "shadows", description = "Shadows: ${it.toInt()}%") { c -> c.copy(shadows = it) } },
            verticalPadding = sliderPadding
        )
        ValueSlider(
            "Whites",
            adj.whites,
            { viewModel.updateColorAdjustment(paramName = "whites", description = "Whites: ${it.toInt()}%") { c -> c.copy(whites = it) } },
            verticalPadding = sliderPadding
        )
        ValueSlider(
            "Blacks",
            adj.blacks,
            { viewModel.updateColorAdjustment(paramName = "blacks", description = "Blacks: ${it.toInt()}%") { c -> c.copy(blacks = it) } },
            verticalPadding = sliderPadding
        )
        ValueSlider(
            "Saturation",
            adj.saturation,
            { viewModel.updateColorAdjustment(paramName = "saturation", description = "Saturation: ${it.toInt()}%") { c -> c.copy(saturation = it) } },
            verticalPadding = sliderPadding
        )
        ValueSlider(
            "Vibrance",
            adj.vibrance,
            { viewModel.updateColorAdjustment(paramName = "vibrance", description = "Vibrance: ${it.toInt()}%") { c -> c.copy(vibrance = it) } },
            verticalPadding = sliderPadding
        )
        ValueSlider(
            "Fade",
            adj.fade,
            { viewModel.updateColorAdjustment(paramName = "fade", description = "Fade: ${it.toInt()}%") { c -> c.copy(fade = it) } },
            valueRange = 0f..100f,
            verticalPadding = sliderPadding
        )
    }
}

@Composable
fun LutsPanel(adj: ColorAdjustment, viewModel: MainViewModel) {
    val luts = listOf(
        0 to ("None (Natural)" to "Original Master Camera Look"),
        1 to ("Teal & Orange" to "Hollywood Blockbuster Film Look"),
        2 to ("Moody Film" to "Emerald & Slate Dark Cinema"),
        3 to ("Cyberpunk Neon" to "Vivid Electric Magenta & Cyan"),
        4 to ("Clean Arri" to "Organic Commercial Skin Tones"),
        5 to ("Vintage 70s" to "Warm Super-8 Kodak Emulsion"),
        6 to ("Bleach Bypass" to "Silver Gelatin High Contrast"),
        7 to ("Noir B&W" to "35mm High-Dynamic Monochrome"),
        8 to ("Sunset Gold" to "Warm Golden Hour Amber Glow")
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        ValueSlider(
            label = "LUT Blend Intensity",
            value = adj.lutIntensity,
            onValueChange = {
                viewModel.updateColorAdjustment(paramName = "lut_intensity", description = "LUT Intensity: ${it.toInt()}%") { c ->
                    c.copy(lutIntensity = it)
                }
            },
            valueRange = 0f..100f,
            defaultValue = 100f,
            unit = "%"
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Cinematic 3D Look-Up Tables (Real-Time GPU)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            luts.forEach { (idx, lutInfo) ->
                val (name, subtitle) = lutInfo
                val isSelected = adj.lutIndex == idx
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .width(150.dp)
                        .clickable {
                            viewModel.updateColorAdjustment(paramName = "lut", description = "Applied LUT: $name") { c ->
                                c.copy(lutIndex = idx)
                            }
                        }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            maxLines = 2,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
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
        ValueSlider(
            "Temperature",
            adj.temperature,
            { viewModel.updateColorAdjustment(paramName = "temperature", description = "Temperature: ${it.toInt()}") { c -> c.copy(temperature = it) } },
            verticalPadding = sliderPadding
        )
        ValueSlider(
            "Tint",
            adj.tint,
            { viewModel.updateColorAdjustment(paramName = "tint", description = "Tint: ${it.toInt()}") { c -> c.copy(tint = it) } },
            verticalPadding = sliderPadding
        )

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.revertParameter("temperature"); viewModel.revertParameter("tint") },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(42.dp)
        ) {
            Text("Reset Color Balance", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
        }
    }
}

@Composable
fun HslPanel(adj: ColorAdjustment, viewModel: MainViewModel, sliderPadding: androidx.compose.ui.unit.Dp = 4.dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        ValueSlider(
            "Hue Shift",
            adj.hueShift,
            { viewModel.updateColorAdjustment(paramName = "hueshift", description = "Hue Shift: ${it.toInt()}°") { c -> c.copy(hueShift = it) } },
            valueRange = -180f..180f,
            verticalPadding = sliderPadding
        )
        ValueSlider(
            "HSL Saturation",
            adj.hslSaturation,
            { viewModel.updateColorAdjustment(paramName = "hslsaturation", description = "HSL Saturation: ${it.toInt()}%") { c -> c.copy(hslSaturation = it) } },
            verticalPadding = sliderPadding
        )
        ValueSlider(
            "HSL Luminance",
            adj.hslLuminance,
            { viewModel.updateColorAdjustment(paramName = "hslluminance", description = "HSL Luminance: ${it.toInt()}%") { c -> c.copy(hslLuminance = it) } },
            verticalPadding = sliderPadding
        )
    }
}

@Composable
fun RgbPanel(adj: ColorAdjustment, viewModel: MainViewModel, sliderPadding: androidx.compose.ui.unit.Dp = 4.dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        ValueSlider(
            "Red Gain",
            adj.rgbRed,
            { viewModel.updateColorAdjustment(paramName = "rgbred", description = "Red Gain: ${it.toInt()}%") { c -> c.copy(rgbRed = it) } },
            verticalPadding = sliderPadding
        )
        ValueSlider(
            "Green Gain",
            adj.rgbGreen,
            { viewModel.updateColorAdjustment(paramName = "rgbgreen", description = "Green Gain: ${it.toInt()}%") { c -> c.copy(rgbGreen = it) } },
            verticalPadding = sliderPadding
        )
        ValueSlider(
            "Blue Gain",
            adj.rgbBlue,
            { viewModel.updateColorAdjustment(paramName = "rgbblue", description = "Blue Gain: ${it.toInt()}%") { c -> c.copy(rgbBlue = it) } },
            verticalPadding = sliderPadding
        )
    }
}

@Composable
fun CurvesPanel(adj: ColorAdjustment, viewModel: MainViewModel, graphHeight: androidx.compose.ui.unit.Dp = 130.dp) {
    var selectedChannel by remember { mutableStateOf("Master") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Master", "Red", "Green", "Blue").forEach { channel ->
                val isSelected = selectedChannel == channel
                val chipColor = when (channel) {
                    "Red" -> Color(0xFFEF5350)
                    "Green" -> Color(0xFF66BB6A)
                    "Blue" -> Color(0xFF42A5F5)
                    else -> Color.White
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedChannel = channel },
                    label = { Text(channel, color = if (isSelected) Color.White else chipColor, fontWeight = FontWeight.Bold) }
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        when (selectedChannel) {
            "Master" -> CurvesGraphView(
                curveValue = adj.curveMaster,
                onCurveValueChange = { viewModel.updateColorAdjustment(paramName = "curves", description = "Master Curve: ${it.toInt()}%") { c -> c.copy(curveMaster = it) } },
                channelColor = Color.White,
                channelName = "RGB Master Curve",
                graphHeight = graphHeight
            )
            "Red" -> CurvesGraphView(
                curveValue = adj.curveRed,
                onCurveValueChange = { viewModel.updateColorAdjustment(paramName = "curves", description = "Red Curve: ${it.toInt()}%") { c -> c.copy(curveRed = it) } },
                channelColor = Color(0xFFEF5350),
                channelName = "Red Channel Curve",
                graphHeight = graphHeight
            )
            "Green" -> CurvesGraphView(
                curveValue = adj.curveGreen,
                onCurveValueChange = { viewModel.updateColorAdjustment(paramName = "curves", description = "Green Curve: ${it.toInt()}%") { c -> c.copy(curveGreen = it) } },
                channelColor = Color(0xFF66BB6A),
                channelName = "Green Channel Curve",
                graphHeight = graphHeight
            )
            "Blue" -> CurvesGraphView(
                curveValue = adj.curveBlue,
                onCurveValueChange = { viewModel.updateColorAdjustment(paramName = "curves", description = "Blue Curve: ${it.toInt()}%") { c -> c.copy(curveBlue = it) } },
                channelColor = Color(0xFF42A5F5),
                channelName = "Blue Channel Curve",
                graphHeight = graphHeight
            )
        }
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
                viewModel.updateColorAdjustment(paramName = "colorwheels", description = "Shadow Wheel: ${a.toInt()}%") { it.copy(shadowTintHue = h, shadowTintAmount = a) }
            },
            wheelSize = wheelSize
        )
        ColorWheelView(
            title = "Midtones",
            hue = adj.midtoneTintHue,
            amount = adj.midtoneTintAmount,
            onHueAmountChange = { h, a ->
                viewModel.updateColorAdjustment(paramName = "colorwheels", description = "Midtone Wheel: ${a.toInt()}%") { it.copy(midtoneTintHue = h, midtoneTintAmount = a) }
            },
            wheelSize = wheelSize
        )
        ColorWheelView(
            title = "Highlights",
            hue = adj.highlightTintHue,
            amount = adj.highlightTintAmount,
            onHueAmountChange = { h, a ->
                viewModel.updateColorAdjustment(paramName = "colorwheels", description = "Highlight Wheel: ${a.toInt()}%") { it.copy(highlightTintHue = h, highlightTintAmount = a) }
            },
            wheelSize = wheelSize
        )
    }
}

@Composable
fun DetailPanel(adj: ColorAdjustment, config: EnhancementConfig, viewModel: MainViewModel, sliderPadding: androidx.compose.ui.unit.Dp = 4.dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        ValueSlider("Sharpness", adj.sharpness, { viewModel.updateColorAdjustment(paramName = "sharpness", description = "Sharpness: ${it.toInt()}%") { c -> c.copy(sharpness = it) } }, valueRange = 0f..100f, verticalPadding = sliderPadding)
        ValueSlider("Clarity", adj.clarity, { viewModel.updateColorAdjustment(paramName = "clarity", description = "Clarity: ${it.toInt()}%") { c -> c.copy(clarity = it) } }, valueRange = 0f..100f, verticalPadding = sliderPadding)
        ValueSlider("Denoise", config.aiDenoise, { viewModel.updateEnhancementConfig { c -> c.copy(aiDenoise = it) } }, valueRange = 0f..100f, verticalPadding = sliderPadding)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Target Resolution
                Text("Output Resolution", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
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
                Text("Target Frame Rate", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFps.values().forEach { f ->
                        FilterChip(
                            selected = config.targetFps == f,
                            onClick = { onUpdateConfig { it.copy(targetFps = f) } },
                            label = { Text(f.label, fontSize = if (compact.isCompact) 11.sp else 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (compact.isCompact) 8.dp else 14.dp))

                // Target Codec
                Text("Video Codec", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
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
