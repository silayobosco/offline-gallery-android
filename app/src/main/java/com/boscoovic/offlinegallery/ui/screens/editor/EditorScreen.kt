package com.boscoovic.offlinegallery.ui.screens.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boscoovic.offlinegallery.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
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
                Button(
                    onClick = { viewModel.saveAsCopy { onSaved() } },
                    enabled = !uiState.isSaving,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text(stringResource(R.string.save_copy))
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
        )

        // Center Preview Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            uiState.previewBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Edited Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = { viewModel.rotate90() }) {
                            Icon(Icons.Default.RotateRight, contentDescription = "Rotate 90", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.flipHorizontal() }) {
                            Icon(Icons.Default.Flip, contentDescription = "Flip Horizontal", tint = Color.White)
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
                                label = { Text(preset.name.lowercase().capitalize()) },
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
                label = "Transform",
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

