package com.example.healthjournal.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeArchivePolicyTest {

    @Test
    fun `releasing below 50 percent width does not commit archive`() {
        val policy = SwipeArchivePolicy(cardWidthPx = 400f)
        
        // Dragged 199px (less than 200px / 50%)
        val result = policy.onRelease(dragOffsetPx = -199f, velocityPxPerSec = 0f)
        
        assertFalse(result.shouldCommit)
        assertEquals(0f, result.targetOffsetPx, 0.01f)
    }

    @Test
    fun `releasing at or above 50 percent width commits archive`() {
        val policy = SwipeArchivePolicy(cardWidthPx = 400f)
        
        // Dragged 200px (exactly 50%)
        val result50 = policy.onRelease(dragOffsetPx = -200f, velocityPxPerSec = 0f)
        assertTrue(result50.shouldCommit)
        assertEquals(-400f, result50.targetOffsetPx, 0.01f)

        // Dragged 300px (> 50%)
        val result75 = policy.onRelease(dragOffsetPx = -300f, velocityPxPerSec = 0f)
        assertTrue(result75.shouldCommit)
        assertEquals(-400f, result75.targetOffsetPx, 0.01f)
    }

    @Test
    fun `fling velocity alone never commits if distance is below 50 percent`() {
        val policy = SwipeArchivePolicy(cardWidthPx = 400f)
        
        // Fast flick (-2500 px/s) but only 100px dragged
        val result = policy.onRelease(dragOffsetPx = -100f, velocityPxPerSec = -2500f)
        
        assertFalse(result.shouldCommit)
        assertEquals(0f, result.targetOffsetPx, 0.01f)
    }

    @Test
    fun `fling velocity commits if distance is at or above 50 percent`() {
        val policy = SwipeArchivePolicy(cardWidthPx = 400f)
        
        val result = policy.onRelease(dragOffsetPx = -210f, velocityPxPerSec = -2000f)
        
        assertTrue(result.shouldCommit)
        assertEquals(-400f, result.targetOffsetPx, 0.01f)
    }

    @Test
    fun `start to end right drag is clamped and never commits`() {
        val policy = SwipeArchivePolicy(cardWidthPx = 400f)
        
        val clampedOffset = policy.clampOffset(dragOffsetPx = 150f, isRtl = false)
        assertEquals(0f, clampedOffset, 0.01f)
        
        val result = policy.onRelease(dragOffsetPx = 150f, velocityPxPerSec = 1000f)
        assertFalse(result.shouldCommit)
        assertEquals(0f, result.targetOffsetPx, 0.01f)
    }

    @Test
    fun `RTL end to start drag direction handling`() {
        val policy = SwipeArchivePolicy(cardWidthPx = 400f)
        
        // In RTL, end-to-start is dragging right (positive delta)
        val clampedLtrWrongWay = policy.clampOffset(dragOffsetPx = 100f, isRtl = false)
        assertEquals(0f, clampedLtrWrongWay, 0.01f)

        val clampedRtlCorrect = policy.clampOffset(dragOffsetPx = 100f, isRtl = true)
        assertEquals(100f, clampedRtlCorrect, 0.01f)

        val clampedRtlWrongWay = policy.clampOffset(dragOffsetPx = -100f, isRtl = true)
        assertEquals(0f, clampedRtlWrongWay, 0.01f)

        // In RTL, 50% positive drag commits
        val result = policy.onRelease(dragOffsetPx = 200f, velocityPxPerSec = 0f, isRtl = true)
        assertTrue(result.shouldCommit)
        assertEquals(400f, result.targetOffsetPx, 0.01f)
    }

    @Test
    fun `dominant axis lock requires horizontal delta at least 2x vertical delta`() {
        val tracker = SwipeAxisLockTracker(touchSlopPx = 10f)

        // Touch slop not yet exceeded
        assertEquals(GestureAxis.UNDETERMINED, tracker.update(dx = 5f, dy = 2f))

        // Exceeded touch slop, dx is 15, dy is 5 (|dx| >= 2 * |dy| -> 15 >= 10: HORIZONTAL)
        val horizontalTracker = SwipeAxisLockTracker(touchSlopPx = 10f)
        assertEquals(GestureAxis.HORIZONTAL, horizontalTracker.update(dx = 15f, dy = 5f))

        // Exceeded touch slop, dx is 10, dy is 8 (|dx| < 2 * |dy| -> 10 < 16: VERTICAL lock)
        val verticalTracker = SwipeAxisLockTracker(touchSlopPx = 10f)
        assertEquals(GestureAxis.VERTICAL, verticalTracker.update(dx = 10f, dy = 8f))

        // Once locked to VERTICAL, even a sudden horizontal move keeps it VERTICAL until reset
        assertEquals(GestureAxis.VERTICAL, verticalTracker.update(dx = 50f, dy = 0f))
        verticalTracker.reset()
        assertEquals(GestureAxis.UNDETERMINED, verticalTracker.axis)
    }

    @Test
    fun `scroll in progress blocks swipe initiation entirely`() {
        val tracker = SwipeAxisLockTracker(touchSlopPx = 10f)
        tracker.onDown(isListScrollInProgress = true)

        // Should lock to BLOCKED/VERTICAL immediately or reject horizontal
        val axis = tracker.update(dx = 30f, dy = 0f)
        assertEquals(GestureAxis.BLOCKED, axis)
    }

    @Test
    fun `threshold armed state and tick detection`() {
        val policy = SwipeArchivePolicy(cardWidthPx = 400f)

        // Start at 0: unarmed
        assertFalse(policy.isArmed(currentOffsetPx = 0f))

        // Move to 150px (37.5%): unarmed
        assertFalse(policy.isArmed(currentOffsetPx = -150f))

        // Move to 200px (50%): armed
        assertTrue(policy.isArmed(currentOffsetPx = -200f))

        // Move to 250px (62.5%): armed
        assertTrue(policy.isArmed(currentOffsetPx = -250f))

        // Crossing detection: unarmed -> armed should return true for haptic tick
        assertTrue(policy.hasCrossedThreshold(previousOffsetPx = -150f, currentOffsetPx = -200f))

        // Staying above threshold should not re-trigger tick
        assertFalse(policy.hasCrossedThreshold(previousOffsetPx = -200f, currentOffsetPx = -250f))

        // Moving back below threshold (armed -> unarmed) should trigger tick
        assertTrue(policy.hasCrossedThreshold(previousOffsetPx = -210f, currentOffsetPx = -190f))

        // Staying below threshold should not re-trigger tick
        assertFalse(policy.hasCrossedThreshold(previousOffsetPx = -190f, currentOffsetPx = -100f))
    }
}
