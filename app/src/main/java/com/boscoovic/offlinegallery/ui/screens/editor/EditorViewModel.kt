package com.boscoovic.offlinegallery.ui.screens.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boscoovic.offlinegallery.data.model.MediaItem
import com.boscoovic.offlinegallery.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class EditorTool {
    CROP_ROTATE,
    ADJUST,
    FILTERS,
    DRAW
}

enum class CropAspectRatio(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_9_16("9:16", 9f / 16f)
}

enum class FilterPreset {
    ORIGINAL,
    VIVID,
    WARM,
    COOL,
    BW,
    SEPIA,
    DRAMATIC
}

data class DrawPath(
    val path: Path,
    val color: Int,
    val strokeWidth: Float
)

data class EditorState(
    val rotationDegrees: Float = 0f,
    val isFlippedHorizontal: Boolean = false,
    val brightness: Float = 0f, // -100 to 100
    val contrast: Float = 1f, // 0.5 to 1.5
    val saturation: Float = 1f, // 0 to 2
    val warmth: Float = 0f, // -50 to 50
    val activeFilter: FilterPreset = FilterPreset.ORIGINAL,
    val drawPaths: List<DrawPath> = emptyList()
)

data class EditorUiState(
    val sourceItem: MediaItem? = null,
    val originalBitmap: Bitmap? = null,
    val previewBitmap: Bitmap? = null,
    val currentTool: EditorTool = EditorTool.CROP_ROTATE,
    val selectedCropRatio: CropAspectRatio = CropAspectRatio.FREE,
    val currentState: EditorState = EditorState(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isSaving: Boolean = false,
    val savedUri: Uri? = null
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repository: MediaRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val mediaId: Long = savedStateHandle.get<String>("mediaId")?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<Pair<Bitmap, EditorState>>()
    private val redoStack = mutableListOf<Pair<Bitmap, EditorState>>()

    init {
        loadBitmap()
    }

    private fun loadBitmap() {
        viewModelScope.launch {
            val allMedia = repository.getAllMedia().first()
            val item = allMedia.find { it.id == mediaId }
            if (item != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, item.uri)
                        _uiState.value = _uiState.value.copy(
                            sourceItem = item,
                            originalBitmap = bitmap,
                            previewBitmap = bitmap
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun selectTool(tool: EditorTool) {
        _uiState.value = _uiState.value.copy(currentTool = tool)
    }

    fun setCropAspectRatio(ratio: CropAspectRatio) {
        _uiState.value = _uiState.value.copy(selectedCropRatio = ratio)
    }

    fun applyCrop(leftFraction: Float, topFraction: Float, rightFraction: Float, bottomFraction: Float) {
        val bmp = _uiState.value.previewBitmap ?: return
        val currentLeft = (bmp.width * leftFraction.coerceIn(0f, 1f)).toInt()
        val currentTop = (bmp.height * topFraction.coerceIn(0f, 1f)).toInt()
        val currentRight = (bmp.width * rightFraction.coerceIn(0f, 1f)).toInt()
        val currentBottom = (bmp.height * bottomFraction.coerceIn(0f, 1f)).toInt()

        val width = (currentRight - currentLeft).coerceAtLeast(10)
        val height = (currentBottom - currentTop).coerceAtLeast(10)

        pushUndo()
        try {
            val cropped = Bitmap.createBitmap(bmp, currentLeft, currentTop, width, height)
            _uiState.value = _uiState.value.copy(
                originalBitmap = cropped,
                previewBitmap = cropped,
                canUndo = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun rotate90() {
        pushUndo()
        val current = _uiState.value.currentState
        val newRotation = (current.rotationDegrees + 90f) % 360f
        applyState(current.copy(rotationDegrees = newRotation))
    }

    fun flipHorizontal() {
        pushUndo()
        val current = _uiState.value.currentState
        applyState(current.copy(isFlippedHorizontal = !current.isFlippedHorizontal))
    }

    fun setBrightness(brightness: Float) {
        val current = _uiState.value.currentState
        applyState(current.copy(brightness = brightness))
    }

    fun setContrast(contrast: Float) {
        val current = _uiState.value.currentState
        applyState(current.copy(contrast = contrast))
    }

    fun setSaturation(saturation: Float) {
        val current = _uiState.value.currentState
        applyState(current.copy(saturation = saturation))
    }

    fun setWarmth(warmth: Float) {
        val current = _uiState.value.currentState
        applyState(current.copy(warmth = warmth))
    }

    fun setFilter(filter: FilterPreset) {
        pushUndo()
        val current = _uiState.value.currentState
        applyState(current.copy(activeFilter = filter))
    }

    fun addDrawPath(drawPath: DrawPath) {
        pushUndo()
        val current = _uiState.value.currentState
        applyState(current.copy(drawPaths = current.drawPaths + drawPath))
    }

    fun reset() {
        pushUndo()
        applyState(EditorState())
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val currentBmp = _uiState.value.originalBitmap ?: return
            redoStack.add(Pair(currentBmp, _uiState.value.currentState))
            val (previousBmp, previousState) = undoStack.removeAt(undoStack.lastIndex)
            _uiState.value = _uiState.value.copy(
                originalBitmap = previousBmp,
                currentState = previousState,
                canUndo = undoStack.isNotEmpty(),
                canRedo = true
            )
            renderPreview(previousState, previousBmp)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val currentBmp = _uiState.value.originalBitmap ?: return
            undoStack.add(Pair(currentBmp, _uiState.value.currentState))
            val (nextBmp, nextState) = redoStack.removeAt(redoStack.lastIndex)
            _uiState.value = _uiState.value.copy(
                originalBitmap = nextBmp,
                currentState = nextState,
                canUndo = true,
                canRedo = redoStack.isNotEmpty()
            )
            renderPreview(nextState, nextBmp)
        }
    }

    private fun pushUndo() {
        val currentBmp = _uiState.value.originalBitmap ?: return
        undoStack.add(Pair(currentBmp, _uiState.value.currentState))
        redoStack.clear()
    }

    private fun applyState(state: EditorState) {
        _uiState.value = _uiState.value.copy(
            currentState = state,
            canUndo = true,
            canRedo = redoStack.isNotEmpty()
        )
        renderPreview(state, _uiState.value.originalBitmap)
    }

    private fun renderPreview(state: EditorState, srcBmp: Bitmap?) {
        val src = srcBmp ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val processed = applyEditsToBitmap(src, state)
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(previewBitmap = processed)
            }
        }
    }

    private fun applyEditsToBitmap(src: Bitmap, state: EditorState): Bitmap {
        val matrix = Matrix().apply {
            postRotate(state.rotationDegrees)
            if (state.isFlippedHorizontal) postScale(-1f, 1f)
        }
        val transformed = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)

        val output = Bitmap.createBitmap(transformed.width, transformed.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(state.saturation)

        val scale = state.contrast
        val translate = state.brightness
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(contrastMatrix)

        when (state.activeFilter) {
            FilterPreset.BW -> {
                val bw = ColorMatrix().apply { setSaturation(0f) }
                colorMatrix.postConcat(bw)
            }
            FilterPreset.SEPIA -> {
                val sepia = ColorMatrix(
                    floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                colorMatrix.postConcat(sepia)
            }
            FilterPreset.VIVID -> {
                val vivid = ColorMatrix().apply { setSaturation(1.5f) }
                colorMatrix.postConcat(vivid)
            }
            FilterPreset.WARM -> {
                val warm = ColorMatrix(
                    floatArrayOf(
                        1.2f, 0f, 0f, 0f, 10f,
                        0f, 1.0f, 0f, 0f, 0f,
                        0f, 0f, 0.8f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                colorMatrix.postConcat(warm)
            }
            FilterPreset.COOL -> {
                val cool = ColorMatrix(
                    floatArrayOf(
                        0.8f, 0f, 0f, 0f, -10f,
                        0f, 1.0f, 0f, 0f, 0f,
                        0f, 0f, 1.2f, 0f, 10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                colorMatrix.postConcat(cool)
            }
            FilterPreset.DRAMATIC -> {
                val dramatic = ColorMatrix(
                    floatArrayOf(
                        1.3f, 0f, 0f, 0f, -15f,
                        0f, 1.3f, 0f, 0f, -15f,
                        0f, 0f, 1.3f, 0f, -15f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                colorMatrix.postConcat(dramatic)
            }
            FilterPreset.ORIGINAL -> {}
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(transformed, 0f, 0f, paint)

        val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        for (drawPath in state.drawPaths) {
            drawPaint.color = drawPath.color
            drawPaint.strokeWidth = drawPath.strokeWidth
            canvas.drawPath(drawPath.path, drawPaint)
        }

        return output
    }

    fun saveAsCopy(onSuccess: (Uri) -> Unit) {
        val srcItem = _uiState.value.sourceItem ?: return
        val finalBitmap = _uiState.value.previewBitmap ?: return

        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            val uri = repository.saveEditedImage(srcItem, finalBitmap)
            _uiState.value = _uiState.value.copy(isSaving = false, savedUri = uri)
            if (uri != null) {
                onSuccess(uri)
            }
        }
    }

    fun saveAsDefault(onSuccess: () -> Unit) {
        val srcItem = _uiState.value.sourceItem ?: return
        val finalBitmap = _uiState.value.previewBitmap ?: return

        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            val success = repository.overwriteOriginalImage(srcItem, finalBitmap)
            _uiState.value = _uiState.value.copy(isSaving = false)
            if (success) {
                onSuccess()
            }
        }
    }
}
