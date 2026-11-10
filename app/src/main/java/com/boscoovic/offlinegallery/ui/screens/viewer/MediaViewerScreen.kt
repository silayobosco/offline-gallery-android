package com.boscoovic.offlinegallery.ui.screens.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boscoovic.offlinegallery.R
import com.boscoovic.offlinegallery.data.model.MediaType
import com.boscoovic.offlinegallery.ui.theme.FavoriteRed
import kotlinx.coroutines.delay

@Composable
fun MediaViewerScreen(
    viewModel: MediaViewerViewModel,
    onBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    onSlideshowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = "${pagerState.currentPage + 1} / ${uiState.items.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = onSlideshowClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = stringResource(R.string.slideshow),
                        tint = Color.White
                    )
                }
            }
        }

        // Media Info Overlay on Bottom Left
        AnimatedVisibility(
            visible = uiState.isChromeVisible && currentItem != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, bottom = 24.dp, end = 80.dp)
        ) {
            Column {
                currentItem?.let { item ->
                    Text(
                        text = item.displayName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${item.bucketName}  •  ${item.formattedSize}",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Swipe left for details, swipe right to delete",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // TikTok-style Vertical Action Column (Lower Right)
        AnimatedVisibility(
            visible = uiState.isChromeVisible && currentItem != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Favorite
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

                // Details (Info)
                TikTokActionButton(
                    icon = Icons.Default.Info,
                    label = "Details",
                    iconTint = Color.White,
                    onClick = { viewModel.toggleDetailsSheet(true) }
                )

                // Delete
                TikTokActionButton(
                    icon = Icons.Default.Delete,
                    label = stringResource(R.string.delete),
                    iconTint = Color.White,
                    onClick = { showDeleteConfirmDialog = true }
                )
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
