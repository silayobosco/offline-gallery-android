package com.boscoovic.offlinegallery.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

/**
 * Enables Samsung One UI / Google Photos style drag-to-select on a LazyVerticalGrid.
 * Long-press any media thumbnail and slide your finger across the grid to select or deselect
 * a continuous range of items, with automatic edge scrolling.
 */
fun Modifier.dragSelectGrid(
    gridState: LazyGridState,
    orderedIds: List<Long>,
    selectedIds: Set<Long>,
    onSelectionChanged: (Set<Long>) -> Unit,
    enabled: Boolean = true
): Modifier = composed {
    if (!enabled) return@composed this

    val haptic = LocalHapticFeedback.current
    val currentOrderedIds by rememberUpdatedState(orderedIds)
    val currentSelectedIds by rememberUpdatedState(selectedIds)
    val currentOnSelectionChanged by rememberUpdatedState(onSelectionChanged)

    var isDragging by remember { mutableStateOf(false) }
    var autoScrollSpeed by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isDragging, autoScrollSpeed) {
        while (isDragging && autoScrollSpeed != 0f) {
            gridState.scrollBy(autoScrollSpeed)
            delay(16L)
        }
    }

    this.pointerInput(gridState, enabled) {
        var anchorIndex = -1
        var targetSelect = true
        var initialSelection: Set<Long> = emptySet()

        fun findItemIndexAt(position: Offset, idList: List<Long>): Int {
            val visibleItems = gridState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return -1

            val exactHit = visibleItems.firstOrNull { item ->
                val left = item.offset.x.toFloat()
                val top = item.offset.y.toFloat()
                val right = left + item.size.width.toFloat()
                val bottom = top + item.size.height.toFloat()
                position.x in left..right && position.y in top..bottom && (item.key as? Long) != null
            }
            val candidate: LazyGridItemInfo? = exactHit ?: visibleItems
                .filter { (it.key as? Long) != null }
                .minByOrNull { item ->
                    val cx = item.offset.x + item.size.width / 2f
                    val cy = item.offset.y + item.size.height / 2f
                    val dx = cx - position.x
                    val dy = cy - position.y
                    dx * dx + dy * dy
                }

            val hitId = candidate?.key as? Long ?: return -1
            return idList.indexOf(hitId)
        }

        fun applyRange(base: Set<Long>, ids: List<Long>, from: Int, to: Int, select: Boolean): Set<Long> {
            if (from < 0 || to < 0 || ids.isEmpty()) return base
            val start = min(from, to).coerceIn(0, ids.lastIndex)
            val end = max(from, to).coerceIn(0, ids.lastIndex)
            val updated = base.toMutableSet()
            for (i in start..end) {
                if (select) updated.add(ids[i]) else updated.remove(ids[i])
            }
            return updated
        }

        detectDragGesturesAfterLongPress(
            onDragStart = { startOffset ->
                val ids = currentOrderedIds
                val hitIdx = findItemIndexAt(startOffset, ids)
                if (hitIdx >= 0) {
                    val hitId = ids[hitIdx]
                    initialSelection = currentSelectedIds
                    anchorIndex = hitIdx
                    targetSelect = !initialSelection.contains(hitId)
                    isDragging = true
                    autoScrollSpeed = 0f
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnSelectionChanged(
                        applyRange(initialSelection, ids, anchorIndex, anchorIndex, targetSelect)
                    )
                }
            },
            onDrag = { change, _ ->
                if (!isDragging || anchorIndex < 0) return@detectDragGesturesAfterLongPress
                change.consume()

                val viewportHeight = gridState.layoutInfo.viewportSize.height.toFloat()
                val edgeThreshold = 110f
                autoScrollSpeed = when {
                    change.position.y < edgeThreshold -> {
                        val factor = ((edgeThreshold - change.position.y) / edgeThreshold).coerceIn(0f, 1f)
                        -24f * factor
                    }
                    change.position.y > viewportHeight - edgeThreshold -> {
                        val factor = ((change.position.y - (viewportHeight - edgeThreshold)) / edgeThreshold).coerceIn(0f, 1f)
                        24f * factor
                    }
                    else -> 0f
                }

                val ids = currentOrderedIds
                val hitIdx = findItemIndexAt(change.position, ids)
                if (hitIdx >= 0) {
                    currentOnSelectionChanged(
                        applyRange(initialSelection, ids, anchorIndex, hitIdx, targetSelect)
                    )
                }
            },
            onDragEnd = {
                isDragging = false
                autoScrollSpeed = 0f
                anchorIndex = -1
            },
            onDragCancel = {
                isDragging = false
                autoScrollSpeed = 0f
                anchorIndex = -1
            }
        )
    }
}

