package com.boscoovic.offlinegallery.ui.screens.main

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.boscoovic.offlinegallery.R
import com.boscoovic.offlinegallery.data.model.Album
import com.boscoovic.offlinegallery.data.model.MediaFilter
import com.boscoovic.offlinegallery.data.model.MediaItem
import com.boscoovic.offlinegallery.data.model.MediaSortOrder
import com.boscoovic.offlinegallery.ui.components.FastScrollScrubber
import com.boscoovic.offlinegallery.ui.components.MediaThumbnail
import com.boscoovic.offlinegallery.ui.components.MoveToAlbumDialog
import com.boscoovic.offlinegallery.ui.components.SelectionActionBar
import com.boscoovic.offlinegallery.ui.components.StickyDateHeader
import com.boscoovic.offlinegallery.ui.components.dragSelectGrid
import com.boscoovic.offlinegallery.ui.screens.albums.AlbumsUiState
import com.boscoovic.offlinegallery.ui.screens.albums.AlbumsViewModel
import com.boscoovic.offlinegallery.ui.screens.pictures.PicturesUiState
import com.boscoovic.offlinegallery.ui.screens.pictures.PicturesViewModel
import com.boscoovic.offlinegallery.ui.theme.FavoriteRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun MainGalleryScreen(
    picturesViewModel: PicturesViewModel,
    albumsViewModel: AlbumsViewModel,
    onMediaClick: (MediaItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onFavoritesClick: () -> Unit,
    onStoriesClick: () -> Unit = {},
    onSecureFolderClick: () -> Unit,
    onRecycleBinClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val picturesState by picturesViewModel.uiState.collectAsState()
    val albumsState by albumsViewModel.uiState.collectAsState()

    BackHandler(enabled = picturesState.isSelectionMode) {
        picturesViewModel.clearSelection()
    }

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
    // Generously expanded at rest (260.dp) so content is easily within thumb reach, consistent across views.
    // Shrinks down to 0dp as the user scrolls, and expands back when scrolled to the top!
    val viewingAreaHeight by animateDpAsState(
        targetValue = if (isScrolled) 0.dp else 260.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "viewingAreaHeight"
    )
    val heroAlpha = ((viewingAreaHeight.value - 50f) / 210f).coerceIn(0f, 1f)

    // Continuous page fraction (0f = Pictures, 1f = Albums) for real-time sliding tab indicator
    val pageProgress by remember {
        derivedStateOf {
            (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
        }
    }

    var showMoreMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Samsung One UI Reachability Viewing Area (Expands when at top, collapses smoothly on scroll)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(viewingAreaHeight)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (viewingAreaHeight > 36.dp) {
                AnimatedContent(
                    targetState = pagerState.currentPage,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "heroHeaderCrossfade",
                    modifier = Modifier.graphicsLayer {
                        alpha = heroAlpha
                        translationY = (1f - heroAlpha) * -20f
                    }
                ) { activePage ->
                    Column {
                        Text(
                            text = if (activePage == 0) stringResource(R.string.pictures) else stringResource(R.string.albums),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 36.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (activePage == 0) {
                                    stringResource(R.string.items_count, picturesState.totalCount)
                                } else {
                                    "${albumsState.albums.size} albums"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Top Navigation & Action Bar with Live Sliding Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Segmented Tabs with Live Pager Offset Indicator
            var tab0WidthPx by remember { mutableIntStateOf(0) }
            var tab1WidthPx by remember { mutableIntStateOf(0) }
            var tab1OffsetXPx by remember { mutableIntStateOf(0) }

            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pictures Tab
                    val isPicturesSelected = pagerState.currentPage == 0
                    val picturesTextColor by animateColorAsState(
                        targetValue = if (isPicturesSelected) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        label = "picturesTabColor"
                    )
                    Row(
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                tab0WidthPx = coords.size.width
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pictures",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                lineHeight = 22.sp
                            ),
                            fontWeight = if (isPicturesSelected) FontWeight.Bold else FontWeight.Medium,
                            color = picturesTextColor,
                            maxLines = 1,
                            softWrap = false
                        )
                        // Compact count appears only when reachability header collapses, using identical font metrics
                        AnimatedVisibility(
                            visible = isScrolled,
                            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
                        ) {
                            Text(
                                text = "${picturesState.totalCount}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    lineHeight = 22.sp
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = if (isPicturesSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(start = 5.dp)
                            )
                        }
                    }

                    // Albums Tab
                    val isAlbumsSelected = pagerState.currentPage == 1
                    val albumsTextColor by animateColorAsState(
                        targetValue = if (isAlbumsSelected) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        label = "albumsTabColor"
                    )
                    Row(
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                tab1WidthPx = coords.size.width
                                tab1OffsetXPx = coords.positionInParent().x.roundToInt()
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                coroutineScope.launch { pagerState.animateScrollToPage(1) }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Albums",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                lineHeight = 22.sp
                            ),
                            fontWeight = if (isAlbumsSelected) FontWeight.Bold else FontWeight.Medium,
                            color = albumsTextColor,
                            maxLines = 1,
                            softWrap = false
                        )
                        // Compact count appears only when reachability header collapses, using identical font metrics
                        AnimatedVisibility(
                            visible = isScrolled,
                            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
                        ) {
                            Text(
                                text = "${albumsState.albums.size}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    lineHeight = 22.sp
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAlbumsSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(start = 5.dp)
                            )
                        }
                    }
                }

                // Sliding Pill Indicator tracking HorizontalPager offset & measured tab centers in real time
                val indicatorWidthDp = 52.dp
                Box(
                    modifier = Modifier.height(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .offset {
                                val indicatorWidthPx = indicatorWidthDp.roundToPx()
                                val center0 = tab0WidthPx / 2f
                                val center1 = tab1OffsetXPx + tab1WidthPx / 2f
                                val currentCenter = center0 + (center1 - center0) * pageProgress
                                val xPx = (currentCenter - indicatorWidthPx / 2f).roundToInt().coerceAtLeast(0)
                                IntOffset(xPx, 0)
                            }
                            .width(indicatorWidthDp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
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

                AnimatedVisibility(
                    visible = pagerState.currentPage == 1,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
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

                    var showSortDialog by remember { mutableStateOf(false) }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort by") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) },
                            trailingIcon = {
                                Text(
                                    text = picturesState.sortOrder.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                showSortDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Stories") },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showMoreMenu = false
                                onStoriesClick()
                            }
                        )
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
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                onSettingsClick()
                            }
                        )
                    }

                    if (showSortDialog) {
                        AlertDialog(
                            onDismissRequest = { showSortDialog = false },
                            title = { Text("Sort Pictures") },
                            text = {
                                Column {
                                    MediaSortOrder.values().forEach { order ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    picturesViewModel.setSortOrder(order)
                                                    showSortDialog = false
                                                }
                                                .padding(horizontal = 8.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(order.label, style = MaterialTheme.typography.bodyLarge)
                                            if (picturesState.sortOrder == order) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showSortDialog = false }) {
                                    Text("Close")
                                }
                            }
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
                    albums = albumsState.albums,
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
                    onStoriesClick = onStoriesClick,
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
    albums: List<Album>,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onMediaClick: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    var showMoveDialog by remember { mutableStateOf(false) }

    val allItems = remember(uiState.dateGroups) {
        uiState.dateGroups.flatMap { it.items }
    }

    // Flat index -> dateMillis lookup for FastScrollScrubber (including sticky headers + media items)
    val indexDateMillisList = remember(uiState.dateGroups) {
        buildList {
            uiState.dateGroups.forEach { group ->
                add(group.dateMillis) // header entry
                repeat(group.items.size) {
                    add(group.dateMillis)
                }
            }
        }
    }
    val dateFormatter = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(uiState.columnCount) {
                // Only consume pointer events when 2+ fingers are performing a pinch zoom to change columns.
                // Single-finger horizontal swipes pass through to HorizontalPager to switch between Pictures & Albums!
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
                val orderedIds = remember(allItems) { allItems.map { it.id } }
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(uiState.columnCount),
                        contentPadding = PaddingValues(bottom = 24.dp),
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

                    FastScrollScrubber(
                        gridState = gridState,
                        totalItemsCount = indexDateMillisList.size,
                        dateLabelProvider = { index ->
                            val millis = indexDateMillisList.getOrNull(index.coerceIn(0, (indexDateMillisList.size - 1).coerceAtLeast(0)))
                            if (millis != null && millis > 0L) dateFormatter.format(Date(millis)) else ""
                        }
                    )
                }
            }
        }

        // Selection Action Bar floating at bottom
        AnimatedVisibility(
            visible = uiState.isSelectionMode,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SelectionActionBar(
                selectedCount = uiState.selectedItemIds.size,
                onClose = { viewModel.clearSelection() },
                onSelectAll = { viewModel.selectAll(allItems) },
                onShare = { viewModel.shareSelected(allItems) },
                onFavorite = {
                    viewModel.favoriteSelected(allItems)
                    Toast.makeText(context, "Updated favorites", Toast.LENGTH_SHORT).show()
                },
                onMove = { showMoveDialog = true },
                onHide = {
                    viewModel.hideSelected(allItems)
                    Toast.makeText(context, "Moved to Secure Folder", Toast.LENGTH_SHORT).show()
                },
                onDelete = { viewModel.deleteSelected(allItems) }
            )
        }

        if (showMoveDialog) {
            MoveToAlbumDialog(
                albums = albums,
                selectedCount = uiState.selectedItemIds.size,
                onAlbumSelected = { targetAlbum ->
                    showMoveDialog = false
                    viewModel.moveSelectedToAlbum(targetAlbum, allItems) { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { showMoveDialog = false }
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
    onStoriesClick: () -> Unit = {},
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
                // Shortcuts Section (Favorites, Stories, Secure Folder, Recycle Bin)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                icon = Icons.Default.AutoAwesome,
                                iconTint = MaterialTheme.colorScheme.primary,
                                title = stringResource(R.string.stories),
                                count = 0,
                                onClick = onStoriesClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
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
