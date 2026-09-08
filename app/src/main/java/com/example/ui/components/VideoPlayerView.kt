package com.example.ui.components

import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.ColorAdjustment
import com.example.engine.ColorFilterEngine
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerView(
    videoUri: Uri?,
    colorAdjustment: ColorAdjustment,
    comparisonMode: ComparisonMode,
    splitFraction: Float,
    onSplitFractionChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(1L) }
    var isMuted by remember { mutableStateOf(false) }
    var isLooping by remember { mutableStateOf(true) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var showControls by remember { mutableStateOf(true) }
    var isScrubbing by remember { mutableStateOf(false) }

    // Auto-hide controls timer
    LaunchedEffect(showControls, isPlaying, isScrubbing) {
        if (showControls && isPlaying && !isScrubbing) {
            delay(4000)
            showControls = false
        }
    }

    // Periodic smooth position update
    LaunchedEffect(isPlaying, isScrubbing) {
        while (isPlaying && !isScrubbing) {
            mediaPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        currentPositionMs = mp.currentPosition.toLong()
                    }
                } catch (_: Exception) {}
            }
            delay(100)
        }
    }

    // Clean up MediaPlayer on dispose
    DisposableEffect(videoUri) {
        onDispose {
            mediaPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) mp.stop()
                    mp.release()
                } catch (_: Exception) {}
            }
            mediaPlayer = null
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        // Video TextureView inside AndroidView
        AndroidView(
            factory = { ctx ->
                val frameLayout = FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val textureView = TextureView(ctx).apply {
                    id = View.generateViewId()
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }

                textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        val surface = Surface(surfaceTexture)
                        try {
                            mediaPlayer?.release()
                            val mp = MediaPlayer().apply {
                                setSurface(surface)
                                if (videoUri != null) {
                                    setDataSource(ctx, videoUri)
                                    this.isLooping = isLooping
                                    prepareAsync()
                                    setOnPreparedListener { player ->
                                        durationMs = player.duration.toLong().coerceAtLeast(1L)
                                        player.start()
                                        isPlaying = true
                                    }
                                }
                            }
                            mediaPlayer = mp
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        mediaPlayer?.release()
                        mediaPlayer = null
                        return true
                    }
                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }

                frameLayout.addView(textureView)
                frameLayout
            },
            update = { frameLayout ->
                val textureView = frameLayout.getChildAt(0) as? TextureView
                if (textureView != null) {
                    if (comparisonMode == ComparisonMode.ORIGINAL) {
                        textureView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    } else {
                        val matrix = ColorFilterEngine.createColorMatrix(colorAdjustment)
                        val paint = Paint().apply {
                            colorFilter = android.graphics.ColorMatrixColorFilter(matrix.values)
                        }
                        textureView.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Draggable Split Screen comparison overlay
        SplitScreenOverlay(
            mode = comparisonMode,
            splitFraction = splitFraction,
            onSplitFractionChange = onSplitFractionChange
        )

        // Floating Inbuilt Player Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                // Top Mini Bar (Loop, Speed)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Loop Toggle
                    IconButton(
                        onClick = {
                            isLooping = !isLooping
                            mediaPlayer?.isLooping = isLooping
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isLooping) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Loop",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Speed Toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable {
                                val nextSpeed = when (currentSpeed) {
                                    0.5f -> 0.75f
                                    0.75f -> 1.0f
                                    1.0f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 2.0f
                                    else -> 0.5f
                                }
                                currentSpeed = nextSpeed
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    try {
                                        mediaPlayer?.playbackParams = PlaybackParams().apply { speed = nextSpeed }
                                    } catch (_: Exception) {}
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${currentSpeed}x",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Center Transport Controls (-5s, Play/Pause, +5s)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Rewind 5 seconds
                    IconButton(
                        onClick = {
                            mediaPlayer?.let { mp ->
                                val target = (currentPositionMs - 5000L).coerceAtLeast(0L)
                                currentPositionMs = target
                                mp.seekTo(target.toInt())
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay5,
                            contentDescription = "Rewind 5s",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Main Play/Pause Button
                    IconButton(
                        onClick = {
                            mediaPlayer?.let { mp ->
                                try {
                                    if (mp.isPlaying) {
                                        mp.pause()
                                        isPlaying = false
                                    } else {
                                        mp.start()
                                        isPlaying = true
                                    }
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Forward 5 seconds
                    IconButton(
                        onClick = {
                            mediaPlayer?.let { mp ->
                                val target = (currentPositionMs + 5000L).coerceAtMost(durationMs)
                                currentPositionMs = target
                                mp.seekTo(target.toInt())
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward5,
                            contentDescription = "Forward 5s",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Bottom Timeline & Volume Strip
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    // Smooth Scrub Slider
                    Slider(
                        value = currentPositionMs.toFloat().coerceIn(0f, durationMs.toFloat()),
                        onValueChange = { newPos ->
                            isScrubbing = true
                            currentPositionMs = newPos.toLong()
                        },
                        onValueChangeFinished = {
                            isScrubbing = false
                            mediaPlayer?.seekTo(currentPositionMs.toInt())
                        },
                        valueRange = 0f..durationMs.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Precise time label
                        Text(
                            text = "${formatTimeWithMs(currentPositionMs)} / ${formatTimeWithMs(durationMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Mute / Unmute
                        IconButton(
                            onClick = {
                                val nextMute = !isMuted
                                isMuted = nextMute
                                val vol = if (nextMute) 0f else 1f
                                mediaPlayer?.setVolume(vol, vol)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimeWithMs(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    val centis = (ms % 1000) / 10
    return String.format("%02d:%02d.%02d", min, sec, centis)
}
