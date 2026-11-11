package com.boscoovic.offlinegallery.ui.screens.albums

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.boscoovic.offlinegallery.data.model.Album
import com.boscoovic.offlinegallery.data.model.MediaItem
import com.boscoovic.offlinegallery.ui.components.MediaThumbnail
import com.boscoovic.offlinegallery.ui.components.SelectionActionBar
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    viewModel: AlbumDetailViewModel,
    onBack: () -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val gridState = rememberLazyGridState()

    // Determine whether user has scrolled down into the media grid
    val isScrolled by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 20
        }
    }

    // Samsung One UI Reachability Viewing Area Height:
    // Expands to 160dp at rest (when top images are reached) so content is easily within thumb reach.
    // Shrinks down to 0dp when scrolled down!
    val viewingAreaHeight by animateDpAsState(
        targetValue = if (isScrolled) 0.dp else 160.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "albumViewingAreaHeight"
    )

    // Drag & Drop State
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var draggedItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var hoveredAlbumId by remember { mutableStateOf<Long?>(null) }
    val albumBounds = remember { mutableStateMapOf<Long, Rect>() }
    var rootPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .onGloballyPositioned { rootPosition = it.positionInRoot() }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Samsung Gallery Top Bar
            TopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = isScrolled,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column {
                            Text(
                                text = uiState.albumName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (uiState.subtitle.isNotEmpty()) {
                                Text(
                                    text = uiState.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSidebar() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ViewSidebar,
                            contentDescription = "Toggle Folder Sidebar",
                            tint = if (uiState.isSidebarVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { /* More options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Samsung One UI Reachability Viewing Area (Expands when at top, shrinks when scrolling down)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(viewingAreaHeight)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                if (viewingAreaHeight > 40.dp) {
                    Column {
                        Text(
                            text = uiState.albumName,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.subtitle.ifEmpty { "${uiState.items.size} items" },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Main Content Area with Optional Folder Sidebar on the Left
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(uiState.isSidebarVisible) {
                        // Edge swipe to bring sidebar back when hidden
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (!uiState.isSidebarVisible && dragAmount > 25f) {
                                viewModel.toggleSidebar(true)
                            }
                        }
                    }
            ) {
                // Samsung Folder Sidebar (Togglable on the Left)
                AnimatedVisibility(
                    visible = uiState.isSidebarVisible,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .width(88.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .pointerInput(Unit) {
                                // Swipe left on sidebar to hide it
                                detectHorizontalDragGestures { _, dragAmount ->
                                    if (dragAmount < -20f) {
                                        viewModel.toggleSidebar(false)
                                    }
                                }
                            }
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.allAlbums, key = { "${it.isCustom}_${it.id}" }) { album ->
                                val isSelected = album.id == uiState.albumId
                                val isHovered = album.id == hoveredAlbumId
                                FolderSidebarItem(
                                    album = album,
                                    isSelected = isSelected,
                                    isHovered = isHovered,
                                    onPositioned = { rect -> albumBounds[album.id] = rect },
                                    onClick = { viewModel.selectAlbum(album) }
                                )
                            }
                        }
                    }
                }

                // Media Grid on the Right
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (uiState.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.items.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No items in this album",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val columns = if (uiState.isSidebarVisible) 3 else 4
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(columns),
                            contentPadding = PaddingValues(
                                start = 2.dp,
                                end = 2.dp,
                                top = 2.dp,
                                bottom = 80.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.items, key = { it.id }) { item ->
                                var itemPosInRoot by remember { mutableStateOf(Offset.Zero) }

                                Box(
                                    modifier = Modifier
                                        .onGloballyPositioned { coords ->
                                            itemPosInRoot = coords.positionInRoot()
                                        }
                                        .pointerInput(uiState.selectedItemIds, uiState.items) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { localOffset ->
                                                    val itemsToMove = if (uiState.selectedItemIds.contains(item.id)) {
                                                        uiState.items.filter { uiState.selectedItemIds.contains(it.id) }
                                                    } else {
                                                        if (!uiState.isSelectionMode) {
                                                            viewModel.toggleItemSelection(item.id)
                                                        }
                                                        listOf(item)
                                                    }
                                                    draggedItems = itemsToMove
                                                    dragPosition = itemPosInRoot + localOffset
                                                    isDragging = true
                                                    // Reveal sidebar if it was collapsed
                                                    if (!uiState.isSidebarVisible) {
                                                        viewModel.toggleSidebar(true)
                                                    }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragPosition += dragAmount
                                                    val hovered = albumBounds.entries.firstOrNull { it.value.contains(dragPosition) }?.key
                                                    hoveredAlbumId = if (hovered != null && hovered != uiState.albumId) hovered else null
                                                },
                                                onDragEnd = {
                                                    if (hoveredAlbumId != null) {
                                                        val target = uiState.allAlbums.firstOrNull { it.id == hoveredAlbumId }
                                                        if (target != null && target.id != uiState.albumId) {
                                                            viewModel.moveOrAddItemsToAlbum(target, draggedItems) { msg ->
                                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                    isDragging = false
                                                    draggedItems = emptyList()
                                                    hoveredAlbumId = null
                                                },
                                                onDragCancel = {
                                                    isDragging = false
                                                    draggedItems = emptyList()
                                                    hoveredAlbumId = null
                                                }
                                            )
                                        }
                                ) {
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
                                        onLongClick = {
                                            viewModel.toggleItemSelection(item.id)
                                        }
                                    )
                                }
                            }
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
                onSelectAll = { viewModel.selectAll() },
                onShare = { /* Share */ },
                onFavorite = { /* Favorite */ },
                onMove = { /* Move */ },
                onHide = { /* Hide */ },
                onDelete = { viewModel.deleteSelected() }
            )
        }

        // Floating Drag Preview Ghost
        if (isDragging && draggedItems.isNotEmpty()) {
            val previewItem = draggedItems.first()
            val density = LocalDensity.current
            val ghostSizePx = with(density) { 72.dp.toPx() }
            val localX = (dragPosition.x - rootPosition.x - ghostSizePx / 2f).roundToInt()
            val localY = (dragPosition.y - rootPosition.y - ghostSizePx / 2f).roundToInt()

            Box(
                modifier = Modifier
                    .offset { IntOffset(localX, localY) }
                    .size(72.dp)
                    .shadow(16.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.5.dp, Color.White, RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(previewItem.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (draggedItems.size > 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${draggedItems.size}",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderSidebarItem(
    album: Album,
    isSelected: Boolean,
    isHovered: Boolean,
    onPositioned: (Rect) -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.10f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "sidebarItemScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onGloballyPositioned { coords ->
                onPositioned(coords.boundsInRoot())
            }
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(
                if (isHovered) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else Color.Transparent
            )
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (isHovered) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    else if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (album.coverUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(album.coverUri)
                        .crossfade(true)
                        .size(160)
                        .build(),
                    contentDescription = album.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = if (isHovered) "Drop here" else album.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isHovered || isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isHovered || isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${album.itemCount}",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
