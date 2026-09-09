package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import android.widget.MediaController
import coil.compose.AsyncImage
import com.example.R
import com.example.data.db.ExportEntity
import com.example.data.db.ProjectEntity
import com.example.data.model.DeviceVideoItem
import com.example.ui.components.InfoBadge
import com.example.ui.components.LocalCompactUiConfig
import com.example.ui.components.PermissionRequester
import com.example.ui.components.SocialMediaAndCapabilitiesDialog
import com.example.ui.components.SocialMediaPreset
import com.example.util.VideoThumbnailHelper
import com.example.ui.viewmodel.MainViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToEditor: () -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToPresets: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val compact = LocalCompactUiConfig.current
    val recentProjects by viewModel.recentProjects.collectAsState()
    val allExports by viewModel.allExports.collectAsState()
    val deviceVideos by viewModel.deviceVideos.collectAsState()
    val selectedExportPreview by viewModel.selectedExportPreview.collectAsState()
    val currentMetadata by viewModel.currentMetadata.collectAsState()

    var activeHistoryTab by remember { mutableStateOf("Exports") } // "Exports" or "Drafts"
    var deviceFilterTab by remember { mutableStateOf("All") } // "All", "Enhance", "Pending"
    var showSocialSpecsDialog by remember { mutableStateOf(false) }

    // Real video pickers
    val galleryVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectVideo(uri)
            onNavigateToEditor()
        }
    }

    val fileStoragePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectVideo(uri)
            onNavigateToEditor()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = "App Icon",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ELITE QUALITY",
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSocialSpecsDialog = true },
                        modifier = Modifier.testTag("home_social_specs_button")
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Specs & Social Settings")
                    }
                    IconButton(
                        onClick = onNavigateToPresets,
                        modifier = Modifier.testTag("home_presets_button")
                    ) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = "Presets")
                    }
                    IconButton(
                        onClick = onNavigateToStorage,
                        modifier = Modifier.testTag("home_storage_button")
                    ) {
                        Icon(imageVector = Icons.Default.SdStorage, contentDescription = "Storage")
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        PermissionRequester(
            onPermissionsGranted = {
                viewModel.loadDeviceVideos()
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = compact.cardPadding),
            verticalArrangement = Arrangement.spacedBy(compact.contentSpacing),
            contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
        ) {
            // Hero Action Banner: Select Real Video from Device
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(if (compact.isCompact) 14.dp else 22.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Offline AI Video Studio",
                                style = if (compact.isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            InfoBadge(text = "100% OFFLINE", isHighlight = true)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Professional color grading, 16 3D LUTs, GPU pipelines, RGB Curves, and AI super-resolution directly on your phone's hardware.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = if (compact.isCompact) 16.sp else 20.sp,
                            fontSize = if (compact.isCompact) 12.sp else 13.sp
                        )

                        Spacer(modifier = Modifier.height(if (compact.isCompact) 12.dp else 18.dp))

                        // Primary Action: Open Gallery
                        Button(
                            onClick = {
                                galleryVideoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(compact.heroButtonHeight)
                                .testTag("select_video_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(if (compact.isCompact) 18.dp else 20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Select Video to Enhance",
                                fontWeight = FontWeight.Bold,
                                fontSize = if (compact.isCompact) 13.sp else 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Secondary Action: Pick from Files / Storage
                            OutlinedButton(
                                onClick = { fileStoragePicker.launch("video/*") },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(compact.secondaryButtonHeight)
                                    .testTag("browse_files_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Files",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = if (compact.isCompact) 12.sp else 13.sp
                                )
                            }

                            // Social & Specs Guide Button
                            OutlinedButton(
                                onClick = { showSocialSpecsDialog = true },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(compact.secondaryButtonHeight)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Specs & Social",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = if (compact.isCompact) 12.sp else 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Real On-Device Gallery Videos Section with Thumbnails & Separate Options
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Videos on Device",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Thumbnails & instant 1-tap enhancement",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (deviceVideos.isNotEmpty()) {
                            Text(
                                text = "${deviceVideos.size} found",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Separate Filter Options: All Videos | Enhance Video | Pending Video
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "All" to "All Videos",
                            "Enhance" to "Enhance Video",
                            "Pending" to "Pending Video"
                        ).forEach { (key, label) ->
                            val isSelected = deviceFilterTab == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                                    .clickable { deviceFilterTab = key }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = if (compact.isCompact) 10.5.sp else 11.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val filteredVideos = remember(deviceVideos, deviceFilterTab) {
                        when (deviceFilterTab) {
                            "Enhance" -> deviceVideos.filter { it.width >= 1920 || it.height >= 1920 }
                            "Pending" -> deviceVideos.filter { it.durationMs < 120_000 }
                            else -> deviceVideos
                        }
                    }

                    if (deviceVideos.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Tap 'Select Video' above or grant storage access to browse local device camera videos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(filteredVideos, key = { it.uri.toString() }) { video ->
                                EnhancedDeviceVideoCard(
                                    video = video,
                                    isPending = deviceFilterTab == "Pending",
                                    onEnhance = {
                                        viewModel.selectVideo(video.uri)
                                        onNavigateToEditor()
                                    },
                                    onShowSpecs = {
                                        viewModel.selectVideo(video.uri)
                                        showSocialSpecsDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Real-Time History Section with Verified Quality Data
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent History & Quality",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        TextButton(onClick = onNavigateToProjects) {
                            Text(
                                text = "Manage (${allExports.size + recentProjects.size})",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Tab Segmented Control
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isExports = activeHistoryTab == "Exports"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isExports) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .clickable { activeHistoryTab = "Exports" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Rendered Exports (${allExports.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isExports) FontWeight.Bold else FontWeight.Medium,
                                color = if (isExports) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val isDrafts = activeHistoryTab == "Drafts"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDrafts) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .clickable { activeHistoryTab = "Drafts" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Project Drafts (${recentProjects.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isDrafts) FontWeight.Bold else FontWeight.Medium,
                                color = if (isDrafts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (activeHistoryTab == "Exports") {
                if (allExports.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MovieFilter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(38.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No Exports Yet",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "When you enhance a video, real-time export quality data (resolution, bitrate, FPS, codec, file size) will appear here instantly.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(allExports, key = { it.id }) { export ->
                        RealExportQualityCard(
                            export = export,
                            onPlay = { viewModel.setExportPreview(export) },
                            onShare = { viewModel.shareExport(export) },
                            onDelete = { viewModel.deleteExport(export.id) }
                        )
                    }
                }
            } else {
                if (recentProjects.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(38.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No Project Drafts",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Select any video from your phone to begin real-time editing and enhancement",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(recentProjects, key = { it.id }) { project ->
                        RecentProjectCard(
                            project = project,
                            onClick = {
                                viewModel.selectVideo(Uri.parse(project.sourceUri))
                                onNavigateToEditor()
                            }
                        )
                    }
                }
            }
        }
    }

    // Supported Video Specs & Social Presets Dialog
    if (showSocialSpecsDialog) {
        SocialMediaAndCapabilitiesDialog(
            currentMetadata = currentMetadata,
            onDismiss = { showSocialSpecsDialog = false },
            onApplySocialPreset = { preset ->
                viewModel.applySocialPreset(
                    targetResolution = preset.targetResolution,
                    targetFps = preset.targetFps,
                    targetBitrate = preset.targetBitrate,
                    targetCodec = preset.targetCodec,
                    cropRatio = preset.cropRatio
                )
                if (viewModel.currentVideoUri.value != null) {
                    onNavigateToEditor()
                }
            }
        )
    }

    // Full Real-Time Export Playback & Quality Detail Dialog
    selectedExportPreview?.let { export ->
        ExportPreviewDialog(
            export = export,
            onDismiss = { viewModel.setExportPreview(null) },
            onShare = { viewModel.shareExport(export) }
        )
    }
}

@Composable
fun EnhancedDeviceVideoCard(
    video: DeviceVideoItem,
    isPending: Boolean = false,
    onEnhance: () -> Unit,
    onShowSpecs: () -> Unit
) {
    val context = LocalContext.current
    val compact = LocalCompactUiConfig.current
    var thumbnailBitmap by remember(video.uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(video.uri) {
        thumbnailBitmap = VideoThumbnailHelper.getThumbnail(context, video.uri)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .width(if (compact.isCompact) 170.dp else 205.dp)
            .clickable { onEnhance() }
    ) {
        Column(modifier = Modifier.padding(if (compact.isCompact) 8.dp else 12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact.isCompact) 95.dp else 115.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = video.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(if (compact.isCompact) 32.dp else 40.dp)
                    )
                }

                // Status Badge Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isPending) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isPending) "PENDING" else video.resolutionBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp
                    )
                }

                // Duration Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.formattedDuration,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact.isCompact) 9.sp else 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = video.displayName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                fontSize = if (compact.isCompact) 12.sp else 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoBadge(text = "${video.width}×${video.height}")
                Text(
                    text = video.formattedSize,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = if (compact.isCompact) 10.sp else 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Separate Action Buttons: "Enhance Video" and "Pending / Specs"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onEnhance,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Enhance",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp
                    )
                }

                OutlinedButton(
                    onClick = onShowSpecs,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(30.dp)
                ) {
                    Text(
                        text = "Specs",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RealExportQualityCard(
    export: ExportEntity,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val compact = LocalCompactUiConfig.current
    val formattedSize = remember(export.fileSizeBytes) {
        val mb = export.fileSizeBytes / (1024.0 * 1024.0)
        if (mb >= 1000) String.format(Locale.US, "%.2f GB", mb / 1024.0)
        else String.format(Locale.US, "%.1f MB", mb)
    }

    val timeFormatted = remember(export.timestamp) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(export.timestamp))
    }

    val fileExists = remember(export.outputFilePath) {
        File(export.outputFilePath).exists()
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
    ) {
        Column(modifier = Modifier.padding(if (compact.isCompact) 10.dp else 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = export.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Rendered $timeFormatted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real-Time Verified Quality Metrics Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoBadge(text = export.resolution, isHighlight = export.resolution.contains("4K") || export.resolution.contains("1080p"))
                InfoBadge(text = "${export.fps} FPS")
                InfoBadge(text = "${String.format(Locale.US, "%.1f", export.bitrateMbps)} Mbps")
                InfoBadge(text = export.codec)
                InfoBadge(text = formattedSize)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (fileExists) Icons.Outlined.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (fileExists) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (fileExists) "Stored on Device / Gallery" else "Media Muxed",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Tap to Play Preview",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ExportPreviewDialog(
    export: ExportEntity,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    val formattedSize = remember(export.fileSizeBytes) {
        val mb = export.fileSizeBytes / (1024.0 * 1024.0)
        if (mb >= 1000) String.format(Locale.US, "%.2f GB", mb / 1024.0)
        else String.format(Locale.US, "%.1f MB", mb)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Rendered Video Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = export.title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Real Android Video Player View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val mc = MediaController(ctx)
                                mc.setAnchorView(this)
                                setMediaController(mc)
                                val targetUri = if (export.outputUri.isNotEmpty()) Uri.parse(export.outputUri)
                                else Uri.fromFile(File(export.outputFilePath))
                                setVideoURI(targetUri)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Real-Time Quality Data",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Resolution:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(export.resolution, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Target Bitrate:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${export.bitrateMbps} Mbps", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Frame Rate:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${export.fps} FPS", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Encoder Codec:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(export.codec, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("File Size:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formattedSize, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onShare,
                shape = RoundedCornerShape(50)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share Video")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun RecentProjectCard(
    project: ProjectEntity,
    onClick: () -> Unit
) {
    val compact = LocalCompactUiConfig.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact.isCompact) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(
                        width = if (compact.isCompact) 64.dp else 80.dp,
                        height = if (compact.isCompact) 52.dp else 65.dp
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
            ) {
                if (!project.thumbnailPath.isNullOrEmpty()) {
                    AsyncImage(
                        model = project.thumbnailPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Movie, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                    }
                }
            }

            Spacer(modifier = Modifier.width(if (compact.isCompact) 10.dp else 14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = if (compact.isCompact) 13.sp else 14.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val resBadge = when {
                        project.width >= 3800 -> "4K"
                        project.width >= 1900 -> "1080p"
                        else -> "720p"
                    }
                    InfoBadge(text = resBadge, isHighlight = project.status == "Enhanced")
                    InfoBadge(text = "${project.fps.toInt()} FPS")
                    InfoBadge(text = project.status)
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
