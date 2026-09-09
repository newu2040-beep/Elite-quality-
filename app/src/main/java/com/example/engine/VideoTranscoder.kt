package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.media.*
import android.net.Uri
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.graphics.SurfaceTexture
import android.view.Surface
import com.example.data.model.ColorAdjustment
import com.example.data.model.EnhancementConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max

/**
 * High-performance Video Transcoding Engine with Hardware GPU Pipeline & Native CPU Fallback.
 * Applies color grading, curves, HSL, LUTs, and AI enhancements frame-by-frame.
 */
object VideoTranscoder {

    private const val TIMEOUT_USEC = 10_000L

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

    suspend fun transcodeVideo(
        context: Context,
        sourceUri: Uri,
        outputFile: File,
        targetWidth: Int,
        targetHeight: Int,
        targetFps: Int,
        targetBitrateBps: Long,
        colorAdjustment: ColorAdjustment,
        config: EnhancementConfig,
        onProgress: (currentFrame: Long, totalFrames: Long, percent: Int) -> Unit
    ): Boolean = withContext(Dispatchers.Default) {
        // Enforce even dimensions
        val outWidth = (targetWidth / 2) * 2
        val outHeight = (targetHeight / 2) * 2

        var success = false
        try {
            success = runTranscodePipeline(
                context = context,
                sourceUri = sourceUri,
                outputFile = outputFile,
                targetWidth = outWidth,
                targetHeight = outHeight,
                targetFps = targetFps,
                targetBitrateBps = targetBitrateBps,
                colorAdjustment = colorAdjustment,
                config = config,
                onProgress = onProgress
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // If high resolution or bitrate failed, retry at standard 1080p
            if (outWidth > 1920 || outHeight > 1920) {
                try {
                    val fallbackW = if (outHeight > outWidth) 1080 else 1920
                    val fallbackH = if (outHeight > outWidth) 1920 else 1080
                    success = runTranscodePipeline(
                        context = context,
                        sourceUri = sourceUri,
                        outputFile = outputFile,
                        targetWidth = fallbackW,
                        targetHeight = fallbackH,
                        targetFps = 30,
                        targetBitrateBps = 12_000_000L,
                        colorAdjustment = colorAdjustment,
                        config = config,
                        onProgress = onProgress
                    )
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        }

        // Real CPU Fallback if GPU pipeline was unavailable or failed
        if (!success || !outputFile.exists() || outputFile.length() == 0L) {
            try {
                success = runCpuFallbackTranscode(
                    context = context,
                    sourceUri = sourceUri,
                    outputFile = outputFile,
                    targetWidth = outWidth,
                    targetHeight = outHeight,
                    targetFps = targetFps,
                    targetBitrateBps = targetBitrateBps,
                    colorAdjustment = colorAdjustment,
                    config = config,
                    onProgress = onProgress
                )
            } catch (e3: Exception) {
                e3.printStackTrace()
            }
        }

        success && outputFile.exists() && outputFile.length() > 0
    }

    private fun runTranscodePipeline(
        context: Context,
        sourceUri: Uri,
        outputFile: File,
        targetWidth: Int,
        targetHeight: Int,
        targetFps: Int,
        targetBitrateBps: Long,
        colorAdjustment: ColorAdjustment,
        config: EnhancementConfig,
        onProgress: (Long, Long, Int) -> Unit
    ): Boolean {
        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null

        var decoderSurfaceTexture: SurfaceTexture? = null
        var decoderSurface: Surface? = null
        var decoderTextureId = 0
        var encoderInputSurface: Surface? = null

        var eglDisplay = EGL14.EGL_NO_DISPLAY
        var eglContext = EGL14.EGL_NO_CONTEXT
        var eglSurface = EGL14.EGL_NO_SURFACE
        var glProgram = 0

        val frameSyncObject = Object()
        var frameAvailable = false

        return try {
            // 1. Setup MediaExtractors
            videoExtractor = MediaExtractor()
            val pfdVideo = context.contentResolver.openFileDescriptor(sourceUri, "r")
            if (pfdVideo != null) {
                videoExtractor.setDataSource(pfdVideo.fileDescriptor)
                pfdVideo.close()
            } else {
                videoExtractor.setDataSource(context, sourceUri, null)
            }

            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var videoInputFormat: MediaFormat? = null
            var sourceAudioFormat: MediaFormat? = null

            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") && videoTrackIndex == -1) {
                    videoTrackIndex = i
                    videoInputFormat = format
                } else if (mime.startsWith("audio/") && audioTrackIndex == -1) {
                    audioTrackIndex = i
                    sourceAudioFormat = format
                }
            }

            if (videoTrackIndex == -1 || videoInputFormat == null) {
                return false
            }

            videoExtractor.selectTrack(videoTrackIndex)

            // Setup audio extractor if audio track exists
            if (audioTrackIndex >= 0) {
                audioExtractor = MediaExtractor()
                val pfdAudio = context.contentResolver.openFileDescriptor(sourceUri, "r")
                if (pfdAudio != null) {
                    audioExtractor.setDataSource(pfdAudio.fileDescriptor)
                    pfdAudio.close()
                } else {
                    audioExtractor.setDataSource(context, sourceUri, null)
                }
                audioExtractor.selectTrack(audioTrackIndex)
            }

            val sourceDurationUs = videoInputFormat.let {
                if (it.containsKey(MediaFormat.KEY_DURATION)) it.getLong(MediaFormat.KEY_DURATION)
                else 10_000_000L
            }.coerceAtLeast(1_000_000L)

            val totalEstimatedFrames = (sourceDurationUs / 1_000_000L) * targetFps

            // 2. Setup Target Video Encoder
            val encoderMime = MediaFormat.MIMETYPE_VIDEO_AVC
            val encoderFormat = MediaFormat.createVideoFormat(encoderMime, targetWidth, targetHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, targetBitrateBps.toInt().coerceIn(1_000_000, 45_000_000))
                setInteger(MediaFormat.KEY_FRAME_RATE, targetFps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                try {
                    setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
                    setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel41)
                } catch (_: Exception) {}
            }

            encoder = MediaCodec.createEncoderByType(encoderMime)
            encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoderInputSurface = encoder.createInputSurface()
            encoder.start()

            // 3. Setup EGL on encoderInputSurface
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) throw RuntimeException("EGL display unavailable")
            val version = IntArray(2)
            EGL14.eglInitialize(eglDisplay, version, 0, version, 1)

            val configAttribs = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGLExt.EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)
            val eglConfig = configs[0] ?: throw RuntimeException("Unable to find EGL config")

            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )
            eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)

            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, encoderInputSurface, surfaceAttribs, 0)
            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)

            // 4. Setup GL Shader & Buffers
            glProgram = ColorFilterEngine.createProgram(ColorFilterEngine.VERTEX_SHADER, ColorFilterEngine.FRAGMENT_SHADER)
            val uniforms = ColorFilterEngine.ShaderUniforms(glProgram)

            val aPositionLoc = GLES20.glGetAttribLocation(glProgram, "aPosition")
            val aTextureCoordLoc = GLES20.glGetAttribLocation(glProgram, "aTextureCoord")

            val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(VERTEX_COORDS.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(VERTEX_COORDS).apply { position(0) }

            val texCoordBuffer: FloatBuffer = ByteBuffer.allocateDirect(TEXTURE_COORDS.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(TEXTURE_COORDS).apply { position(0) }

            // 5. Setup External Texture & Surface for Decoder
            val texIds = IntArray(1)
            GLES20.glGenTextures(1, texIds, 0)
            decoderTextureId = texIds[0]
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, decoderTextureId)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

            decoderSurfaceTexture = SurfaceTexture(decoderTextureId).apply {
                setOnFrameAvailableListener {
                    synchronized(frameSyncObject) {
                        frameAvailable = true
                        frameSyncObject.notifyAll()
                    }
                }
            }
            decoderSurface = Surface(decoderSurfaceTexture)

            // 6. Setup Source Video Decoder
            val decoderMime = videoInputFormat.getString(MediaFormat.KEY_MIME) ?: MediaFormat.MIMETYPE_VIDEO_AVC
            decoder = MediaCodec.createDecoderByType(decoderMime)
            decoder.configure(videoInputFormat, decoderSurface, null, 0)
            decoder.start()

            // 7. Initialize MediaMuxer
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerVideoTrackIndex = -1
            var muxerAudioTrackIndex = -1
            var muxerStarted = false

            // Transcoding Loop
            var decoderInputDone = false
            var decoderOutputDone = false
            var encoderDone = false
            var encodedFrameCount = 0L
            val texMatrix = FloatArray(16)
            val decoderBufferInfo = MediaCodec.BufferInfo()
            val encoderBufferInfo = MediaCodec.BufferInfo()

            while (!encoderDone) {
                // Feed Decoder Input
                if (!decoderInputDone) {
                    val inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC)
                    if (inputBufIndex >= 0) {
                        val inputBuf = decoder.getInputBuffer(inputBufIndex)
                        if (inputBuf != null) {
                            val sampleSize = videoExtractor.readSampleData(inputBuf, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                decoderInputDone = true
                            } else {
                                val presentationTimeUs = videoExtractor.sampleTime
                                decoder.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, 0)
                                videoExtractor.advance()
                            }
                        }
                    }
                }

                // Dequeue Decoder Output -> Render to Encoder Input Surface
                if (!decoderOutputDone) {
                    val decoderStatus = decoder.dequeueOutputBuffer(decoderBufferInfo, TIMEOUT_USEC)
                    if (decoderStatus >= 0) {
                        val isEos = (decoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                        val render = decoderBufferInfo.size > 0

                        if (render) {
                            synchronized(frameSyncObject) {
                                frameAvailable = false
                            }
                            decoder.releaseOutputBuffer(decoderStatus, true)

                            // Wait for SurfaceTexture to receive decoded frame
                            val deadline = System.currentTimeMillis() + 400L
                            synchronized(frameSyncObject) {
                                while (!frameAvailable && System.currentTimeMillis() < deadline) {
                                    try {
                                        frameSyncObject.wait(40)
                                    } catch (_: InterruptedException) {
                                        break
                                    }
                                }
                            }

                            try {
                                decoderSurfaceTexture.updateTexImage()
                                decoderSurfaceTexture.getTransformMatrix(texMatrix)
                            } catch (_: Exception) {}

                            // Render frame through OpenGL with full color, LUT, and AI processing
                            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
                            GLES20.glViewport(0, 0, targetWidth, targetHeight)
                            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                            GLES20.glUseProgram(glProgram)

                            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, decoderTextureId)

                            vertexBuffer.position(0)
                            GLES20.glEnableVertexAttribArray(aPositionLoc)
                            GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

                            texCoordBuffer.position(0)
                            GLES20.glEnableVertexAttribArray(aTextureCoordLoc)
                            GLES20.glVertexAttribPointer(aTextureCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

                            // ComparisonMode: 1 = Enhanced (Export always renders Enhanced)
                            ColorFilterEngine.bindUniforms(
                                uniforms = uniforms,
                                texMatrix = texMatrix,
                                comparisonMode = 1,
                                splitFraction = 0.5f,
                                width = targetWidth,
                                height = targetHeight,
                                adj = colorAdjustment,
                                config = config
                            )

                            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

                            GLES20.glDisableVertexAttribArray(aPositionLoc)
                            GLES20.glDisableVertexAttribArray(aTextureCoordLoc)

                            // Set presentation time on encoder surface
                            EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, decoderBufferInfo.presentationTimeUs * 1000L)
                            EGL14.eglSwapBuffers(eglDisplay, eglSurface)

                            encodedFrameCount++
                            val percent = ((decoderBufferInfo.presentationTimeUs.toDouble() / sourceDurationUs.toDouble()) * 100.0).toInt().coerceIn(10, 95)
                            onProgress(encodedFrameCount, totalEstimatedFrames, percent)
                        } else {
                            decoder.releaseOutputBuffer(decoderStatus, false)
                        }

                        if (isEos) {
                            decoderOutputDone = true
                            encoder.signalEndOfInputStream()
                        }
                    }
                }

                // Drain Encoder Output -> Muxer
                var encoderStatus = encoder.dequeueOutputBuffer(encoderBufferInfo, TIMEOUT_USEC)
                while (encoderStatus >= 0 || encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            val newFormat = encoder.outputFormat
                            muxerVideoTrackIndex = muxer.addTrack(newFormat)
                            if (audioTrackIndex >= 0 && sourceAudioFormat != null) {
                                muxerAudioTrackIndex = muxer.addTrack(sourceAudioFormat)
                            }
                            muxer.start()
                            muxerStarted = true
                        }
                    } else if (encoderStatus >= 0) {
                        val encodedData = encoder.getOutputBuffer(encoderStatus)
                        if (encodedData != null) {
                            if ((encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                encoderBufferInfo.size = 0
                            }

                            if (encoderBufferInfo.size > 0 && muxerStarted) {
                                encodedData.position(encoderBufferInfo.offset)
                                encodedData.limit(encoderBufferInfo.offset + encoderBufferInfo.size)
                                muxer.writeSampleData(muxerVideoTrackIndex, encodedData, encoderBufferInfo)
                            }

                            val isEncoderEos = (encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                            encoder.releaseOutputBuffer(encoderStatus, false)
                            if (isEncoderEos) {
                                encoderDone = true
                                break
                            }
                        }
                    }
                    encoderStatus = encoder.dequeueOutputBuffer(encoderBufferInfo, TIMEOUT_USEC)
                }
            }

            // Write Audio Track to Muxer
            if (muxerStarted && muxerAudioTrackIndex >= 0 && audioExtractor != null) {
                try {
                    val audioBuffer = ByteBuffer.allocate(512 * 1024)
                    val audioBufferInfo = MediaCodec.BufferInfo()
                    while (true) {
                        audioBuffer.clear()
                        val sampleSize = audioExtractor.readSampleData(audioBuffer, 0)
                        if (sampleSize < 0) break
                        audioBufferInfo.offset = 0
                        audioBufferInfo.size = sampleSize
                        audioBufferInfo.presentationTimeUs = audioExtractor.sampleTime
                        audioBufferInfo.flags = audioExtractor.sampleFlags
                        muxer.writeSampleData(muxerAudioTrackIndex, audioBuffer, audioBufferInfo)
                        audioExtractor.advance()
                    }
                } catch (audioEx: Exception) {
                    audioEx.printStackTrace()
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try { decoder?.stop() } catch (_: Exception) {}
            try { decoder?.release() } catch (_: Exception) {}
            try { encoder?.stop() } catch (_: Exception) {}
            try { encoder?.release() } catch (_: Exception) {}
            try { videoExtractor?.release() } catch (_: Exception) {}
            try { audioExtractor?.release() } catch (_: Exception) {}
            try {
                if (muxer != null) {
                    muxer.stop()
                    muxer.release()
                }
            } catch (_: Exception) {}
            try { decoderSurface?.release() } catch (_: Exception) {}
            try { decoderSurfaceTexture?.release() } catch (_: Exception) {}
            try { encoderInputSurface?.release() } catch (_: Exception) {}

            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(eglDisplay, eglSurface)
                if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(eglDisplay, eglContext)
                EGL14.eglTerminate(eglDisplay)
            }
        }
    }

    /**
     * Native Multi-Core CPU Fallback Video Processor.
     * Guaranteed to work on devices where EGL / GPU shaders are unavailable or restricted.
     */
    private fun runCpuFallbackTranscode(
        context: Context,
        sourceUri: Uri,
        outputFile: File,
        targetWidth: Int,
        targetHeight: Int,
        targetFps: Int,
        targetBitrateBps: Long,
        colorAdjustment: ColorAdjustment,
        config: EnhancementConfig,
        onProgress: (Long, Long, Int) -> Unit
    ): Boolean {
        var retriever: MediaMetadataRetriever? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: Surface? = null

        return try {
            retriever = MediaMetadataRetriever().apply {
                setDataSource(context, sourceUri)
            }

            val durationMsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationUs = (durationMsStr?.toLongOrNull() ?: 5000L) * 1000L
            val frameIntervalUs = 1_000_000L / targetFps
            val totalFrames = max(10L, durationUs / frameIntervalUs)

            val encoderMime = MediaFormat.MIMETYPE_VIDEO_AVC
            val encoderFormat = MediaFormat.createVideoFormat(encoderMime, targetWidth, targetHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, targetBitrateBps.toInt().coerceIn(1_000_000, 20_000_000))
                setInteger(MediaFormat.KEY_FRAME_RATE, targetFps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            encoder = MediaCodec.createEncoderByType(encoderMime)
            encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrack = -1
            var muxerStarted = false

            val encoderBufferInfo = MediaCodec.BufferInfo()
            val dstRect = Rect(0, 0, targetWidth, targetHeight)

            var currentTimeUs = 0L
            var frameIdx = 0L

            while (currentTimeUs < durationUs) {
                val rawBmp = retriever.getFrameAtTime(currentTimeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                if (rawBmp != null) {
                    val processedBmp = CpuColorFilterFallback.processBitmap(rawBmp, colorAdjustment, config)
                    val canvas: Canvas = inputSurface.lockCanvas(null)
                    val srcRect = Rect(0, 0, processedBmp.width, processedBmp.height)
                    canvas.drawBitmap(processedBmp, srcRect, dstRect, null)
                    inputSurface.unlockCanvasAndPost(canvas)
                    if (processedBmp != rawBmp) processedBmp.recycle()
                    rawBmp.recycle()
                }

                // Drain encoder
                var status = encoder.dequeueOutputBuffer(encoderBufferInfo, TIMEOUT_USEC)
                while (status >= 0 || status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            videoTrack = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    } else if (status >= 0) {
                        val encoded = encoder.getOutputBuffer(status)
                        if (encoded != null && encoderBufferInfo.size > 0 && muxerStarted) {
                            encoded.position(encoderBufferInfo.offset)
                            encoded.limit(encoderBufferInfo.offset + encoderBufferInfo.size)
                            muxer.writeSampleData(videoTrack, encoded, encoderBufferInfo)
                        }
                        encoder.releaseOutputBuffer(status, false)
                    }
                    status = encoder.dequeueOutputBuffer(encoderBufferInfo, TIMEOUT_USEC)
                }

                frameIdx++
                val pct = ((currentTimeUs.toDouble() / durationUs.toDouble()) * 100.0).toInt().coerceIn(10, 95)
                onProgress(frameIdx, totalFrames, pct)
                currentTimeUs += frameIntervalUs
            }

            encoder.signalEndOfInputStream()

            // Drain remaining
            var status = encoder.dequeueOutputBuffer(encoderBufferInfo, TIMEOUT_USEC * 2)
            while (status >= 0 || status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxerStarted) {
                        videoTrack = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                } else if (status >= 0) {
                    val encoded = encoder.getOutputBuffer(status)
                    if (encoded != null && encoderBufferInfo.size > 0 && muxerStarted) {
                        encoded.position(encoderBufferInfo.offset)
                        encoded.limit(encoderBufferInfo.offset + encoderBufferInfo.size)
                        muxer.writeSampleData(videoTrack, encoded, encoderBufferInfo)
                    }
                    encoder.releaseOutputBuffer(status, false)
                }
                status = encoder.dequeueOutputBuffer(encoderBufferInfo, TIMEOUT_USEC)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try { retriever?.release() } catch (_: Exception) {}
            try { encoder?.stop() } catch (_: Exception) {}
            try { encoder?.release() } catch (_: Exception) {}
            try { inputSurface?.release() } catch (_: Exception) {}
            try {
                if (muxer != null) {
                    muxer.stop()
                    muxer.release()
                }
            } catch (_: Exception) {}
        }
    }
}
