package com.boscoovic.offlinegallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FastScrollScrubber(
    gridState: LazyGridState,
    totalItemsCount: Int,
    dateLabelProvider: (Int) -> String,
    modifier: Modifier = Modifier
) {
    if (totalItemsCount <= 12) return

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var isDraggingThumb by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var isThumbVisible by remember { mutableStateOf(false) }

    val isScrolling = gridState.isScrollInProgress

    LaunchedEffect(isScrolling, isDraggingThumb) {
        if (isScrolling || isDraggingThumb) {
            isThumbVisible = true
        } else if (isThumbVisible) {
            delay(1500L)
            isThumbVisible = false
        }
    }

    val scrollFraction by remember(totalItemsCount) {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalGridItems = layoutInfo.totalItemsCount.coerceAtLeast(totalItemsCount)
            val visibleCount = layoutInfo.visibleItemsInfo.size
            val maxScrollIndex = (totalGridItems - visibleCount).coerceAtLeast(1)
            (gridState.firstVisibleItemIndex.toFloat() / maxScrollIndex.toFloat()).coerceIn(0f, 1f)
        }
    }

    val activeFraction = if (isDraggingThumb) dragFraction else scrollFraction

    val currentDateText by remember(activeFraction, totalItemsCount) {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalGridItems = layoutInfo.totalItemsCount.coerceAtLeast(totalItemsCount)
            val targetIdx = if (isDraggingThumb) {
                (activeFraction * (totalGridItems - 1).coerceAtLeast(0)).roundToInt()
            } else {
                gridState.firstVisibleItemIndex
            }
            dateLabelProvider(targetIdx)
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        val trackHeightPx = with(density) { maxHeight.toPx() }
        val thumbHeightDp = 52.dp
        val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
        val verticalPaddingPx = with(density) { 12.dp.toPx() }
        val usableTrackPx = (trackHeightPx - thumbHeightPx - verticalPaddingPx * 2f).coerceAtLeast(1f)
        val thumbOffsetY = (verticalPaddingPx + activeFraction * usableTrackPx).roundToInt()

        val thumbWidth by animateDpAsState(
            targetValue = if (isDraggingThumb) 8.dp else 5.dp,
            animationSpec = tween(150),
            label = "thumbWidth"
        )

        AnimatedVisibility(
            visible = isThumbVisible || isDraggingThumb,
            enter = fadeIn(tween(180)) + slideInHorizontally(tween(180)) { it / 2 },
            exit = fadeOut(tween(250)) + slideOutHorizontally(tween(250)) { it / 2 },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 2.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                // Floating Date Bubble + Thumb Row positioned at thumbOffsetY
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.offset { IntOffset(0, thumbOffsetY) }
                ) {
                    AnimatedVisibility(
                        visible = isDraggingThumb && currentDateText.isNotEmpty(),
                        enter = fadeIn(tween(120)) + scaleIn(tween(120), initialScale = 0.85f),
                        exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.85f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shadowElevation = 6.dp
                        ) {
                            Text(
                                text = currentDateText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Visual Thumb Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDraggingThumb) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        },
                        shadowElevation = if (isDraggingThumb) 4.dp else 1.dp,
                        modifier = Modifier
                            .width(thumbWidth)
                            .height(thumbHeightDp)
                    ) {}
                }

                // Invisible Drag Touch Strip along the right edge
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .fillMaxHeight()
                        .pointerInput(totalItemsCount) {
                            detectVerticalDragGestures(
                                onDragStart = { startOffset ->
                                    isDraggingThumb = true
                                    val newFraction = ((startOffset.y - verticalPaddingPx - thumbHeightPx / 2f) / usableTrackPx)
                                        .coerceIn(0f, 1f)
                                    dragFraction = newFraction
                                    val totalGridItems = gridState.layoutInfo.totalItemsCount.coerceAtLeast(totalItemsCount)
                                    val targetIndex = (newFraction * (totalGridItems - 1).coerceAtLeast(0)).roundToInt()
                                    coroutineScope.launch {
                                        gridState.scrollToItem(targetIndex)
                                    }
                                },
                                onVerticalDrag = { change, _ ->
                                    change.consume()
                                    val newFraction = ((change.position.y - verticalPaddingPx - thumbHeightPx / 2f) / usableTrackPx)
                                        .coerceIn(0f, 1f)
                                    dragFraction = newFraction
                                    val totalGridItems = gridState.layoutInfo.totalItemsCount.coerceAtLeast(totalItemsCount)
                                    val targetIndex = (newFraction * (totalGridItems - 1).coerceAtLeast(0)).roundToInt()
                                    coroutineScope.launch {
                                        gridState.scrollToItem(targetIndex)
                                    }
                                },
                                onDragEnd = {
                                    isDraggingThumb = false
                                },
                                onDragCancel = {
                                    isDraggingThumb = false
                                }
                            )
                        }
                )
            }
        }
    }
}

