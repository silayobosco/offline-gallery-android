package com.boscoovic.offlinegallery.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

/**
 * Enables Samsung One UI / Google Photos style drag-to-select on a LazyVerticalGrid.
 * Works alongside MediaThumbnail's onClick/onLongClick by observing PointerEventPass.Initial:
 * - Normal taps and scrolls pass through untouched.
 * - Holding a thumbnail past longPressTimeoutMillis and sliding your finger selects/deselects
 *   a continuous range of items with automatic edge scrolling.
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
        val touchSlop = viewConfiguration.touchSlop
        val longPressTimeoutMs = viewConfiguration.longPressTimeoutMillis

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

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val downPos = down.position
            val downTime = System.currentTimeMillis()
            val idsAtDown = currentOrderedIds
            val anchorIndex = findItemIndexAt(downPos, idsAtDown)
            if (anchorIndex < 0) return@awaitEachGesture

            val anchorId = idsAtDown[anchorIndex]
            val initialSelection = currentSelectedIds
            val targetSelect = !initialSelection.contains(anchorId)
            var dragSelectActive = false
            var movedTooEarly = false

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pointer = event.changes.firstOrNull { it.id == down.id } ?: break

                if (!pointer.pressed || event.changes.count { it.pressed } > 1) {
                    break
                }

                val elapsed = System.currentTimeMillis() - downTime
                val distance = (pointer.position - downPos).getDistance()

                if (!dragSelectActive) {
                    if (distance > touchSlop && elapsed < longPressTimeoutMs) {
                        // Normal scroll gesture — let LazyVerticalGrid handle it
                        movedTooEarly = true
                        break
                    }
                    if (elapsed >= longPressTimeoutMs && distance > touchSlop && !movedTooEarly) {
                        dragSelectActive = true
                        isDragging = true
                        autoScrollSpeed = 0f
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnSelectionChanged(
                            applyRange(initialSelection, idsAtDown, anchorIndex, anchorIndex, targetSelect)
                        )
                    }
                }

                if (dragSelectActive) {
                    pointer.consume()

                    val viewportHeight = gridState.layoutInfo.viewportSize.height.toFloat()
                    val edgeThreshold = 110f
                    autoScrollSpeed = when {
                        pointer.position.y < edgeThreshold -> {
                            val factor = ((edgeThreshold - pointer.position.y) / edgeThreshold).coerceIn(0f, 1f)
                            -24f * factor
                        }
                        pointer.position.y > viewportHeight - edgeThreshold -> {
                            val factor = ((pointer.position.y - (viewportHeight - edgeThreshold)) / edgeThreshold).coerceIn(0f, 1f)
                            24f * factor
                        }
                        else -> 0f
                    }

                    val ids = currentOrderedIds
                    val hitIdx = findItemIndexAt(pointer.position, ids)
                    if (hitIdx >= 0) {
                        currentOnSelectionChanged(
                            applyRange(initialSelection, ids, anchorIndex, hitIdx, targetSelect)
                        )
                    }
                }
            }

            isDragging = false
            autoScrollSpeed = 0f
        }
    }
}
