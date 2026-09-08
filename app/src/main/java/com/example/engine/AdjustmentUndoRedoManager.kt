package com.example.engine

import com.example.data.model.ColorAdjustment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Snapshot of a GPU pipeline adjustment state with human-readable change description.
 */
data class AdjustmentSnapshot(
    val adjustment: ColorAdjustment,
    val description: String,
    val modifiedParameter: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * High-performance Undo/Redo Manager for the GPU real-time rendering pipeline.
 * Tracks parameter adjustments, batches rapid continuous slider drags,
 * and allows reverting specific parameter changes or jumping back in history.
 */
class AdjustmentUndoRedoManager(
    private val maxHistorySize: Int = 50
) {
    private val _undoStack = MutableStateFlow<List<AdjustmentSnapshot>>(emptyList())
    val undoStack: StateFlow<List<AdjustmentSnapshot>> = _undoStack.asStateFlow()

    private val _redoStack = MutableStateFlow<List<AdjustmentSnapshot>>(emptyList())
    val redoStack: StateFlow<List<AdjustmentSnapshot>> = _redoStack.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private var lastRecordedTime = 0L
    private var lastParamName: String? = null

    init {
        // Initialize with default state
        val initial = AdjustmentSnapshot(
            adjustment = ColorAdjustment(),
            description = "Initial / Original State"
        )
        _undoStack.value = listOf(initial)
        updateFlags()
    }

    fun initializeWithState(initialAdj: ColorAdjustment) {
        val initial = AdjustmentSnapshot(
            adjustment = initialAdj,
            description = if (initialAdj.isDefault) "Original State" else "Loaded Preset: ${initialAdj.presetName}"
        )
        _undoStack.value = listOf(initial)
        _redoStack.value = emptyList()
        updateFlags()
    }

    /**
     * Record a new adjustment. If the same parameter is changed continuously within 400ms,
     * it coalesces the change to avoid cluttering the undo stack during slider scrubbing.
     */
    fun recordAdjustment(
        newAdj: ColorAdjustment,
        description: String,
        paramName: String? = null,
        forceNewStep: Boolean = false
    ) {
        val currentList = _undoStack.value.toMutableList()
        if (currentList.isEmpty()) {
            currentList.add(AdjustmentSnapshot(newAdj, description, paramName))
            _undoStack.value = currentList
            _redoStack.value = emptyList()
            updateFlags()
            return
        }

        val lastSnapshot = currentList.last()
        if (lastSnapshot.adjustment == newAdj) return

        val now = System.currentTimeMillis()
        val isRapidSameParam = !forceNewStep &&
                paramName != null &&
                paramName == lastParamName &&
                (now - lastRecordedTime) < 450L

        if (isRapidSameParam && currentList.size > 1) {
            // Replace the top element with latest value
            currentList[currentList.size - 1] = AdjustmentSnapshot(
                adjustment = newAdj,
                description = description,
                modifiedParameter = paramName,
                timestamp = now
            )
        } else {
            // New snapshot
            currentList.add(
                AdjustmentSnapshot(
                    adjustment = newAdj,
                    description = description,
                    modifiedParameter = paramName,
                    timestamp = now
                )
            )
            if (currentList.size > maxHistorySize) {
                currentList.removeAt(0)
            }
            // Clear redo on new action
            _redoStack.value = emptyList()
        }

        lastRecordedTime = now
        lastParamName = paramName
        _undoStack.value = currentList
        updateFlags()
    }

    /**
     * Reverts to the previous adjustment step in the GPU pipeline.
     */
    fun undo(): ColorAdjustment? {
        val uStack = _undoStack.value.toMutableList()
        if (uStack.size <= 1) return null

        val currentTop = uStack.removeAt(uStack.size - 1)
        val rStack = _redoStack.value.toMutableList()
        rStack.add(currentTop)

        _undoStack.value = uStack
        _redoStack.value = rStack
        lastParamName = null
        updateFlags()

        return uStack.lastOrNull()?.adjustment
    }

    /**
     * Re-applies the next adjustment step in the GPU pipeline.
     */
    fun redo(): ColorAdjustment? {
        val rStack = _redoStack.value.toMutableList()
        if (rStack.isEmpty()) return null

        val nextTop = rStack.removeAt(rStack.size - 1)
        val uStack = _undoStack.value.toMutableList()
        uStack.add(nextTop)

        _undoStack.value = uStack
        _redoStack.value = rStack
        lastParamName = null
        updateFlags()

        return nextTop.adjustment
    }

    /**
     * Revert a single specific parameter (e.g. "exposure" or "saturation") to its default or previous value,
     * without resetting other adjustments in the project.
     */
    fun revertParameter(current: ColorAdjustment, paramKey: String): ColorAdjustment {
        val reverted = when (paramKey.lowercase()) {
            "exposure" -> current.copy(exposure = 0f)
            "brightness" -> current.copy(brightness = 0f)
            "contrast" -> current.copy(contrast = 0f)
            "highlights" -> current.copy(highlights = 0f)
            "shadows" -> current.copy(shadows = 0f)
            "whites" -> current.copy(whites = 0f)
            "blacks" -> current.copy(blacks = 0f)
            "saturation" -> current.copy(saturation = 0f)
            "vibrance" -> current.copy(vibrance = 0f)
            "temperature" -> current.copy(temperature = 0f)
            "tint" -> current.copy(tint = 0f)
            "fade" -> current.copy(fade = 0f)
            "sharpness" -> current.copy(sharpness = 0f)
            "clarity" -> current.copy(clarity = 0f)
            "hueshift" -> current.copy(hueShift = 0f)
            "hslsaturation" -> current.copy(hslSaturation = 0f)
            "hslluminance" -> current.copy(hslLuminance = 0f)
            "rgbred" -> current.copy(rgbRed = 0f)
            "rgbgreen" -> current.copy(rgbGreen = 0f)
            "rgbblue" -> current.copy(rgbBlue = 0f)
            "lut" -> current.copy(lutIndex = 0, lutIntensity = 100f)
            "curves" -> current.copy(curveMaster = 0f, curveRed = 0f, curveGreen = 0f, curveBlue = 0f)
            "colorwheels" -> current.copy(shadowTintAmount = 0f, midtoneTintAmount = 0f, highlightTintAmount = 0f)
            else -> current
        }
        recordAdjustment(reverted, "Reset $paramKey", paramName = paramKey, forceNewStep = true)
        return reverted
    }

    /**
     * Jump directly to a historical snapshot index.
     */
    fun jumpToStep(index: Int): ColorAdjustment? {
        val uStack = _undoStack.value
        if (index < 0 || index >= uStack.size) return null

        val target = uStack[index]
        val newUndo = uStack.subList(0, index + 1).toMutableList()
        val undone = uStack.subList(index + 1, uStack.size).reversed()
        val newRedo = (_redoStack.value + undone).toMutableList()

        _undoStack.value = newUndo
        _redoStack.value = newRedo
        lastParamName = null
        updateFlags()

        return target.adjustment
    }

    fun clearAll() {
        val initial = AdjustmentSnapshot(ColorAdjustment(), "Reset All Adjustments")
        _undoStack.value = listOf(initial)
        _redoStack.value = emptyList()
        lastParamName = null
        updateFlags()
    }

    private fun updateFlags() {
        _canUndo.value = _undoStack.value.size > 1
        _canRedo.value = _redoStack.value.isNotEmpty()
    }
}
