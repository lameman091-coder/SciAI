package com.funtime.sciai.aisphere

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange

/**
 * Gesture detection system for the AI Sphere.
 * Handles tap, long-press, drag, petting, rapid-tap, and double-tap detection.
 */
object InteractionHandler {

    fun Modifier.sphereTapGestures(
        onTap: () -> Unit,
        onLongPress: () -> Unit
    ): Modifier = this.pointerInput(Unit) {
        detectTapGestures(
            onTap = { onTap() },
            onLongPress = { onLongPress() }
        )
    }

    fun Modifier.sphereDragGestures(
        onDragStart: () -> Unit = {},
        onDrag: (Offset) -> Unit,
        onDragEnd: () -> Unit = {}
    ): Modifier = this.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { onDragStart() },
            onDrag = { change: PointerInputChange, dragAmount: Offset ->
                change.consume()
                onDrag(dragAmount)
            },
            onDragEnd = { onDragEnd() }
        )
    }
}

/**
 * Petting gesture detector.
 * Tracks repeated horizontal swipes across the sphere to detect "petting".
 *
 * Algorithm:
 * 1. Track pointer movement direction changes
 * 2. Count directional swipes (horizontal movement > threshold)
 * 3. If ≥3 swipes within time window → emit pet event
 */
class PetDetector(
    private val swipeThresholdDp: Float = 15f,
    private val requiredSwipes: Int = 3,
    private val timeWindowMs: Long = 2000L
) {
    private val swipeTimes = mutableListOf<Long>()
    private var lastDirection: Int = 0
    private var accumulatedX: Float = 0f

    fun onDrag(deltaX: Float): Boolean {
        accumulatedX += deltaX
        val currentDirection = if (accumulatedX > swipeThresholdDp) 1
        else if (accumulatedX < -swipeThresholdDp) -1
        else return false

        if (currentDirection != lastDirection && lastDirection != 0) {
            val now = System.currentTimeMillis()
            swipeTimes.add(now)
            swipeTimes.removeAll { now - it > timeWindowMs }
            accumulatedX = 0f
            lastDirection = currentDirection

            if (swipeTimes.size >= requiredSwipes) {
                swipeTimes.clear()
                return true
            }
        } else if (lastDirection == 0) {
            lastDirection = currentDirection
            accumulatedX = 0f
        }
        return false
    }

    fun reset() {
        swipeTimes.clear()
        lastDirection = 0
        accumulatedX = 0f
    }
}

/**
 * Rapid tap detector — detects "angry" tapping (>3 taps in 2 seconds).
 * When too many taps happen too fast, the sphere gets annoyed!
 */
class RapidTapDetector(
    private val requiredTaps: Int = 4,
    private val timeWindowMs: Long = 2000L
) {
    private val tapTimes = mutableListOf<Long>()

    /**
     * Record a tap. Returns true if rapid tapping detected.
     */
    fun onTap(): Boolean {
        val now = System.currentTimeMillis()
        tapTimes.add(now)
        tapTimes.removeAll { now - it > timeWindowMs }

        if (tapTimes.size >= requiredTaps) {
            tapTimes.clear()
            return true  // Rapid tap detected! Sphere is annoyed
        }
        return false
    }

    fun reset() {
        tapTimes.clear()
    }
}

/**
 * Double-tap detector — detects two quick taps → shy reaction.
 * Two taps within 350ms triggers a cute shy response.
 */
class DoubleTapDetector(
    private val maxIntervalMs: Long = 350L
) {
    private var lastTapTime: Long = 0L

    /**
     * Record a tap. Returns true if double-tap detected.
     */
    fun onTap(): Boolean {
        val now = System.currentTimeMillis()
        val isDoubleTap = (now - lastTapTime) < maxIntervalMs
        lastTapTime = now

        if (isDoubleTap) {
            lastTapTime = 0L  // Reset to prevent triple-tap counting
            return true
        }
        return false
    }

    fun reset() {
        lastTapTime = 0L
    }
}
