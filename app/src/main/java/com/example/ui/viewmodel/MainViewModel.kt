package com.example.ui.viewmodel

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.ExportEntity
import com.example.data.db.PresetEntity
import com.example.data.db.ProjectEntity
import com.example.data.model.*
import com.example.data.repository.*
import com.example.engine.ColorFilterEngine
import com.example.engine.EliteEnhanceEngine
import com.example.engine.NotificationHelper
import com.example.engine.VideoExportHelper
import com.example.engine.VideoMetadataExtractor
import com.example.ui.components.ComparisonMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val projectRepo = ProjectRepository(db.projectDao())
    val presetRepo = PresetRepository(db.presetDao())
    val exportRepo = ExportRepository(db.exportDao())
    val settingsRepo = SettingsRepository(application)
    val storageRepo = StorageRepository(application)

    private val metadataExtractor = VideoMetadataExtractor(application)
    val enhanceEngine = EliteEnhanceEngine(application)

    // Current Active Video & Project
    private val _currentVideoUri = MutableStateFlow<Uri?>(null)
    val currentVideoUri: StateFlow<Uri?> = _currentVideoUri.asStateFlow()

    private val _currentMetadata = MutableStateFlow<VideoMetadata?>(null)
    val currentMetadata: StateFlow<VideoMetadata?> = _currentMetadata.asStateFlow()

    private val _colorAdjustment = MutableStateFlow(ColorAdjustment())
    val colorAdjustment: StateFlow<ColorAdjustment> = _colorAdjustment.asStateFlow()

    private val _enhancementConfig = MutableStateFlow(EnhancementConfig())
    val enhancementConfig: StateFlow<EnhancementConfig> = _enhancementConfig.asStateFlow()

    private val _comparisonMode = MutableStateFlow(ComparisonMode.SPLIT)
    val comparisonMode: StateFlow<ComparisonMode> = _comparisonMode.asStateFlow()

    private val _splitFraction = MutableStateFlow(0.5f)
    val splitFraction: StateFlow<Float> = _splitFraction.asStateFlow()

    private val _activeEditingCategory = MutableStateFlow("Adjust")
    val activeEditingCategory: StateFlow<String> = _activeEditingCategory.asStateFlow()

    private val _currentProjectId = MutableStateFlow<Long?>(null)
    val currentProjectId: StateFlow<Long?> = _currentProjectId.asStateFlow()

    // Last Export & Auto-Save
    private val _lastExportedFile = MutableStateFlow<File?>(null)
    val lastExportedFile: StateFlow<File?> = _lastExportedFile.asStateFlow()

    private val _autoSavedToGallery = MutableStateFlow(false)
    val autoSavedToGallery: StateFlow<Boolean> = _autoSavedToGallery.asStateFlow()

    val processingProgress: StateFlow<ProcessingProgress> = enhanceEngine.progress

    // Real device videos loaded from MediaStore
    private val _deviceVideos = MutableStateFlow<List<DeviceVideoItem>>(emptyList())
    val deviceVideos: StateFlow<List<DeviceVideoItem>> = _deviceVideos.asStateFlow()

    // Room Flows
    val allProjects = projectRepo.allProjects.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val recentProjects = projectRepo.recentProjects.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allPresets = presetRepo.allPresets.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allExports = exportRepo.allExports.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Storage Stats
    private val _storageStats = MutableStateFlow(storageRepo.getStorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    init {
        viewModelScope.launch {
            presetRepo.initDefaultPresetsIfEmpty()
            refreshStorageStats()
            loadDeviceVideos()
        }
    }

    fun refreshStorageStats() {
        _storageStats.value = storageRepo.getStorageStats()
    }

    fun loadDeviceVideos() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<DeviceVideoItem>()
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_MODIFIED
            )
            val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            try {
                getApplication<Application>().contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                    val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                    val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                    val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                    val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                    val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                    val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)

                    var count = 0
                    while (cursor.moveToNext() && count < 25) {
                        if (idCol != -1) {
                            val id = cursor.getLong(idCol)
                            val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                            val name = if (nameCol != -1) cursor.getString(nameCol) ?: "Video" else "Video"
                            val duration = if (durationCol != -1) cursor.getLong(durationCol) else 0L
                            val width = if (widthCol != -1) cursor.getInt(widthCol) else 1920
                            val height = if (heightCol != -1) cursor.getInt(heightCol) else 1080
                            val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                            val dateMod = if (dateCol != -1) cursor.getLong(dateCol) else 0L
                            list.add(DeviceVideoItem(uri, name, duration, width, height, size, dateMod))
                            count++
                        }
                    }
                }
            } catch (_: Exception) {}
            _deviceVideos.value = list
        }
    }

    fun selectVideo(uri: Uri) {
        _currentVideoUri.value = uri
        viewModelScope.launch {
            val meta = metadataExtractor.extractMetadata(uri)
            _currentMetadata.value = meta
            val thumb = metadataExtractor.extractThumbnail(uri)

            // Create or save project in Room
            val projId = projectRepo.saveProject(
                ProjectEntity(
                    title = meta.fileName,
                    sourceUri = uri.toString(),
                    thumbnailPath = thumb,
                    durationMs = meta.durationMs,
                    width = meta.width,
                    height = meta.height,
                    fps = meta.fps,
                    codec = meta.codec,
                    bitrateBps = meta.bitrateBps,
                    fileSizeBytes = meta.fileSizeBytes,
                    enhancementPreset = "Cinematic 4K",
                    status = "Draft"
                )
            )
            _currentProjectId.value = projId
        }
    }

    fun updateColorAdjustment(update: (ColorAdjustment) -> ColorAdjustment) {
        _colorAdjustment.value = update(_colorAdjustment.value)
    }

    fun updateEnhancementConfig(update: (EnhancementConfig) -> EnhancementConfig) {
        _enhancementConfig.value = update(_enhancementConfig.value)
    }

    fun setComparisonMode(mode: ComparisonMode) {
        _comparisonMode.value = mode
    }

    fun setSplitFraction(fraction: Float) {
        _splitFraction.value = fraction
    }

    fun setActiveCategory(category: String) {
        _activeEditingCategory.value = category
    }

    fun applyPreset(preset: PresetEntity) {
        _colorAdjustment.value = _colorAdjustment.value.copy(
            exposure = preset.exposure,
            contrast = preset.contrast,
            highlights = preset.highlights,
            shadows = preset.shadows,
            saturation = preset.saturation,
            vibrance = preset.vibrance,
            temperature = preset.temperature,
            tint = preset.tint,
            sharpness = preset.sharpness,
            clarity = preset.clarity,
            presetName = preset.name
        )
        _enhancementConfig.value = _enhancementConfig.value.copy(
            aiDenoise = preset.aiDenoise,
            aiSharpen = preset.aiSharpen,
            aiUpscale = preset.aiUpscale
        )
    }

    fun resetColorAdjustment() {
        _colorAdjustment.value = ColorAdjustment()
    }

    fun startEliteEnhancement(onComplete: (File) -> Unit) {
        val uri = _currentVideoUri.value ?: return
        val meta = _currentMetadata.value ?: return

        viewModelScope.launch {
            val result = enhanceEngine.processVideo(
                sourceUri = uri,
                metadata = meta,
                colorAdjustment = _colorAdjustment.value,
                config = _enhancementConfig.value
            )

            if (result.isFinished && result.outputFilePath != null) {
                val exportedFile = File(result.outputFilePath)
                _lastExportedFile.value = exportedFile

                // AUTOMATICALLY SAVE TO DEVICE GALLERY
                val savedUri = withContext(Dispatchers.IO) {
                    VideoExportHelper.saveToGallery(
                        getApplication(),
                        exportedFile.absolutePath,
                        exportedFile.name
                    )
                }
                val autoSaved = (savedUri != null)
                _autoSavedToGallery.value = autoSaved

                // Trigger System Notification
                NotificationHelper.showEnhancementCompleteNotification(
                    context = getApplication(),
                    fileName = exportedFile.name,
                    resolution = result.outputResolutionText,
                    savedToGallery = autoSaved
                )

                // Record in Room Database
                val exportId = exportRepo.saveExport(
                    ExportEntity(
                        projectId = _currentProjectId.value ?: 0L,
                        title = meta.fileName.substringBeforeLast(".") + "_ELITE.mp4",
                        outputUri = savedUri?.toString() ?: result.outputFileUri ?: "",
                        outputFilePath = result.outputFilePath,
                        resolution = result.outputResolutionText,
                        fps = _enhancementConfig.value.targetFps.fps.let { if (it > 0) it else meta.fps.toInt() },
                        codec = _enhancementConfig.value.targetCodec.label,
                        bitrateMbps = _enhancementConfig.value.targetBitrate.mbps,
                        fileSizeBytes = exportedFile.length()
                    )
                )

                // Update Project Entity
                _currentProjectId.value?.let { pId ->
                    val proj = projectRepo.getProjectById(pId)
                    if (proj != null) {
                        projectRepo.updateProject(
                            proj.copy(
                                status = "Enhanced",
                                updatedAt = System.currentTimeMillis(),
                                lastExportUri = savedUri?.toString() ?: result.outputFileUri,
                                lastExportPath = result.outputFilePath
                            )
                        )
                    }
                }

                refreshStorageStats()
                loadDeviceVideos()
                onComplete(exportedFile)
            }
        }
    }

    fun cancelEnhancement() {
        enhanceEngine.cancel()
    }

    fun shareLastExport() {
        val file = _lastExportedFile.value ?: return
        VideoExportHelper.shareVideo(getApplication(), file.absolutePath)
    }

    fun openLastExport() {
        val file = _lastExportedFile.value ?: return
        VideoExportHelper.openVideo(getApplication(), file.absolutePath)
    }

    fun saveLastExportToGallery(onSaved: (Boolean) -> Unit) {
        val file = _lastExportedFile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val savedUri = VideoExportHelper.saveToGallery(
                getApplication(),
                file.absolutePath,
                file.name
            )
            withContext(Dispatchers.Main) {
                val success = savedUri != null
                _autoSavedToGallery.value = success
                onSaved(success)
            }
        }
    }

    fun clearAppCache() {
        viewModelScope.launch(Dispatchers.IO) {
            storageRepo.clearCache()
            refreshStorageStats()
        }
    }

    fun deleteTempFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            storageRepo.deleteTempFiles()
            refreshStorageStats()
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            projectRepo.deleteProject(id)
            refreshStorageStats()
        }
    }
}
