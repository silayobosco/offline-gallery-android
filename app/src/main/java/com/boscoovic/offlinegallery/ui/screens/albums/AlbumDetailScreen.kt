package com.boscoovic.offlinegallery.ui.screens.albums

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.boscoovic.offlinegallery.data.model.Album
import com.boscoovic.offlinegallery.data.model.MediaItem
import com.boscoovic.offlinegallery.data.model.MediaType
import com.boscoovic.offlinegallery.ui.components.MediaThumbnail
import com.boscoovic.offlinegallery.ui.components.SelectionActionBar
import kotlin.math.pow
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    viewModel: AlbumDetailViewModel,
    onBack: () -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onSearchClick: () -> Unit,
    onSlideshowClick: (contextType: String, contextArg: String) -> Unit = { _, _ -> },
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
    // Generously expanded at rest (260.dp) so content is easily within thumb reach, matching MainGalleryScreen.
    // Shrinks down to 0dp when scrolled down!
    val viewingAreaHeight by animateDpAsState(
        targetValue = if (isScrolled) 0.dp else 260.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "albumViewingAreaHeight"
    )
    val heroAlpha = ((viewingAreaHeight.value - 50f) / 210f).coerceIn(0f, 1f)

    // Menu and Dialog states
    var showMenu by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var renameAlbumName by remember { mutableStateOf("") }
    var gridColumns by remember { mutableIntStateOf(if (uiState.isSidebarVisible) 3 else 4) }

    // Scroll to top whenever album changes
    LaunchedEffect(uiState.albumId) {
        gridState.scrollToItem(0)
    }

    // Telegram-style pull-up to switch folder
    var overscrollPullY by remember { mutableFloatStateOf(0f) }
    val pullThreshold = 130f
    val isReadyToSwitch = overscrollPullY >= pullThreshold
    val pullProgress = (overscrollPullY / pullThreshold).coerceIn(0f, 1f)

    val nestedScrollConnection = remember(uiState.albumId, uiState.allAlbums) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (overscrollPullY > 0f && available.y > 0f) {
                    val consumed = available.y.coerceAtMost(overscrollPullY)
                    overscrollPullY -= consumed
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val next = viewModel.getNextAlbum()
                if (next != null && !gridState.canScrollForward && available.y < 0f && source == NestedScrollSource.UserInput) {
                    overscrollPullY += -available.y * 0.65f
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (overscrollPullY >= pullThreshold) {
                    val next = viewModel.switchToNextAlbum()
                    if (next != null) {
                        Toast.makeText(context, "Switched to ${next.name}", Toast.LENGTH_SHORT).show()
                    }
                }
                overscrollPullY = 0f
                return Velocity.Zero
            }
        }
    }

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
            // Samsung One UI Reachability Viewing Area (Expands to 260dp at top, shrinks when scrolling down)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(viewingAreaHeight)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (viewingAreaHeight > 36.dp) {
                    Column(
                        modifier = Modifier.graphicsLayer {
                            alpha = heroAlpha
                            translationY = (1f - heroAlpha) * -20f
                        }
                    ) {
                        Text(
                            text = uiState.albumName,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 36.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = uiState.subtitle.ifEmpty { "${uiState.items.size} items" },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Samsung Gallery Top Bar (Sits directly above the media area, moving up to the top when scrolled)
            TopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = isScrolled,
                        enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                        exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.albumName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    lineHeight = 22.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = "${uiState.items.size}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    lineHeight = 22.sp
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(start = 6.dp)
                            )
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
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            // 1. Start Slideshow
                            DropdownMenuItem(
                                text = { Text("Start Slideshow") },
                                leadingIcon = { Icon(Icons.Default.PlayCircle, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onSlideshowClick(if (uiState.isCustom) "CUSTOM_ALBUM" else "ALBUM", uiState.albumId.toString())
                                }
                            )

                            // 2. Select Items / Select All
                            DropdownMenuItem(
                                text = { Text(if (uiState.isSelectionMode) "Select all" else "Select items") },
                                leadingIcon = { Icon(Icons.Default.CheckCircleOutline, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    if (uiState.isSelectionMode) {
                                        viewModel.selectAll()
                                    } else {
                                        val first = uiState.items.firstOrNull()
                                        if (first != null) {
                                            viewModel.toggleItemSelection(first.id)
                                        }
                                    }
                                }
                            )

                            // 3. Grid Columns (Zoom in / Zoom out)
                            DropdownMenuItem(
                                text = { Text("Grid columns: $gridColumns") },
                                leadingIcon = { Icon(Icons.Default.GridOn, contentDescription = null) },
                                onClick = {
                                    gridColumns = if (gridColumns >= 5) 2 else gridColumns + 1
                                }
                            )

                            // 4. Next Folder shortcut
                            val nextAlbum = viewModel.getNextAlbum()
                            if (nextAlbum != null) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("Next folder")
                                            Text(
                                                text = nextAlbum.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        val next = viewModel.switchToNextAlbum()
                                        if (next != null) {
                                            Toast.makeText(context, "Switched to ${next.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }

                            // 5. Folder Details
                            DropdownMenuItem(
                                text = { Text("Details") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showDetailsDialog = true
                                }
                            )

                            if (uiState.isCustom) {
                                HorizontalDivider()

                                // 6. Rename Album (custom only)
                                DropdownMenuItem(
                                    text = { Text("Rename album") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        renameAlbumName = uiState.albumName
                                        showRenameDialog = true
                                    }
                                )

                                // 7. Delete Album (custom only)
                                DropdownMenuItem(
                                    text = { Text("Delete album", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

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
                        val columns = if (uiState.isSidebarVisible) (gridColumns - 1).coerceAtLeast(2) else gridColumns
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
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(nestedScrollConnection)
                        ) {
                            items(uiState.items, key = { it.id }) { item ->
                                var itemPosInRoot by remember { mutableStateOf(Offset.Zero) }

                                Box(
                                    modifier = Modifier
                                        .onGloballyPositioned { coords ->
                                            itemPosInRoot = coords.positionInRoot()
                                        }
                                        .then(
                                            if (uiState.isSelectionMode) {
                                                Modifier.pointerInput(uiState.isSelectionMode, uiState.selectedItemIds) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = { localOffset ->
                                                            // When selection is on, drag all selected items (or this item if touched)
                                                            if (!uiState.selectedItemIds.contains(item.id)) {
                                                                viewModel.toggleItemSelection(item.id)
                                                            }
                                                            val itemsToMove = uiState.items.filter {
                                                                uiState.selectedItemIds.contains(it.id) || it.id == item.id
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
                                                            val hovered = albumBounds.entries.firstOrNull { entry ->
                                                                val rect = entry.value
                                                                // Generous hit detection over the sidebar area on the left
                                                                dragPosition.x <= (rect.right + 60f) &&
                                                                dragPosition.y >= (rect.top - 20f) &&
                                                                dragPosition.y <= (rect.bottom + 20f)
                                                            }?.key
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
                                            } else {
                                                Modifier
                                            }
                                        )
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
                                        onLongClick = if (uiState.isSelectionMode) null else {
                                            { viewModel.toggleItemSelection(item.id) }
                                        }
                                    )
                                }
                            }

                            // Persistent Telegram-style Switch to Next Folder Card
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                val nextAlbum = remember(uiState.albumId, uiState.allAlbums) {
                                    viewModel.getNextAlbum()
                                }
                                if (nextAlbum != null) {
                                    NextFolderTelegramSwitchCard(
                                        currentAlbumName = uiState.albumName,
                                        nextAlbum = nextAlbum,
                                        pullProgress = pullProgress,
                                        isReadyToSwitch = isReadyToSwitch,
                                        onClick = {
                                            val next = viewModel.switchToNextAlbum()
                                            if (next != null) {
                                                Toast.makeText(context, "Switched to ${next.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${uiState.items.size} items",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
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

        // 1. Details Dialog
        if (showDetailsDialog) {
            val totalSizeBytes = remember(uiState.items) {
                uiState.items.sumOf { it.size }
            }
            val imgCount = remember(uiState.items) { uiState.items.count { it.mediaType == MediaType.IMAGE } }
            val vidCount = remember(uiState.items) { uiState.items.count { it.mediaType == MediaType.VIDEO } }
            val folderPath = remember(uiState.items) {
                uiState.items.firstOrNull()?.path?.substringBeforeLast('/') ?: ""
            }

            AlertDialog(
                onDismissRequest = { showDetailsDialog = false },
                icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text(uiState.albumName, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AlbumDetailInfoRow(label = "Type", value = if (uiState.isCustom) "Custom Album" else "Device Folder")
                        AlbumDetailInfoRow(label = "Total items", value = "${uiState.items.size}")
                        AlbumDetailInfoRow(label = "Breakdown", value = "$imgCount photos, $vidCount videos")
                        AlbumDetailInfoRow(label = "Total size", value = formatAlbumSize(totalSizeBytes))
                        if (folderPath.isNotEmpty()) {
                            AlbumDetailInfoRow(label = "Location", value = folderPath)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDetailsDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // 2. Rename Custom Album Dialog
        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename Album") },
                text = {
                    OutlinedTextField(
                        value = renameAlbumName,
                        onValueChange = { renameAlbumName = it },
                        label = { Text("Album Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (renameAlbumName.isNotBlank()) {
                                viewModel.renameCustomAlbum(renameAlbumName)
                                showRenameDialog = false
                            }
                        }
                    ) {
                        Text("Rename")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 3. Delete Custom Album Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Album?") },
                text = { Text("Are you sure you want to delete this custom album? Media files will not be deleted from your device.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteCurrentAlbum {
                                onBack()
                            }
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
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

@Composable
private fun NextFolderTelegramSwitchCard(
    currentAlbumName: String,
    nextAlbum: Album,
    pullProgress: Float,
    isReadyToSwitch: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (isReadyToSwitch) 180f else (pullProgress * 180f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "arrowRotation"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isReadyToSwitch) {
            MaterialTheme.colorScheme.primaryContainer
        } else if (pullProgress > 0.05f) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f + pullProgress * 0.3f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        label = "containerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isReadyToSwitch) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "contentColor"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated Up Arrow
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (isReadyToSwitch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isReadyToSwitch) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer(rotationZ = arrowRotation)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isReadyToSwitch) {
                    "Release to switch"
                } else if (pullProgress > 0.1f) {
                    "Pull up to switch folder"
                } else {
                    "Next Folder"
                },
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.75f),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = nextAlbum.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${nextAlbum.itemCount} items  •  Swipe up or tap to jump",
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun AlbumDetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatAlbumSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024.0 && unitIndex < units.size - 1) {
        size /= 1024.0
        unitIndex++
    }
    return String.format(java.util.Locale.US, "%.1f %s", size, units[unitIndex])
}
