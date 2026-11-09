package com.example.healthjournal.domain

import kotlin.math.abs
import kotlin.math.hypot

enum class GestureAxis {
    UNDETERMINED,
    HORIZONTAL,
    VERTICAL,
    BLOCKED
}

data class SwipeReleaseResult(
    val shouldCommit: Boolean,
    val targetOffsetPx: Float
)

class SwipeArchivePolicy(
    val cardWidthPx: Float,
    val commitThresholdFraction: Float = 0.5f
) {
    private val thresholdDistancePx: Float
        get() = cardWidthPx * commitThresholdFraction

    fun clampOffset(dragOffsetPx: Float, isRtl: Boolean): Float {
        return if (!isRtl) {
            // LTR: archive is end-to-start (negative offset)
            if (dragOffsetPx > 0f) 0f else dragOffsetPx.coerceAtLeast(-cardWidthPx)
        } else {
            // RTL: archive is end-to-start (positive offset)
            if (dragOffsetPx < 0f) 0f else dragOffsetPx.coerceAtMost(cardWidthPx)
        }
    }

    fun isArmed(currentOffsetPx: Float, isRtl: Boolean = false): Boolean {
        val effectiveDistance = if (!isRtl) -currentOffsetPx else currentOffsetPx
        return effectiveDistance >= thresholdDistancePx && thresholdDistancePx > 0f
    }

    fun hasCrossedThreshold(
        previousOffsetPx: Float,
        currentOffsetPx: Float,
        isRtl: Boolean = false
    ): Boolean {
        val wasArmed = isArmed(previousOffsetPx, isRtl)
        val isNowArmed = isArmed(currentOffsetPx, isRtl)
        return wasArmed != isNowArmed
    }

    fun onRelease(
        dragOffsetPx: Float,
        velocityPxPerSec: Float,
        isRtl: Boolean = false
    ): SwipeReleaseResult {
        val effectiveDistance = if (!isRtl) -dragOffsetPx else dragOffsetPx

        // Fling velocity alone never commits unless the 50% distance threshold is crossed
        val shouldCommit = effectiveDistance >= thresholdDistancePx && thresholdDistancePx > 0f

        val targetOffsetPx = if (shouldCommit) {
            if (!isRtl) -cardWidthPx else cardWidthPx
        } else {
            0f
        }

        return SwipeReleaseResult(
            shouldCommit = shouldCommit,
            targetOffsetPx = targetOffsetPx
        )
    }
}

class SwipeAxisLockTracker(
    val touchSlopPx: Float,
    val dominantAxisRatio: Float = 2.0f
) {
    var axis: GestureAxis = GestureAxis.UNDETERMINED
        private set

    fun onDown(isListScrollInProgress: Boolean) {
        axis = if (isListScrollInProgress) {
            GestureAxis.BLOCKED
        } else {
            GestureAxis.UNDETERMINED
        }
    }

    fun update(dx: Float, dy: Float): GestureAxis {
        if (axis != GestureAxis.UNDETERMINED) {
            return axis
        }

        val totalDistance = hypot(dx, dy)
        if (totalDistance < touchSlopPx) {
            return GestureAxis.UNDETERMINED
        }

        val absDx = abs(dx)
        val absDy = abs(dy)

        axis = if (absDx >= dominantAxisRatio * absDy) {
            GestureAxis.HORIZONTAL
        } else {
            GestureAxis.VERTICAL
        }

        return axis
    }

    fun reset() {
        axis = GestureAxis.UNDETERMINED
    }
}
