package com.example.ui.components

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.opengl.*
import android.os.Build
import android.os.Handler
import android.os.Looper
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
import com.example.data.model.EnhancementConfig
import com.example.engine.ColorFilterEngine
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class GlVideoTextureView(context: Context) : TextureView(context), TextureView.SurfaceTextureListener {

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    private var glProgram: Int = 0
    private var uniforms: ColorFilterEngine.ShaderUniforms? = null
    private var videoTextureId: Int = 0
    private var videoSurfaceTexture: SurfaceTexture? = null
    private var videoSurface: Surface? = null

    private var aPositionLoc = -1
    private var aTextureCoordLoc = -1
    private var vertexBuffer: FloatBuffer? = null
    private var texCoordBuffer: FloatBuffer? = null
    private val texMatrix = FloatArray(16).apply {
        android.opengl.Matrix.setIdentityM(this, 0)
    }

    private var viewWidth: Int = 1
    private var viewHeight: Int = 1

    private val mainHandler = Handler(Looper.getMainLooper())

    var mediaPlayer: MediaPlayer? = null
        private set

    var videoUri: Uri? = null
    var isLooping: Boolean = true
    var onPreparedCallback: ((durationMs: Long) -> Unit)? = null
    var onPlayingChanged: ((Boolean) -> Unit)? = null

    var colorAdjustment: ColorAdjustment = ColorAdjustment()
    var comparisonMode: ComparisonMode = ComparisonMode.SPLIT
    var splitFraction: Float = 0.5f
    var enhancementConfig: EnhancementConfig? = null

    companion object {
        private val VERTEX_COORDS = floatArrayOf(
            -1.0f, -1.0f,
             1.0f, -1.0f,
            -1.0f,  1.0f,
             1.0f,  1.0f
        )
        private val TEXTURE_COORDS = floatArrayOf(
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
        )
    }

    init {
        surfaceTextureListener = this
        val vbb = ByteBuffer.allocateDirect(VERTEX_COORDS.size * 4).order(ByteOrder.nativeOrder())
        vertexBuffer = vbb.asFloatBuffer().put(VERTEX_COORDS).apply { position(0) }

        val tbb = ByteBuffer.allocateDirect(TEXTURE_COORDS.size * 4).order(ByteOrder.nativeOrder())
        texCoordBuffer = tbb.asFloatBuffer().put(TEXTURE_COORDS).apply { position(0) }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        initEglAndGl(surface)
        setupMediaPlayer()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        renderFrame(updateTex = false)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        releaseMediaPlayer()
        releaseEglAndGl()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    private fun initEglAndGl(surface: SurfaceTexture) {
        try {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val version = IntArray(2)
            EGL14.eglInitialize(eglDisplay, version, 0, version, 1)

            val configAttribs = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)
            val eglConfig = configs[0] ?: return

            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )
            eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)

            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, surfaceAttribs, 0)
            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)

            // Setup GL Program
            glProgram = ColorFilterEngine.createProgram(ColorFilterEngine.VERTEX_SHADER, ColorFilterEngine.FRAGMENT_SHADER)
            uniforms = ColorFilterEngine.ShaderUniforms(glProgram)
            aPositionLoc = GLES20.glGetAttribLocation(glProgram, "aPosition")
            aTextureCoordLoc = GLES20.glGetAttribLocation(glProgram, "aTextureCoord")

            // Setup external video texture
            val texIds = IntArray(1)
            GLES20.glGenTextures(1, texIds, 0)
            videoTextureId = texIds[0]
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoTextureId)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

            videoSurfaceTexture = SurfaceTexture(videoTextureId).apply {
                setOnFrameAvailableListener({
                    mainHandler.post { renderFrame(updateTex = true) }
                }, mainHandler)
            }
            videoSurface = Surface(videoSurfaceTexture)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupMediaPlayer() {
        val uri = videoUri ?: return
        val surface = videoSurface ?: return
        try {
            releaseMediaPlayer()
            val mp = MediaPlayer().apply {
                setSurface(surface)
                setDataSource(context, uri)
                this.isLooping = this@GlVideoTextureView.isLooping
                prepareAsync()
                setOnPreparedListener { player ->
                    onPreparedCallback?.invoke(player.duration.toLong().coerceAtLeast(1L))
                    player.start()
                    onPlayingChanged?.invoke(true)
                }
                setOnCompletionListener {
                    if (!this@GlVideoTextureView.isLooping) {
                        onPlayingChanged?.invoke(false)
                    }
                }
            }
            mediaPlayer = mp
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setSource(uri: Uri?, looping: Boolean) {
        val changed = uri != videoUri
        videoUri = uri
        isLooping = looping
        if (changed && videoSurface != null) {
            setupMediaPlayer()
        }
    }

    fun updateState(
        adj: ColorAdjustment,
        mode: ComparisonMode,
        fraction: Float,
        config: EnhancementConfig?
    ) {
        colorAdjustment = adj
        comparisonMode = mode
        splitFraction = fraction
        enhancementConfig = config
        mainHandler.post { renderFrame(updateTex = false) }
    }

    fun renderFrame(updateTex: Boolean) {
        if (eglDisplay == EGL14.EGL_NO_DISPLAY || eglSurface == EGL14.EGL_NO_SURFACE || glProgram == 0) return
        val uni = uniforms ?: return

        try {
            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)

            if (updateTex) {
                try {
                    videoSurfaceTexture?.updateTexImage()
                    videoSurfaceTexture?.getTransformMatrix(texMatrix)
                } catch (_: Exception) {}
            }

            GLES20.glViewport(0, 0, viewWidth, viewHeight)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glUseProgram(glProgram)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoTextureId)

            vertexBuffer?.position(0)
            GLES20.glEnableVertexAttribArray(aPositionLoc)
            GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

            texCoordBuffer?.position(0)
            GLES20.glEnableVertexAttribArray(aTextureCoordLoc)
            GLES20.glVertexAttribPointer(aTextureCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

            val modeInt = when (comparisonMode) {
                ComparisonMode.ORIGINAL -> 0
                ComparisonMode.ENHANCED -> 1
                ComparisonMode.SPLIT -> 2
            }

            ColorFilterEngine.bindUniforms(
                uniforms = uni,
                texMatrix = texMatrix,
                comparisonMode = modeInt,
                splitFraction = splitFraction,
                width = viewWidth,
                height = viewHeight,
                adj = colorAdjustment,
                config = enhancementConfig
            )

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

            GLES20.glDisableVertexAttribArray(aPositionLoc)
            GLES20.glDisableVertexAttribArray(aTextureCoordLoc)

            EGL14.eglSwapBuffers(eglDisplay, eglSurface)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    private fun releaseEglAndGl() {
        try {
            videoSurface?.release()
            videoSurface = null
            videoSurfaceTexture?.release()
            videoSurfaceTexture = null

            if (glProgram != 0) {
                GLES20.glDeleteProgram(glProgram)
                glProgram = 0
            }
            if (videoTextureId != 0) {
                GLES20.glDeleteTextures(1, intArrayOf(videoTextureId), 0)
                videoTextureId = 0
            }
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(eglDisplay, eglSurface)
                if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(eglDisplay, eglContext)
                EGL14.eglTerminate(eglDisplay)
            }
            eglDisplay = EGL14.EGL_NO_DISPLAY
            eglContext = EGL14.EGL_NO_CONTEXT
            eglSurface = EGL14.EGL_NO_SURFACE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun VideoPlayerView(
    videoUri: Uri?,
    colorAdjustment: ColorAdjustment,
    comparisonMode: ComparisonMode,
    splitFraction: Float,
    onSplitFractionChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enhancementConfig: EnhancementConfig? = null
) {
    var glViewRef by remember { mutableStateOf<GlVideoTextureView?>(null) }
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
            glViewRef?.mediaPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        currentPositionMs = mp.currentPosition.toLong()
                    }
                } catch (_: Exception) {}
            }
            delay(100)
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
        // Hardware Accelerated OpenGL Video TextureView
        AndroidView(
            factory = { ctx ->
                GlVideoTextureView(ctx).apply {
                    id = View.generateViewId()
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.onPreparedCallback = { dur ->
                        durationMs = dur
                    }
                    this.onPlayingChanged = { playing ->
                        isPlaying = playing
                    }
                    setSource(videoUri, isLooping)
                    updateState(colorAdjustment, comparisonMode, splitFraction, enhancementConfig)
                    glViewRef = this
                }
            },
            update = { view ->
                view.setSource(videoUri, isLooping)
                view.updateState(colorAdjustment, comparisonMode, splitFraction, enhancementConfig)
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
                            glViewRef?.mediaPlayer?.isLooping = isLooping
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
                                        glViewRef?.mediaPlayer?.playbackParams = PlaybackParams().apply { speed = nextSpeed }
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
                            glViewRef?.mediaPlayer?.let { mp ->
                                val target = (currentPositionMs - 5000L).coerceAtLeast(0L)
                                currentPositionMs = target
                                mp.seekTo(target.toInt())
                                glViewRef?.renderFrame(updateTex = false)
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
                            glViewRef?.mediaPlayer?.let { mp ->
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
                            glViewRef?.mediaPlayer?.let { mp ->
                                val target = (currentPositionMs + 5000L).coerceAtMost(durationMs)
                                currentPositionMs = target
                                mp.seekTo(target.toInt())
                                glViewRef?.renderFrame(updateTex = false)
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
                            glViewRef?.mediaPlayer?.seekTo(currentPositionMs.toInt())
                            glViewRef?.renderFrame(updateTex = false)
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
                                glViewRef?.mediaPlayer?.setVolume(vol, vol)
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
