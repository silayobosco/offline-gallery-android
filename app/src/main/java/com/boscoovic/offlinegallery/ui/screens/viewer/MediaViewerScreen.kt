package com.boscoovic.offlinegallery.ui.screens.viewer

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boscoovic.offlinegallery.R
import com.boscoovic.offlinegallery.data.model.MediaType
import com.boscoovic.offlinegallery.ui.theme.FavoriteRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MediaViewerScreen(
    viewModel: MediaViewerViewModel,
    onBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    onSlideshowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }

    // Heart pop animation state for double-tap liking
    var heartAnimTrigger by remember { mutableLongStateOf(0L) }
    val heartScale = remember { Animatable(0f) }
    val heartAlpha = remember { Animatable(0f) }

    LaunchedEffect(heartAnimTrigger) {
        if (heartAnimTrigger > 0) {
            heartScale.snapTo(0.2f)
            heartAlpha.snapTo(1f)
            // Spring bounce up to 1.3f
            heartScale.animateTo(
                targetValue = 1.3f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            // Settle and fade out
            delay(150)
            heartAlpha.animateTo(0f, animationSpec = tween(350))
            heartScale.snapTo(0f)
        }
    }

    if (uiState.items.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("No media", color = Color.White)
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = uiState.currentIndex,
        pageCount = { uiState.items.size }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.onPageChanged(page)
        }
    }

    val currentItem = uiState.items.getOrNull(pagerState.currentPage)

    val coroutineScope = rememberCoroutineScope()
    var videoEndAction by rememberSaveable { mutableStateOf(VideoEndAction.REPEAT_ONE) }
    var playbackSpeed by rememberSaveable { mutableFloatStateOf(1.0f) }
    var feedbackNotice by remember { mutableStateOf<String?>(null) }
    var showQueueMenu by remember { mutableStateOf(false) }

    LaunchedEffect(feedbackNotice) {
        if (feedbackNotice != null) {
            delay(2200L)
            feedbackNotice = null
        }
    }

    LaunchedEffect(uiState.currentIndex) {
        if (pagerState.currentPage != uiState.currentIndex && uiState.currentIndex in uiState.items.indices) {
            pagerState.scrollToPage(uiState.currentIndex)
        }
    }

    // Video playback & TikTok scrub state
    var isVideoPlaying by remember(currentItem?.id) { mutableStateOf(false) }
    var videoCurrentPosition by remember(currentItem?.id) { mutableLongStateOf(0L) }
    var videoDuration by remember(currentItem?.id) { mutableLongStateOf(0L) }
    var playPauseToggleTrigger by remember(currentItem?.id) { mutableLongStateOf(0L) }
    var seekPositionRequest by remember(currentItem?.id) { mutableStateOf<Long?>(null) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }

    // Gesture detection: Swipe Left (< -120f) for Details, Swipe Right (> 120f) to Delete
    var totalHorizontalDrag by remember { mutableFloatStateOf(0f) }

    val handleDoubleTapLike = {
        viewModel.setFavorite(true)
        heartAnimTrigger = System.currentTimeMillis()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(pagerState.currentPage) {
                detectHorizontalDragGestures(
                    onDragStart = { totalHorizontalDrag = 0f },
                    onDragEnd = {
                        if (totalHorizontalDrag < -120f) {
                            // Swiped Left -> Open Details
                            viewModel.toggleDetailsSheet(true)
                        } else if (totalHorizontalDrag > 120f) {
                            // Swiped Right -> Prompt Delete
                            showDeleteConfirmDialog = true
                        }
                        totalHorizontalDrag = 0f
                    },
                    onDragCancel = { totalHorizontalDrag = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        totalHorizontalDrag += dragAmount
                    }
                )
            }
    ) {
        // Vertical Pager (TikTok style)
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = uiState.items[page]
            val isActive = page == pagerState.currentPage
            if (item.mediaType == MediaType.VIDEO) {
                VideoPlayerView(
                    item = item,
                    isActive = isActive,
                    playPauseToggleTrigger = if (isActive) playPauseToggleTrigger else 0L,
                    seekPositionRequest = if (isActive) seekPositionRequest else null,
                    videoEndAction = videoEndAction,
                    playbackSpeed = playbackSpeed,
                    onVideoEnded = {
                        val nextIdx = pagerState.currentPage + 1
                        if (nextIdx < uiState.items.size) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(nextIdx)
                            }
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        }
                    },
                    onPlayerStateUpdated = { playing, pos, dur ->
                        if (isActive) {
                            isVideoPlaying = playing
                            videoCurrentPosition = pos
                            videoDuration = dur
                        }
                    },
                    onTap = { viewModel.toggleChrome() },
                    onDoubleTapLike = handleDoubleTapLike
                )
            } else {
                ZoomableImage(
                    item = item,
                    onTap = { viewModel.toggleChrome() },
                    onDoubleTapLike = handleDoubleTapLike
                )
            }
        }

        // Bouncy Heart Pop Animation overlay on double-tap
        if (heartAlpha.value > 0f) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = FavoriteRed,
                modifier = Modifier
                    .size(110.dp)
                    .align(Alignment.Center)
                    .graphicsLayer(
                        scaleX = heartScale.value,
                        scaleY = heartScale.value,
                        alpha = heartAlpha.value
                    )
            )
        }

        // Date/Time formatting for top bar
        val formattedDate = remember(currentItem) {
            val ts = currentItem?.let { if (it.dateTaken > 0) it.dateTaken else it.dateAdded * 1000L } ?: 0L
            if (ts > 0) {
                java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(ts))
            } else {
                ""
            }
        }
        val formattedTime = remember(currentItem) {
            val ts = currentItem?.let { if (it.dateTaken > 0) it.dateTaken else it.dateAdded * 1000L } ?: 0L
            if (ts > 0) {
                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ts))
            } else {
                ""
            }
        }

        // Top Controls Header (Edge-to-Edge with statusBarsPadding)
        AnimatedVisibility(
            visible = uiState.isChromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Back button + Date (replaced 10/800 counter)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    if (formattedDate.isNotEmpty()) {
                        Spacer(modifier = Modifier.size(12.dp))
                        Column {
                            Text(
                                text = formattedDate,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            if (formattedTime.isNotEmpty()) {
                                Text(
                                    text = formattedTime,
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Right: Top Kebab Menu controlling Queue & Playback (Shuffle, Video End Behavior, Playback Speed, Slideshow)
                Box {
                    IconButton(
                        onClick = { showQueueMenu = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Queue & Playback controls",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showQueueMenu,
                        onDismissRequest = { showQueueMenu = false }
                    ) {
                        // 1. Shuffle
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("Shuffle", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = when (uiState.shuffleMode) {
                                            ShuffleMode.OFF -> "Off (Normal order)"
                                            ShuffleMode.FOLDER -> "Within folder (${currentItem?.bucketName ?: "Folder"})"
                                            ShuffleMode.UNIVERSAL -> "Universal (All media)"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (uiState.shuffleMode != ShuffleMode.OFF) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (uiState.shuffleMode) {
                                        ShuffleMode.OFF -> Icons.Default.Shuffle
                                        ShuffleMode.FOLDER -> Icons.Default.FolderSpecial
                                        ShuffleMode.UNIVERSAL -> Icons.Default.Shuffle
                                    },
                                    contentDescription = null,
                                    tint = if (uiState.shuffleMode != ShuffleMode.OFF) MaterialTheme.colorScheme.primary else Color.White
                                )
                            },
                            onClick = {
                                val nextMode = viewModel.cycleShuffleMode()
                                feedbackNotice = when (nextMode) {
                                    ShuffleMode.OFF -> "Shuffle: Off (Normal order)"
                                    ShuffleMode.FOLDER -> "Shuffle: Within folder (${currentItem?.bucketName ?: "Folder"})"
                                    ShuffleMode.UNIVERSAL -> "Shuffle: Universal (All media)"
                                }
                            }
                        )

                        // 2. When Video Ends (Repeat / Loop / Next / Stop)
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("When Video Ends", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = when (videoEndAction) {
                                            VideoEndAction.REPEAT_ONE -> "Repeat same video (Loop)"
                                            VideoEndAction.NEXT -> "Auto play next item"
                                            VideoEndAction.STOP -> "Stop at end"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (videoEndAction) {
                                        VideoEndAction.REPEAT_ONE -> Icons.Default.RepeatOne
                                        VideoEndAction.NEXT -> Icons.Default.SkipNext
                                        VideoEndAction.STOP -> Icons.Default.StopCircle
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                val nextAction = when (videoEndAction) {
                                    VideoEndAction.REPEAT_ONE -> VideoEndAction.NEXT
                                    VideoEndAction.NEXT -> VideoEndAction.STOP
                                    VideoEndAction.STOP -> VideoEndAction.REPEAT_ONE
                                }
                                videoEndAction = nextAction
                                feedbackNotice = when (nextAction) {
                                    VideoEndAction.REPEAT_ONE -> "When video ends: Repeat same video 🔂"
                                    VideoEndAction.NEXT -> "When video ends: Auto play next ⏭️"
                                    VideoEndAction.STOP -> "When video ends: Stop at end ⏹️"
                                }
                            }
                        )

                        // 3. Playback Speed toggle
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("Playback Speed", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "${playbackSpeed}x (Tap to toggle: 0.5x, 1x, 1.25x, 1.5x, 2x)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = if (playbackSpeed != 1.0f) MaterialTheme.colorScheme.primary else Color.White
                                )
                            },
                            onClick = {
                                val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f, 0.5f)
                                val currentIdx = speeds.indexOfFirst { kotlin.math.abs(it - playbackSpeed) < 0.05f }
                                val nextSpeed = if (currentIdx >= 0) speeds[(currentIdx + 1) % speeds.size] else 1.0f
                                playbackSpeed = nextSpeed
                                feedbackNotice = "Playback Speed: ${nextSpeed}x"
                            }
                        )

                        HorizontalDivider()

                        // 4. Start Slideshow
                        DropdownMenuItem(
                            text = { Text("Start Slideshow") },
                            leadingIcon = {
                                Icon(Icons.Default.PlayCircle, contentDescription = null)
                            },
                            onClick = {
                                showQueueMenu = false
                                onSlideshowClick()
                            }
                        )
                    }
                }
            }
        }

        // Floating Pill Counter below the top bar
        AnimatedVisibility(
            visible = uiState.isChromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 58.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${uiState.items.size}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Floating feedback banner for Shuffle / Repeat status changes
        AnimatedVisibility(
            visible = feedbackNotice != null,
            enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { -30 }),
            exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { -30 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 96.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = feedbackNotice.orEmpty(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // TikTok-style Vertical Action Column (Lower Right, sits above bottom description area)
        AnimatedVisibility(
            visible = uiState.isChromeVisible && currentItem != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 100.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Play / Pause (Only for videos)
                if (currentItem?.mediaType == MediaType.VIDEO) {
                    TikTokActionButton(
                        icon = if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        label = if (isVideoPlaying) "Pause" else "Play",
                        iconTint = Color.White,
                        onClick = {
                            playPauseToggleTrigger = System.currentTimeMillis()
                        }
                    )
                }

                // Like (Favorite)
                TikTokActionButton(
                    icon = if (currentItem?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    label = if (currentItem?.isFavorite == true) "Liked" else "Like",
                    iconTint = if (currentItem?.isFavorite == true) FavoriteRed else Color.White,
                    onClick = {
                        val newFav = !(currentItem?.isFavorite ?: false)
                        viewModel.toggleFavorite()
                        if (newFav) {
                            heartAnimTrigger = System.currentTimeMillis()
                        }
                    }
                )

                // Share
                TikTokActionButton(
                    icon = Icons.Default.Share,
                    label = stringResource(R.string.share),
                    iconTint = Color.White,
                    onClick = { viewModel.shareCurrent() }
                )

                // Edit (for images)
                if (currentItem?.mediaType == MediaType.IMAGE) {
                    TikTokActionButton(
                        icon = Icons.Default.Edit,
                        label = stringResource(R.string.edit),
                        iconTint = Color.White,
                        onClick = { currentItem?.let { onEditClick(it.id) } }
                    )
                }

                // Kebab Menu (More) - Replaced Delete button
                TikTokActionButton(
                    icon = Icons.Default.MoreVert,
                    label = "More",
                    iconTint = Color.White,
                    onClick = { viewModel.toggleMoreSheet(true) }
                )
            }
        }

        // Full-width Bottom Container (Progress Bar placed above Description Nav Bar)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .then(if (!uiState.isChromeVisible) Modifier.navigationBarsPadding() else Modifier)
        ) {
            // TikTok Progress / Seek Bar directly above the bottom description bar
            if (currentItem?.mediaType == MediaType.VIDEO && videoDuration > 0L) {
                val progressFraction = if (isScrubbing) scrubFraction else {
                    (videoCurrentPosition.toFloat() / videoDuration.toFloat()).coerceIn(0f, 1f)
                }
                val scrubTimeMs = (progressFraction * videoDuration).toLong().coerceIn(0L, videoDuration)

                TikTokProgressBar(
                    progressFraction = progressFraction,
                    scrubTimeMs = scrubTimeMs,
                    totalDurationMs = videoDuration,
                    isScrubbing = isScrubbing,
                    onScrubStart = {
                        isScrubbing = true
                        scrubFraction = (videoCurrentPosition.toFloat() / videoDuration.toFloat()).coerceIn(0f, 1f)
                    },
                    onScrub = { frac ->
                        scrubFraction = frac.coerceIn(0f, 1f)
                        seekPositionRequest = (scrubFraction * videoDuration).toLong()
                    },
                    onScrubEnd = {
                        val finalSeek = (scrubFraction * videoDuration).toLong()
                        seekPositionRequest = finalSeek
                        isScrubbing = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Full-Width Description Bar with shadow filling entire bottom as nav bar
            AnimatedVisibility(
                visible = uiState.isChromeVisible && currentItem != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxWidth()
            ) {
                val textShadow = Shadow(
                    color = Color.Black.copy(alpha = 0.85f),
                    offset = Offset(2f, 2f),
                    blurRadius = 6f
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.65f),
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Black.copy(alpha = 0.95f)
                                )
                            )
                        )
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 20.dp, top = 10.dp, bottom = 12.dp)
                ) {
                    Column {
                        currentItem?.let { item ->
                            Text(
                                text = item.displayName,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(shadow = textShadow),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${item.bucketName}  •  ${item.formattedSize}",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodySmall.copy(shadow = textShadow)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Swipe left for details, swipe right to delete",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.labelSmall.copy(shadow = textShadow)
                            )
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Move to Recycle Bin?") },
                text = { Text("This item will be kept in the Recycle Bin for 30 days before being permanently removed.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.deleteCurrent(onDeleted = {
                                if (uiState.items.size <= 1) {
                                    onBack()
                                }
                            })
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        // Details Bottom Sheet
        if (uiState.showDetailsSheet && currentItem != null) {
            MediaDetailsSheet(
                item = currentItem,
                exif = uiState.currentExif,
                onDismiss = { viewModel.toggleDetailsSheet(false) }
            )
        }

        // Kebab More Bottom Sheet
        if (uiState.showMoreSheet && currentItem != null) {
            MediaViewerMoreSheet(
                onDismiss = { viewModel.toggleMoreSheet(false) },
                onUseAs = { viewModel.useAs() },
                onHide = {
                    viewModel.hideCurrent {
                        Toast.makeText(context, "Item hidden", Toast.LENGTH_SHORT).show()
                        if (uiState.items.size <= 1) {
                            onBack()
                        }
                    }
                },
                onMove = { showMoveDialog = true },
                onEditIn = {
                    viewModel.editIn { fallbackId ->
                        onEditClick(fallbackId)
                    }
                },
                onInfo = { viewModel.toggleDetailsSheet(true) },
                onDelete = { showDeleteConfirmDialog = true }
            )
        }

        // Move to Album Dialog
        if (showMoveDialog && currentItem != null) {
            AlertDialog(
                onDismissRequest = { showMoveDialog = false },
                title = { Text("Move to Album") },
                text = {
                    if (uiState.allAlbums.isEmpty()) {
                        Text("No albums available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        ) {
                            items(uiState.allAlbums, key = { "${it.isCustom}_${it.id}" }) { album ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            showMoveDialog = false
                                            viewModel.moveCurrentToAlbum(album) {
                                                Toast.makeText(context, "Moved to ${album.name}", Toast.LENGTH_SHORT).show()
                                                if (uiState.items.size <= 1) {
                                                    onBack()
                                                }
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (album.isCustom) Icons.Default.FolderSpecial else Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.size(12.dp))
                                    Text(
                                        text = album.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showMoveDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaViewerMoreSheet(
    onDismiss: () -> Unit,
    onUseAs: () -> Unit,
    onHide: () -> Unit,
    onMove: () -> Unit,
    onEditIn: () -> Unit,
    onInfo: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            MoreSheetItem(
                icon = Icons.Default.Wallpaper,
                title = "Use as",
                onClick = {
                    onDismiss()
                    onUseAs()
                }
            )
            MoreSheetItem(
                icon = Icons.Default.VisibilityOff,
                title = "Hide",
                onClick = {
                    onDismiss()
                    onHide()
                }
            )
            MoreSheetItem(
                icon = Icons.AutoMirrored.Filled.DriveFileMove,
                title = "Move to album",
                onClick = {
                    onDismiss()
                    onMove()
                }
            )
            MoreSheetItem(
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                title = "Edit in...",
                onClick = {
                    onDismiss()
                    onEditIn()
                }
            )
            MoreSheetItem(
                icon = Icons.Default.Info,
                title = "Details",
                onClick = {
                    onDismiss()
                    onInfo()
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            MoreSheetItem(
                icon = Icons.Default.Delete,
                title = stringResource(R.string.delete),
                iconTint = MaterialTheme.colorScheme.error,
                textColor = MaterialTheme.colorScheme.error,
                onClick = {
                    onDismiss()
                    onDelete()
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MoreSheetItem(
    icon: ImageVector,
    title: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = title,
            color = textColor,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TikTokActionButton(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
