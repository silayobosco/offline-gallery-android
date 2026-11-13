package com.boscoovic.offlinegallery.ui.screens.slideshow

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.boscoovic.offlinegallery.data.model.MediaFilter
import com.boscoovic.offlinegallery.data.repository.MediaRepository
import kotlinx.coroutines.delay

@Composable
fun SlideshowScreen(
    repository: MediaRepository,
    contextType: String = "ALL",
    contextArg: String = "",
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mediaFlow = remember(contextType, contextArg) {
        when (contextType) {
            "FAVORITES" -> repository.getAllMedia(MediaFilter.FAVORITES)
            "ALBUM" -> {
                val albumId = contextArg.toLongOrNull() ?: 0L
                repository.getAlbumMedia(albumId, isCustom = false)
            }
            "CUSTOM_ALBUM" -> {
                val albumId = contextArg.toLongOrNull() ?: 0L
                repository.getAlbumMedia(albumId, isCustom = true)
            }
            "SECURE" -> repository.getSecureItems()
            else -> repository.getAllMedia()
        }
    }

    val items by mediaFlow.collectAsState(initial = emptyList())
    var currentIndex by remember { mutableIntStateOf(0) }
    var intervalSeconds by remember { mutableLongStateOf(3L) }
    var isPlaying by remember { mutableStateOf(true) }
    var isShuffle by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(items.size) {
        if (currentIndex >= items.size && items.isNotEmpty()) {
            currentIndex = 0
        }
    }

    LaunchedEffect(isPlaying, currentIndex, intervalSeconds, items, isShuffle) {
        if (isPlaying && items.isNotEmpty()) {
            delay(intervalSeconds * 1000L)
            currentIndex = if (isShuffle && items.size > 1) {
                var next = (0 until items.size).random()
                while (next == currentIndex) {
                    next = (0 until items.size).random()
                }
                next
            } else {
                (currentIndex + 1) % items.size
            }
        }
    }

    if (items.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No items for slideshow", color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }
        }
        return
    }

    val safeIndex = currentIndex.coerceIn(0, items.lastIndex)
    val currentItem = items[safeIndex]

    // Subtle Ken Burns zoom animation per slide
    var zoomTarget by remember(currentItem.id, safeIndex) { mutableStateOf(false) }
    LaunchedEffect(currentItem.id, safeIndex) {
        zoomTarget = true
    }
    val kenBurnsScale by animateFloatAsState(
        targetValue = if (zoomTarget && isPlaying) 1.07f else 1.0f,
        animationSpec = tween(
            durationMillis = (intervalSeconds * 1000L).toInt(),
            easing = LinearEasing
        ),
        label = "kenBurnsScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls }
    ) {
        Crossfade(
            targetState = currentItem,
            animationSpec = tween(900),
            label = "slideshow_crossfade"
        ) { item ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = item.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = kenBurnsScale,
                        scaleY = kenBurnsScale
                    )
            )
        }

        // Top progress indicator across all slides
        LinearProgressIndicator(
            progress = { (safeIndex + 1).toFloat() / items.size.toFloat() },
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.White.copy(alpha = 0.25f),
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(3.dp)
                .align(Alignment.TopCenter)
        )

        if (showControls) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentItem.displayName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${safeIndex + 1} / ${items.size}",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { isPlaying = !isPlaying }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White
                    )
                }

                IconButton(onClick = { isShuffle = !isShuffle }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) MaterialTheme.colorScheme.primary else Color.White
                    )
                }

                listOf(2L, 3L, 5L).forEach { sec ->
                    FilterChip(
                        selected = intervalSeconds == sec,
                        onClick = { intervalSeconds = sec },
                        label = { Text("${sec}s") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
    }
}
