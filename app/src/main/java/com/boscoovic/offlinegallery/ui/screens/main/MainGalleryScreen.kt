package com.boscoovic.offlinegallery.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.boscoovic.offlinegallery.R
import com.boscoovic.offlinegallery.data.model.Album
import com.boscoovic.offlinegallery.data.model.MediaFilter
import com.boscoovic.offlinegallery.data.model.MediaItem
import com.boscoovic.offlinegallery.ui.components.MediaThumbnail
import com.boscoovic.offlinegallery.ui.components.SelectionActionBar
import com.boscoovic.offlinegallery.ui.components.StickyDateHeader
import com.boscoovic.offlinegallery.ui.screens.albums.AlbumsUiState
import com.boscoovic.offlinegallery.ui.screens.albums.AlbumsViewModel
import com.boscoovic.offlinegallery.ui.screens.pictures.PicturesUiState
import com.boscoovic.offlinegallery.ui.screens.pictures.PicturesViewModel
import com.boscoovic.offlinegallery.ui.theme.FavoriteRed
import kotlinx.coroutines.launch

@Composable
fun MainGalleryScreen(
    picturesViewModel: PicturesViewModel,
    albumsViewModel: AlbumsViewModel,
    onMediaClick: (MediaItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onFavoritesClick: () -> Unit,
    onSecureFolderClick: () -> Unit,
    onRecycleBinClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val picturesState by picturesViewModel.uiState.collectAsState()
    val albumsState by albumsViewModel.uiState.collectAsState()

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    val picturesGridState = rememberLazyGridState()
    val albumsGridState = rememberLazyGridState()

    // Determine if scrolled on the current active tab
    val isScrolled by remember {
        derivedStateOf {
            if (pagerState.currentPage == 0) {
                picturesGridState.firstVisibleItemIndex > 0 || picturesGridState.firstVisibleItemScrollOffset > 30
            } else {
                albumsGridState.firstVisibleItemIndex > 0 || albumsGridState.firstVisibleItemScrollOffset > 30
            }
        }
    }

    // Samsung One UI Reachability Viewing Area Height:
    // Generously expanded at rest (~200dp) so content is easily within thumb reach.
    // Shrinks down to 0dp as the user scrolls, and expands back when scrolled to the top!
    val viewingAreaHeight by animateDpAsState(
        targetValue = if (isScrolled) 0.dp else 200.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "viewingAreaHeight"
    )

    var showMoreMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header Row with Tabs: [ Pictures(367) ] [ Albums(12) ] and Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tabs on the Same Row: Pictures(count) and Albums(count)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pictures Tab
                val isPicturesSelected = pagerState.currentPage == 0
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pictures",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = if (isPicturesSelected) 22.sp else 18.sp
                        ),
                        fontWeight = if (isPicturesSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isPicturesSelected) MaterialTheme.colorScheme.onBackground
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "(${picturesState.totalCount})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = if (isPicturesSelected) 16.sp else 14.sp
                        ),
                        fontWeight = if (isPicturesSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isPicturesSelected) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // Albums Tab
                val isAlbumsSelected = pagerState.currentPage == 1
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Albums",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = if (isAlbumsSelected) 22.sp else 18.sp
                        ),
                        fontWeight = if (isAlbumsSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isAlbumsSelected) MaterialTheme.colorScheme.onBackground
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "(${albumsState.albums.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = if (isAlbumsSelected) 16.sp else 14.sp
                        ),
                        fontWeight = if (isAlbumsSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isAlbumsSelected) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            // Right Actions: Search, Add (for albums), More options
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (pagerState.currentPage == 1) {
                    IconButton(onClick = { albumsViewModel.openCreateDialog() }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.create_album),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Favorites") },
                            leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = FavoriteRed) },
                            onClick = {
                                showMoreMenu = false
                                onFavoritesClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Recycle Bin") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                onRecycleBinClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Secure Folder") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                onSecureFolderClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                showMoreMenu = false
                                onSettingsClick()
                            }
                        )
                    }
                }
            }
        }

        // Samsung One UI Reachability Viewing Area (Expands when at top, shrinks when scrolling down)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(viewingAreaHeight)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (viewingAreaHeight > 50.dp) {
                Column {
                    if (pagerState.currentPage == 0) {
                        Text(
                            text = stringResource(R.string.pictures),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 38.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.items_count, picturesState.totalCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.albums),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 38.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${albumsState.albums.size} albums",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Pager holding Pictures content (Page 0) and Albums content (Page 1)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            if (page == 0) {
                PicturesTabContent(
                    viewModel = picturesViewModel,
                    uiState = picturesState,
                    gridState = picturesGridState,
                    onMediaClick = onMediaClick
                )
            } else {
                AlbumsTabContent(
                    viewModel = albumsViewModel,
                    uiState = albumsState,
                    gridState = albumsGridState,
                    onAlbumClick = onAlbumClick,
                    onFavoritesClick = onFavoritesClick,
                    onSecureFolderClick = onSecureFolderClick,
                    onRecycleBinClick = onRecycleBinClick
                )
            }
        }
    }
}

@Composable
private fun PicturesTabContent(
    viewModel: PicturesViewModel,
    uiState: PicturesUiState,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onMediaClick: (MediaItem) -> Unit
) {
    val allItems = remember(uiState.dateGroups) {
        uiState.dateGroups.flatMap { it.items }
    }
    var scaleFactor by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scaleFactor *= zoom
                    if (scaleFactor > 1.35f) {
                        viewModel.setColumnCount(uiState.columnCount - 1)
                        scaleFactor = 1f
                    } else if (scaleFactor < 0.75f) {
                        viewModel.setColumnCount(uiState.columnCount + 1)
                        scaleFactor = 1f
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    state = gridState,
                    columns = GridCells.Fixed(uiState.columnCount),
                    contentPadding = PaddingValues(bottom = 24.dp),
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
                onMove = { /* Move */ },
                onHide = { viewModel.hideSelected(allItems) },
                onDelete = { viewModel.deleteSelected(allItems) }
            )
        }
    }
}

@Composable
private fun AlbumsTabContent(
    viewModel: AlbumsViewModel,
    uiState: AlbumsUiState,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onAlbumClick: (Album) -> Unit,
    onFavoritesClick: () -> Unit,
    onSecureFolderClick: () -> Unit,
    onRecycleBinClick: () -> Unit
) {
    var newAlbumName by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Shortcuts Section (Favorites, Secure Folder, Recycle Bin)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ShortcutCard(
                            icon = Icons.Default.Favorite,
                            iconTint = FavoriteRed,
                            title = stringResource(R.string.favorites),
                            count = uiState.favoritesCount,
                            onClick = onFavoritesClick,
                            modifier = Modifier.weight(1f)
                        )
                        ShortcutCard(
                            icon = Icons.Default.Lock,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = stringResource(R.string.secure_folder),
                            count = uiState.secureCount,
                            onClick = onSecureFolderClick,
                            modifier = Modifier.weight(1f)
                        )
                        ShortcutCard(
                            icon = Icons.Default.Delete,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            title = stringResource(R.string.recycle_bin),
                            count = uiState.trashCount,
                            onClick = onRecycleBinClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Album items
                items(uiState.albums, key = { "${it.isCustom}_${it.id}" }) { album ->
                    AlbumCard(
                        album = album,
                        onClick = { onAlbumClick(album) }
                    )
                }
            }
        }

        // Create Album Dialog
        if (uiState.showCreateDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.closeCreateDialog() },
                title = { Text(stringResource(R.string.create_album)) },
                text = {
                    OutlinedTextField(
                        value = newAlbumName,
                        onValueChange = { newAlbumName = it },
                        label = { Text(stringResource(R.string.album_name)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.createCustomAlbum(newAlbumName)
                            newAlbumName = ""
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeCreateDialog() }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun ShortcutCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (album.coverUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(album.coverUri)
                        .crossfade(true)
                        .size(400)
                        .build(),
                    contentDescription = album.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = "${album.itemCount} items",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
