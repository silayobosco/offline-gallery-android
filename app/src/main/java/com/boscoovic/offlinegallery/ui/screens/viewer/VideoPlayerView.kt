package com.boscoovic.offlinegallery.ui.screens.viewer

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.boscoovic.offlinegallery.data.model.MediaItem
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    item: MediaItem,
    isActive: Boolean,
    playPauseToggleTrigger: Long,
    seekPositionRequest: Long?,
    onPlayerStateUpdated: (isPlaying: Boolean, currentPositionMs: Long, durationMs: Long) -> Unit,
    onTap: () -> Unit,
    onDoubleTapLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    // TikTok Speed controls: 2x Speed & Slide-down-to-Lock
    var isSpeedUpActive by remember { mutableStateOf(false) }
    var isSpeedLocked by remember { mutableStateOf(false) }
    var slideDownProgress by remember { mutableFloatStateOf(0f) }

    val exoPlayer = remember(item.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(Media3Item.fromUri(item.uri))
            prepare()
            playWhenReady = false
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    // Handle Active Page playback
    LaunchedEffect(isActive) {
        if (isActive) {
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        } else {
            exoPlayer.playWhenReady = false
            exoPlayer.pause()
            isSpeedUpActive = false
            isSpeedLocked = false
            exoPlayer.setPlaybackSpeed(1.0f)
        }
        onPlayerStateUpdated(
            exoPlayer.isPlaying,
            exoPlayer.currentPosition.coerceAtLeast(0L),
            exoPlayer.duration.coerceAtLeast(0L)
        )
    }

    // Handle Play / Pause toggle triggered strictly from the right action button
    LaunchedEffect(playPauseToggleTrigger) {
        if (playPauseToggleTrigger > 0L && isActive) {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            } else {
                exoPlayer.play()
            }
            onPlayerStateUpdated(
                exoPlayer.isPlaying,
                exoPlayer.currentPosition.coerceAtLeast(0L),
                exoPlayer.duration.coerceAtLeast(0L)
            )
        }
    }

    // Handle Seek position request triggered from the scrub bar
    LaunchedEffect(seekPositionRequest) {
        seekPositionRequest?.let { targetMs ->
            exoPlayer.seekTo(targetMs)
            onPlayerStateUpdated(
                exoPlayer.isPlaying,
                exoPlayer.currentPosition.coerceAtLeast(0L),
                exoPlayer.duration.coerceAtLeast(0L)
            )
        }
    }

    // Periodic position updates to parent screen (10 fps)
    LaunchedEffect(isActive, isPlaying) {
        while (isActive) {
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
            duration = exoPlayer.duration.coerceAtLeast(0L)
            onPlayerStateUpdated(exoPlayer.isPlaying, currentPosition, duration)
            delay(100L)
        }
    }

    // Stop sound when switching apps / activity in background
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                onPlayerStateUpdated(
                    playing,
                    exoPlayer.currentPosition.coerceAtLeast(0L),
                    exoPlayer.duration.coerceAtLeast(0L)
                )
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                    onPlayerStateUpdated(
                        exoPlayer.isPlaying,
                        exoPlayer.currentPosition.coerceAtLeast(0L),
                        duration
                    )
                }
            }
        }
        exoPlayer.addListener(listener)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    exoPlayer.playWhenReady = false
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isActive) {
                        exoPlayer.playWhenReady = true
                        exoPlayer.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.removeListener(listener)
            exoPlayer.pause()
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video Viewport (PlayerView touch events disabled so Compose captures all touches reliably)
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    isClickable = false
                    isFocusable = false
                    setOnTouchListener { _, _ -> false }
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Unified TikTok Gesture Surface:
        // - Hold (>200ms): 2x Speed (intercepts in PointerEventPass.Initial with consume() so VerticalPager cannot steal gesture)
        // - Slide down while holding: locks 2x speed
        // - Quick tap: toggles UI chrome (or resets locked speed if active) - DOES NOT toggle play/pause!
        // - Double tap: like heart animation
        VideoGestureSurface(
            isSpeedLocked = isSpeedLocked,
            onSpeedStart = {
                if (!isSpeedLocked) {
                    isSpeedUpActive = true
                    exoPlayer.setPlaybackSpeed(2.0f)
                }
            },
            onSlideProgress = { progress ->
                slideDownProgress = progress
            },
            onLockTriggered = {
                isSpeedLocked = true
                isSpeedUpActive = true
                exoPlayer.setPlaybackSpeed(2.0f)
            },
            onSpeedEnd = {
                if (!isSpeedLocked) {
                    isSpeedUpActive = false
                    exoPlayer.setPlaybackSpeed(1.0f)
                }
                slideDownProgress = 0f
            },
            onTap = {
                if (isSpeedLocked) {
                    isSpeedLocked = false
                    isSpeedUpActive = false
                    exoPlayer.setPlaybackSpeed(1.0f)
                } else {
                    onTap()
                }
            },
            onDoubleTap = onDoubleTapLike,
            modifier = Modifier.fillMaxSize()
        )

        // TikTok Floating Speed Pill (Top Center)
        AnimatedVisibility(
            visible = (isSpeedUpActive || isSpeedLocked) && isActive,
            enter = fadeIn(tween(150)) + slideInVertically(initialOffsetY = { -60 }),
            exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { -60 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.70f))
                    .clickable {
                        // Tap pill to reset speed
                        isSpeedLocked = false
                        isSpeedUpActive = false
                        exoPlayer.setPlaybackSpeed(1.0f)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isSpeedLocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Speed Locked",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "2x Speed Locked",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Reset speed",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "2x Speed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (slideDownProgress > 0.1f) "Slide down to lock 🔒" else "2x Speed",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * TikTok Gesture Surface:
 * - Detects hold (>200ms) with minimal movement to activate 2x playback speed.
 *   Uses PointerEventPass.Initial and calls pointer.consume() to prevent VerticalPager from stealing the touch.
 * - Detects vertical downward slide during hold to lock 2x speed.
 * - Detects single tap (toggles UI chrome or resets locked speed; does NOT play/pause).
 * - Detects double tap (triggers like animation).
 * - Lets vertical/horizontal swipes pass through unconsumed if rapid movement occurs before 200ms.
 */
@Composable
private fun VideoGestureSurface(
    isSpeedLocked: Boolean,
    onSpeedStart: () -> Unit,
    onSlideProgress: (Float) -> Unit,
    onLockTriggered: () -> Unit,
    onSpeedEnd: () -> Unit,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var lastTapTime by remember { mutableLongStateOf(0L) }

    Box(
        modifier = modifier.pointerInput(isSpeedLocked) {
            awaitEachGesture {
                val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                val downTime = System.currentTimeMillis()
                val startX = down.position.x
                val startY = down.position.y
                val pointerId = down.id
                var isSpeeding = false
                var isLocked = false
                var cancelled = false

                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val pointer = event.changes.firstOrNull { it.id == pointerId } ?: break

                    if (!pointer.pressed) {
                        // Pointer released
                        val elapsed = System.currentTimeMillis() - downTime
                        val dx = pointer.position.x - startX
                        val dy = pointer.position.y - startY
                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)

                        if (!isSpeeding && !cancelled && elapsed < 250L && dist < 30f) {
                            pointer.consume()
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime < 320L) {
                                onDoubleTap()
                                lastTapTime = 0L
                            } else {
                                lastTapTime = now
                                onTap()
                            }
                        }
                        break
                    }

                    val elapsed = System.currentTimeMillis() - downTime
                    val dx = pointer.position.x - startX
                    val dy = pointer.position.y - startY
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)

                    if (!isSpeeding) {
                        // Rapid movement before 200ms means intentional swipe (page scroll or action swipe)
                        if (dist > 35f && elapsed < 200L) {
                            cancelled = true
                            break
                        }

                        // Hold threshold reached: start 2x speed!
                        if (elapsed >= 200L && !cancelled) {
                            isSpeeding = true
                            pointer.consume()
                            onSpeedStart()
                        }
                    } else {
                        // Consume all events in Initial pass so parent VerticalPager cannot intercept
                        pointer.consume()
                        val lockThresholdPx = 120f
                        val progress = (dy / lockThresholdPx).coerceIn(0f, 1f)
                        onSlideProgress(progress)

                        if (dy >= lockThresholdPx && !isLocked) {
                            isLocked = true
                            onLockTriggered()
                        }
                    }
                }

                if (isSpeeding) {
                    onSpeedEnd()
                }
            }
        }
    )
}

/**
 * TikTok-style sleek scrub bar:
 * - Idle: 3dp slim line with small thumb dot.
 * - Scrubbing: expands to 6dp height, larger thumb, with live time pill floating above.
 */
@Composable
internal fun TikTokProgressBar(
    progressFraction: Float,
    scrubTimeMs: Long,
    totalDurationMs: Long,
    isScrubbing: Boolean,
    onScrubStart: () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barHeight by animateDpAsState(
        targetValue = if (isScrubbing) 6.dp else 3.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "tiktokBarHeight"
    )
    val thumbSize by animateDpAsState(
        targetValue = if (isScrubbing) 12.dp else 6.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tiktokThumbSize"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isScrubbing) 44.dp else 20.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onScrubStart()
                        val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onScrub(frac)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onScrub(frac)
                    },
                    onDragEnd = { onScrubEnd() },
                    onDragCancel = { onScrubEnd() }
                )
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val thumbOffsetPx = (progressFraction * totalWidthPx).coerceIn(0f, totalWidthPx)

        // Floating scrub time indicator above thumb
        if (isScrubbing) {
            val pillOffset = (thumbOffsetPx - 45f).coerceIn(12f, (totalWidthPx - 100f).coerceAtLeast(12f))
            Box(
                modifier = Modifier
                    .offset { IntOffset(pillOffset.roundToInt(), -18) }
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${formatVideoTime(scrubTimeMs)} / ${formatVideoTime(totalDurationMs)}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Track Background + Played Progress
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(Color.White.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progressFraction)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.95f))
            )
        }

        // Scrubber Thumb Dot
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset {
                    IntOffset(
                        x = (thumbOffsetPx - (thumbSize.value * 1.5f)).roundToInt().coerceAtLeast(0),
                        y = 0
                    )
                }
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

internal fun formatVideoTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
}
