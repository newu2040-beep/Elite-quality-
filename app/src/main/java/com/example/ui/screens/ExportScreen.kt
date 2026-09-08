package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.data.model.ColorAdjustment
import com.example.ui.components.ComparisonMode
import com.example.ui.components.InfoBadge
import com.example.ui.components.VideoPlayerView
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: MainViewModel,
    onNavigateHome: () -> Unit,
    onContinueEditing: () -> Unit
) {
    val context = LocalContext.current
    val exportedFile by viewModel.lastExportedFile.collectAsState()
    val metadata by viewModel.currentMetadata.collectAsState()
    val config by viewModel.enhancementConfig.collectAsState()
    val autoSaved by viewModel.autoSavedToGallery.collectAsState()

    var manualSaveSuccess by remember { mutableStateOf(false) }
    val isSaved = autoSaved || manualSaveSuccess

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Export Complete",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateHome,
                        modifier = Modifier.testTag("export_home_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Video Player Preview of the Output
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .background(Color.Black)
            ) {
                if (exportedFile != null) {
                    VideoPlayerView(
                        videoUri = Uri.fromFile(exportedFile!!),
                        colorAdjustment = ColorAdjustment(),
                        comparisonMode = ComparisonMode.ORIGINAL,
                        splitFraction = 0.5f,
                        onSplitFractionChange = {},
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Auto-Saved Confirmation Banner
            if (isSaved) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Automatically saved to device Gallery (Movies/EliteQuality)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Output Video Summary Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = exportedFile?.name ?: "Enhanced Video",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        InfoBadge(text = "SUCCESS", isHighlight = true)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        InfoBadge(text = config.targetResolution.label, isHighlight = true)
                        InfoBadge(text = "${config.targetFps.fps.let { if (it > 0) it else metadata?.fps?.toInt() ?: 30 }} FPS")
                        InfoBadge(text = config.targetCodec.label.substringBefore(" "))
                        InfoBadge(text = config.targetBitrate.label.substringBefore(" "))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val sizeMb = (exportedFile?.length() ?: 0L).toDouble() / (1024 * 1024)
                    Text(
                        text = "File Size: ${String.format("%.2f MB", sizeMb)} • 100% Processed Locally",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            // 1. Share Video
            Button(
                onClick = { viewModel.shareLastExport() },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("export_share_button")
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Video", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Save / Re-save to Photos / Gallery
            Button(
                onClick = {
                    viewModel.saveLastExportToGallery { success ->
                        if (success) {
                            manualSaveSuccess = true
                            Toast.makeText(context, "Saved to Movies/EliteQuality in Gallery!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Failed to save to Gallery", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("export_gallery_button")
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.CheckCircle else Icons.Default.Download,
                    contentDescription = null,
                    tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSaved) "Saved to Device Gallery" else "Save to Gallery",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Open in External System Player
            OutlinedButton(
                onClick = { viewModel.openLastExport() },
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open in System Player")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Row: Continue Editing / Return Home
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onContinueEditing) {
                    Text("Continue Editing", color = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = onNavigateHome) {
                    Text("Done", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
