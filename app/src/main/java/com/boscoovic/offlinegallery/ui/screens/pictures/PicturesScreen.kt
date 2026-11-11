package com.boscoovic.offlinegallery.ui.screens.pictures

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boscoovic.offlinegallery.R
import com.boscoovic.offlinegallery.data.model.MediaFilter
import com.boscoovic.offlinegallery.data.model.MediaItem
import com.boscoovic.offlinegallery.ui.components.MediaThumbnail
import com.boscoovic.offlinegallery.ui.components.SelectionActionBar
import com.boscoovic.offlinegallery.ui.components.StickyDateHeader

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PicturesScreen(
    viewModel: PicturesViewModel,
    onMediaClick: (MediaItem) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val allItems = remember(uiState.dateGroups) {
        uiState.dateGroups.flatMap { it.items }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(uiState.columnCount) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var accumulatedZoom = 1f
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.size >= 2) {
                            val zoom = event.calculateZoom()
                            accumulatedZoom *= zoom
                            if (accumulatedZoom > 1.3f) {
                                viewModel.setColumnCount(uiState.columnCount - 1)
                                accumulatedZoom = 1f
                                event.changes.forEach { it.consume() }
                            } else if (accumulatedZoom < 0.77f) {
                                viewModel.setColumnCount(uiState.columnCount + 1)
                                accumulatedZoom = 1f
                                event.changes.forEach { it.consume() }
                            } else if (kotlin.math.abs(zoom - 1f) > 0.02f) {
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Top Bar with Search action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Samsung One UI Reachability Viewing Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.pictures),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 34.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.items_count, uiState.totalCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaFilter.values().forEach { filter ->
                    val label = when (filter) {
                        MediaFilter.ALL -> stringResource(R.string.all)
                        MediaFilter.PHOTOS -> stringResource(R.string.photos)
                        MediaFilter.VIDEOS -> stringResource(R.string.videos)
                        MediaFilter.FAVORITES -> stringResource(R.string.favorites)
                    }
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(label) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.totalCount == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.no_media),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(uiState.columnCount),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    uiState.dateGroups.forEach { group ->
                        val groupIds = group.items.map { it.id }.toSet()
                        val isDaySelected = groupIds.isNotEmpty() && groupIds.all { uiState.selectedItemIds.contains(it) }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            StickyDateHeader(
                                dateMillis = group.dateMillis,
                                itemCount = group.items.size,
                                isSelectionMode = uiState.isSelectionMode,
                                isAllSelected = isDaySelected,
                                onSelectDayClick = { viewModel.toggleDaySelection(group) }
                            )
                        }

                        items(group.items, key = { it.id }) { item ->
                            val isSelected = uiState.selectedItemIds.contains(item.id)
                            MediaThumbnail(
                                item = item,
                                isSelectionMode = uiState.isSelectionMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleItemSelection(item.id)
                                    } else {
                                        onMediaClick(item)
                                    }
                                },
                                onLongClick = {
                                    viewModel.toggleItemSelection(item.id)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Selection Action Bar floating at bottom
        AnimatedVisibility(
            visible = uiState.isSelectionMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SelectionActionBar(
                selectedCount = uiState.selectedItemIds.size,
                onClose = { viewModel.clearSelection() },
                onSelectAll = { viewModel.selectAll(allItems) },
                onShare = { viewModel.shareSelected(allItems) },
                onFavorite = { viewModel.favoriteSelected(allItems) },
                onMove = { /* Move to album */ },
                onHide = { viewModel.hideSelected(allItems) },
                onDelete = { viewModel.deleteSelected(allItems) }
            )
        }
    }
}
