package com.boscoovic.offlinegallery.ui.screens.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boscoovic.offlinegallery.R
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSaveMenu by remember { mutableStateOf(false) }

    // Crop box fractions (0f .. 1f)
    var cropLeft by remember { mutableFloatStateOf(0.05f) }
    var cropTop by remember { mutableFloatStateOf(0.05f) }
    var cropRight by remember { mutableFloatStateOf(0.95f) }
    var cropBottom by remember { mutableFloatStateOf(0.95f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        // Top Bar
        TopAppBar(
            title = { Text(stringResource(R.string.edit), color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = { viewModel.reset() }) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset), tint = Color.White)
                }
                IconButton(onClick = { viewModel.undo() }, enabled = uiState.canUndo) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = stringResource(R.string.undo),
                        tint = if (uiState.canUndo) Color.White else Color.Gray
                    )
                }
                IconButton(onClick = { viewModel.redo() }, enabled = uiState.canRedo) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = stringResource(R.string.redo),
                        tint = if (uiState.canRedo) Color.White else Color.Gray
                    )
                }

                // Save button with picker menu
                Box {
                    Button(
                        onClick = { showSaveMenu = true },
                        enabled = !uiState.isSaving,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        } else {
                            Text(stringResource(R.string.save))
                        }
                    }

                    DropdownMenu(
                        expanded = showSaveMenu,
                        onDismissRequest = { showSaveMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Save as copy (Default)") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showSaveMenu = false
                                viewModel.saveAsCopy { onSaved() }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Save as default (Overwrite)") },
                            leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
                            onClick = {
                                showSaveMenu = false
                                viewModel.saveAsDefault { onSaved() }
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
        )

        // Center Preview Canvas with Crop Overlay
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val localDensity = LocalDensity.current
            val totalWidthPx = constraints.maxWidth.toFloat()
            val totalHeightPx = constraints.maxHeight.toFloat()
            var isComparingOriginal by remember { mutableStateOf(false) }

            val displayedBitmap = if (isComparingOriginal) {
                uiState.originalBitmap ?: uiState.previewBitmap
            } else {
                uiState.previewBitmap
            }

            displayedBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Edited Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // Compare Original Pill (hold to preview original unedited image)
                if (uiState.canUndo) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isComparingOriginal) MaterialTheme.colorScheme.primary
                                else Color.Black.copy(alpha = 0.65f)
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isComparingOriginal = true
                                        tryAwaitRelease()
                                        isComparingOriginal = false
                                    }
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = "Hold to compare original",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isComparingOriginal) "ORIGINAL" else "Hold to compare",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Interactive Crop Guide mapped accurately to the rendered image bounds
                if (uiState.currentTool == EditorTool.CROP_ROTATE && totalWidthPx > 0f && totalHeightPx > 0f) {
                    val bmpAspect = bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
                    val boxAspect = totalWidthPx / totalHeightPx
                    val imgW: Float
                    val imgH: Float
                    if (bmpAspect > boxAspect) {
                        imgW = totalWidthPx
                        imgH = totalWidthPx / bmpAspect
                    } else {
                        imgH = totalHeightPx
                        imgW = totalHeightPx * bmpAspect
                    }
                    val imgLeft = (totalWidthPx - imgW) / 2f
                    val imgTop = (totalHeightPx - imgH) / 2f

                    val boxX = imgLeft + imgW * cropLeft
                    val boxY = imgTop + imgH * cropTop
                    val boxW = imgW * (cropRight - cropLeft)
                    val boxH = imgH * (cropBottom - cropTop)
                    val boxWDp = with(localDensity) { boxW.toDp() }
                    val boxHDp = with(localDensity) { boxH.toDp() }
                    val minSpan = 0.15f
                    val lockedRatio = uiState.selectedCropRatio.ratio

                    // Darkened scrim outside crop area + rule-of-thirds grid inside
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val scrim = Color.Black.copy(alpha = 0.52f)
                        // Top, Bottom, Left, Right scrim strips around crop box
                        drawRect(scrim, topLeft = Offset(imgLeft, imgTop), size = Size(imgW, (boxY - imgTop).coerceAtLeast(0f)))
                        drawRect(scrim, topLeft = Offset(imgLeft, boxY + boxH), size = Size(imgW, (imgTop + imgH - (boxY + boxH)).coerceAtLeast(0f)))
                        drawRect(scrim, topLeft = Offset(imgLeft, boxY), size = Size((boxX - imgLeft).coerceAtLeast(0f), boxH))
                        drawRect(scrim, topLeft = Offset(boxX + boxW, boxY), size = Size((imgLeft + imgW - (boxX + boxW)).coerceAtLeast(0f), boxH))

                        // Rule-of-thirds lines
                        val gridColor = Color.White.copy(alpha = 0.45f)
                        for (i in 1..2) {
                            val gx = boxX + boxW * (i / 3f)
                            drawLine(gridColor, Offset(gx, boxY), Offset(gx, boxY + boxH), strokeWidth = 1f)
                            val gy = boxY + boxH * (i / 3f)
                            drawLine(gridColor, Offset(boxX, gy), Offset(boxX + boxW, gy), strokeWidth = 1f)
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Draggable Crop Box (pan inside)
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(boxX.roundToInt(), boxY.roundToInt()) }
                                .size(width = boxWDp, height = boxHDp)
                                .border(2.dp, Color.White, RoundedCornerShape(2.dp))
                                .pointerInput(imgW, imgH) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val dx = dragAmount.x / imgW
                                        val dy = dragAmount.y / imgH
                                        val wSpan = cropRight - cropLeft
                                        val hSpan = cropBottom - cropTop
                                        val newLeft = (cropLeft + dx).coerceIn(0f, 1f - wSpan)
                                        val newTop = (cropTop + dy).coerceIn(0f, 1f - hSpan)
                                        cropLeft = newLeft
                                        cropTop = newTop
                                        cropRight = newLeft + wSpan
                                        cropBottom = newTop + hSpan
                                    }
                                }
                        )

                        // 4 Corner Resize Handles
                        val handleSize = 26.dp
                        val handleHalfPx = with(localDensity) { (handleSize / 2).toPx() }

                        // Helper to render each corner handle
                        @Composable
                        fun CropCornerHandle(
                            cx: Float,
                            cy: Float,
                            onDrag: (dxFrac: Float, dyFrac: Float) -> Unit
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            (cx - handleHalfPx).roundToInt(),
                                            (cy - handleHalfPx).roundToInt()
                                        )
                                    }
                                    .size(handleSize)
                                    .pointerInput(imgW, imgH, lockedRatio) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            onDrag(dragAmount.x / imgW, dragAmount.y / imgH)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }
                        }

                        // Top-Left
                        CropCornerHandle(cx = boxX, cy = boxY) { dx, dy ->
                            val newL = (cropLeft + dx).coerceIn(0f, cropRight - minSpan)
                            val newT = (cropTop + dy).coerceIn(0f, cropBottom - minSpan)
                            cropLeft = newL
                            if (lockedRatio != null) {
                                val targetH = ((cropRight - newL) * imgW / lockedRatio) / imgH
                                cropTop = (cropBottom - targetH).coerceIn(0f, cropBottom - minSpan)
                            } else {
                                cropTop = newT
                            }
                        }
                        // Top-Right
                        CropCornerHandle(cx = boxX + boxW, cy = boxY) { dx, dy ->
                            val newR = (cropRight + dx).coerceIn(cropLeft + minSpan, 1f)
                            val newT = (cropTop + dy).coerceIn(0f, cropBottom - minSpan)
                            cropRight = newR
                            if (lockedRatio != null) {
                                val targetH = ((newR - cropLeft) * imgW / lockedRatio) / imgH
                                cropTop = (cropBottom - targetH).coerceIn(0f, cropBottom - minSpan)
                            } else {
                                cropTop = newT
                            }
                        }
                        // Bottom-Left
                        CropCornerHandle(cx = boxX, cy = boxY + boxH) { dx, dy ->
                            val newL = (cropLeft + dx).coerceIn(0f, cropRight - minSpan)
                            val newB = (cropBottom + dy).coerceIn(cropTop + minSpan, 1f)
                            cropLeft = newL
                            if (lockedRatio != null) {
                                val targetH = ((cropRight - newL) * imgW / lockedRatio) / imgH
                                cropBottom = (cropTop + targetH).coerceIn(cropTop + minSpan, 1f)
                            } else {
                                cropBottom = newB
                            }
                        }
                        // Bottom-Right
                        CropCornerHandle(cx = boxX + boxW, cy = boxY + boxH) { dx, dy ->
                            val newR = (cropRight + dx).coerceIn(cropLeft + minSpan, 1f)
                            val newB = (cropBottom + dy).coerceIn(cropTop + minSpan, 1f)
                            cropRight = newR
                            if (lockedRatio != null) {
                                val targetH = ((newR - cropLeft) * imgW / lockedRatio) / imgH
                                cropBottom = (cropTop + targetH).coerceIn(cropTop + minSpan, 1f)
                            } else {
                                cropBottom = newB
                            }
                        }
                    }
                }
            } ?: CircularProgressIndicator(color = Color.White)
        }

        // Sub-tool Controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141518))
                .padding(16.dp)
        ) {
            when (uiState.currentTool) {
                EditorTool.CROP_ROTATE -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Aspect Ratio Chips
                        LazyRow(
                            contentPadding = PaddingValues(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(CropAspectRatio.values()) { ratio ->
                                FilterChip(
                                    selected = uiState.selectedCropRatio == ratio,
                                    onClick = {
                                        viewModel.setCropAspectRatio(ratio)
                                        ratio.ratio?.let { r ->
                                            if (r >= 1f) {
                                                cropLeft = 0.1f
                                                cropRight = 0.9f
                                                cropTop = 0.2f
                                                cropBottom = (cropTop + (cropRight - cropLeft) / r).coerceAtMost(0.95f)
                                            } else {
                                                cropTop = 0.1f
                                                cropBottom = 0.9f
                                                cropLeft = 0.2f
                                                cropRight = (cropLeft + (cropBottom - cropTop) * r).coerceAtMost(0.95f)
                                            }
                                        } ?: run {
                                            cropLeft = 0.05f
                                            cropTop = 0.05f
                                            cropRight = 0.95f
                                            cropBottom = 0.95f
                                        }
                                    },
                                    label = { Text(ratio.label) },
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row {
                                IconButton(onClick = { viewModel.rotate90() }) {
                                    Icon(Icons.Default.RotateRight, contentDescription = "Rotate 90", tint = Color.White)
                                }
                                IconButton(onClick = { viewModel.flipHorizontal() }) {
                                    Icon(Icons.Default.Flip, contentDescription = "Flip Horizontal", tint = Color.White)
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.applyCrop(cropLeft, cropTop, cropRight, cropBottom)
                                    // Reset crop box after applying
                                    cropLeft = 0.05f
                                    cropTop = 0.05f
                                    cropRight = 0.95f
                                    cropBottom = 0.95f
                                },
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Apply Crop")
                            }
                        }
                    }
                }
                EditorTool.ADJUST -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Brightness", color = Color.White, fontSize = 12.sp)
                        Slider(
                            value = uiState.currentState.brightness,
                            onValueChange = { viewModel.setBrightness(it) },
                            valueRange = -80f..80f
                        )
                        Text("Contrast", color = Color.White, fontSize = 12.sp)
                        Slider(
                            value = uiState.currentState.contrast,
                            onValueChange = { viewModel.setContrast(it) },
                            valueRange = 0.5f..1.5f
                        )
                        Text("Saturation", color = Color.White, fontSize = 12.sp)
                        Slider(
                            value = uiState.currentState.saturation,
                            onValueChange = { viewModel.setSaturation(it) },
                            valueRange = 0f..2f
                        )
                    }
                }
                EditorTool.FILTERS -> {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(FilterPreset.values()) { preset ->
                            FilterChip(
                                selected = uiState.currentState.activeFilter == preset,
                                onClick = { viewModel.setFilter(preset) },
                                label = { Text(preset.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }
                EditorTool.DRAW -> {
                    Text("Drawing tool active", color = Color.White)
                }
            }
        }

        // Main Bottom Tool Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ToolTab(
                icon = Icons.Default.CropRotate,
                label = "Crop & Rotate",
                isSelected = uiState.currentTool == EditorTool.CROP_ROTATE,
                onClick = { viewModel.selectTool(EditorTool.CROP_ROTATE) }
            )
            ToolTab(
                icon = Icons.Default.Tune,
                label = "Adjust",
                isSelected = uiState.currentTool == EditorTool.ADJUST,
                onClick = { viewModel.selectTool(EditorTool.ADJUST) }
            )
            ToolTab(
                icon = Icons.Default.AutoFixHigh,
                label = "Filters",
                isSelected = uiState.currentTool == EditorTool.FILTERS,
                onClick = { viewModel.selectTool(EditorTool.FILTERS) }
            )
        }
    }
}

@Composable
private fun ToolTab(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
        )
    }
}
