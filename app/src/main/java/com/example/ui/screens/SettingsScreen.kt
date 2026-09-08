package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.InfoBadge
import com.example.ui.components.LocalCompactUiConfig
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToStorage: () -> Unit
) {
    val compact = LocalCompactUiConfig.current
    val currentTheme by viewModel.settingsRepo.theme.collectAsState()
    val darkModeSetting by viewModel.settingsRepo.darkMode.collectAsState()
    val compactModeSetting by viewModel.settingsRepo.compactMode.collectAsState()
    val preferGpu by viewModel.settingsRepo.preferGpu.collectAsState()
    val hwEncoding by viewModel.settingsRepo.hardwareEncoding.collectAsState()
    val thermalProtection by viewModel.settingsRepo.thermalProtection.collectAsState()

    val studioThemes = listOf(
        "Obsidian" to Color(0xFFE5B54F),
        "Midnight" to Color(0xFF38BDF8),
        "Graphite" to Color(0xFFA1A1AA),
        "Pearl" to Color(0xFFB4833E),
        "Frost" to Color(0xFF0284C7),
        "Aurora" to Color(0xFF10B981)
    )

    val pastelThemes = listOf(
        "Pastel Rose" to Color(0xFFF472B6),
        "Pastel Lavender" to Color(0xFFA78BFA),
        "Pastel Mint" to Color(0xFF34D399),
        "Pastel Peach" to Color(0xFFFB923C),
        "Pastel Sky" to Color(0xFF38BDF8),
        "Pastel Butter" to Color(0xFFEAB308),
        "Pastel Matcha" to Color(0xFF84CC16)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
                .padding(horizontal = compact.cardPadding, vertical = if (compact.isCompact) 8.dp else 12.dp)
        ) {
            // Privacy Commitment Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Your videos stay on your device.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ELITE QUALITY is fundamentally offline-first. Processing, enhancement, color grading, and exports happen 100% locally on your device's hardware. Zero cloud servers, zero analytics tracking, and zero account required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Appearance & Themes
            Text(
                text = "Themes & Appearance",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Studio Themes Subheader
                    Text(
                        text = "Studio & Classic Themes",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        studioThemes.forEach { (name, dotColor) ->
                            FilterChip(
                                selected = currentTheme == name,
                                onClick = { viewModel.settingsRepo.setTheme(name) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(name)
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pastel Themes Subheader
                    Text(
                        text = "Pastel Aesthetics",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pastelThemes.forEach { (name, dotColor) ->
                            FilterChip(
                                selected = currentTheme == name,
                                onClick = { viewModel.settingsRepo.setTheme(name) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(name)
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Dark / Light / System Mode
                    Text(
                        text = "Display Mode",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("dark" to "Dark", "light" to "Light", "system" to "System").forEach { (k, label) ->
                            FilterChip(
                                selected = darkModeSetting == k,
                                onClick = { viewModel.settingsRepo.setDarkMode(k) },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (compact.isCompact) 14.dp else 22.dp))

            // Display Scaling & Compact Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Display Scaling & Compact Mode",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                InfoBadge(
                    text = if (compact.isCompact) "COMPACT ACTIVE" else "STANDARD",
                    isHighlight = compact.isCompact
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(if (compact.isCompact) 12.dp else 16.dp)) {
                    Text(
                        text = "Automatically resize player controls, sliders, tools, and cards to fit small display phones and high density screens.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = if (compact.isCompact) 11.5.sp else 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "auto" to "Auto (Adaptive)",
                            "compact" to "Compact",
                            "standard" to "Standard"
                        ).forEach { (modeKey, label) ->
                            FilterChip(
                                selected = compactModeSetting == modeKey,
                                onClick = { viewModel.settingsRepo.setCompactMode(modeKey) },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = if (compact.isCompact) 11.sp else 12.sp,
                                        fontWeight = if (compactModeSetting == modeKey) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (compact.isCompact) 14.dp else 22.dp))

            // Engine & Hardware Acceleration
            Text(
                text = "Processing Engine",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("GPU Acceleration", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("Render color matrices & shaders on GPU", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = preferGpu,
                            onCheckedChange = { viewModel.settingsRepo.setPreferGpu(it) }
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Hardware Encoding", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("Use MediaCodec hardware encoder", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = hwEncoding,
                            onCheckedChange = { viewModel.settingsRepo.setHardwareEncoding(it) }
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Thermal Protection", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("Prevent device overheating during 4K tasks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = thermalProtection,
                            onCheckedChange = { viewModel.settingsRepo.setThermalProtection(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Hardware Diagnostics
            Text(
                text = "Device Hardware Diagnostics",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Device: ${Build.MANUFACTURER.capitalize()} ${Build.MODEL}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Android OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Architecture: ${Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Hardware Encoders: H.264 / AVC, H.265 / HEVC Hardware Active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Developer Credit & App Version Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("developer_credit_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Made with love",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Made with ❤️ by Rahul Shah",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "ELITE QUALITY • Version 5.0",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "High-performance offline GPU video enhancement, real-time color grading & cinematic LUT mastering engine.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InfoBadge(text = "v5.0 RELEASE", isHighlight = true)
                        InfoBadge(text = "GPU ACCELERATED", isHighlight = false)
                        InfoBadge(text = "100% OFFLINE", isHighlight = false)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
