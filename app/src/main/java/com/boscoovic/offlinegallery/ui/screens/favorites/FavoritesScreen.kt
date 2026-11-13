package com.boscoovic.offlinegallery.ui.screens.favorites

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boscoovic.offlinegallery.R
import com.boscoovic.offlinegallery.data.model.MediaItem
import com.boscoovic.offlinegallery.ui.components.FastScrollScrubber
import com.boscoovic.offlinegallery.ui.components.MediaThumbnail
import com.boscoovic.offlinegallery.ui.components.MoveToAlbumDialog
import com.boscoovic.offlinegallery.ui.components.OneUiCollapsibleHeader
import com.boscoovic.offlinegallery.ui.components.SelectionActionBar
import com.boscoovic.offlinegallery.ui.components.dragSelectGrid
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onBack: () -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onSlideshowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val gridState = rememberLazyGridState()

    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }
    var showMoveDialog by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    val isScrolled by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 20
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OneUiCollapsibleHeader(
                title = stringResource(R.string.favorites),
                subtitle = "${uiState.items.size} favorite(s)",
                isScrolled = isScrolled,
                onBack = onBack,
                actions = {
                    if (uiState.items.isNotEmpty()) {
                        IconButton(onClick = onSlideshowClick) {
                            Icon(Icons.Default.PlayCircle, contentDescription = "Slideshow")
                        }
                        IconButton(onClick = {
                            if (uiState.isSelectionMode) viewModel.clearSelection() else viewModel.selectAll()
                        }) {
                            Icon(Icons.Default.CheckCircleOutline, contentDescription = "Select")
                        }
                    }
                }
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No favorites yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val orderedIds = remember(uiState.items) { uiState.items.map { it.id } }
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(start = 2.dp, end = 2.dp, top = 2.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .dragSelectGrid(
                                gridState = gridState,
                                orderedIds = orderedIds,
                                selectedIds = uiState.selectedItemIds,
                                onSelectionChanged = { viewModel.setSelection(it) }
                            )
                    ) {
                        items(uiState.items, key = { it.id }) { item ->
                            MediaThumbnail(
                                item = item,
                                isSelectionMode = uiState.isSelectionMode,
                                isSelected = uiState.selectedItemIds.contains(item.id),
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleItemSelection(item.id)
                                    } else {
                                        onMediaClick(item)
                                    }
                                },
                                onLongClick = null
                            )
                        }
                    }

                    FastScrollScrubber(
                        gridState = gridState,
                        totalItemsCount = uiState.items.size,
                        dateLabelProvider = { index ->
                            val item = uiState.items.getOrNull(index.coerceIn(0, (uiState.items.size - 1).coerceAtLeast(0)))
                            if (item != null) {
                                val timestamp = if (item.dateTaken > 0) item.dateTaken else item.dateAdded * 1000
                                if (timestamp > 0L) dateFormatter.format(Date(timestamp)) else ""
                            } else ""
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.isSelectionMode,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SelectionActionBar(
                selectedCount = uiState.selectedItemIds.size,
                onClose = { viewModel.clearSelection() },
                onSelectAll = { viewModel.selectAll() },
                onShare = { viewModel.shareSelected() },
                onFavorite = {
                    viewModel.unfavoriteSelected { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                onMove = { showMoveDialog = true },
                onHide = {
                    viewModel.hideSelected { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                onDelete = { viewModel.deleteSelected() }
            )
        }

        if (showMoveDialog) {
            MoveToAlbumDialog(
                albums = uiState.allAlbums,
                selectedCount = uiState.selectedItemIds.size,
                onAlbumSelected = { targetAlbum ->
                    showMoveDialog = false
                    viewModel.moveSelectedToAlbum(targetAlbum) { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { showMoveDialog = false }
            )
        }
    }
}

