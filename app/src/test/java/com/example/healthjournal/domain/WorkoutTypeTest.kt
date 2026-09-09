package com.example.healthjournal.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the expanded WorkoutType catalog: v2 grows the activity
 * set from 3 to 10 types, each carrying a label, a MET factor for calorie
 * estimation, and a per-type target kind that drives the configuration UX.
 */
class WorkoutTypeTest {

    @Test
    fun catalog_hasTenTypes() {
        assertEquals(10, WorkoutType.entries.size)
    }

    @Test
    fun labels_areUserFacingPerActivity() {
        assertEquals("Run", WorkoutType.RUN.label)
        assertEquals("Fitness", WorkoutType.FITNESS.label)
        assertEquals("Yoga", WorkoutType.YOGA.label)
        assertEquals("HIIT", WorkoutType.HIIT.label)
        assertEquals("Walking/Hiking", WorkoutType.WALKING_HIKING.label)
        assertEquals("Cycling", WorkoutType.CYCLING.label)
        assertEquals("Stretching", WorkoutType.STRETCHING_MOBILITY.label)
        assertEquals("Pilates", WorkoutType.PILATES.label)
        assertEquals("Swimming", WorkoutType.SWIMMING.label)
        assertEquals("Calisthenics", WorkoutType.CALISTHENICS.label)
    }

    @Test
    fun targetKinds_matchPerTypeSemantics() {
        assertEquals(WorkoutTargetKind.DISTANCE_KM, WorkoutType.RUN.targetKind)
        assertEquals(WorkoutTargetKind.DURATION_OR_SET_MATRIX, WorkoutType.FITNESS.targetKind)
        assertEquals(WorkoutTargetKind.DURATION_MIN, WorkoutType.YOGA.targetKind)
        assertEquals(WorkoutTargetKind.MANUAL_INTERVALS, WorkoutType.HIIT.targetKind)
        assertEquals(WorkoutTargetKind.DURATION_OR_DISTANCE, WorkoutType.WALKING_HIKING.targetKind)
        assertEquals(WorkoutTargetKind.DURATION_OR_DISTANCE, WorkoutType.CYCLING.targetKind)
        assertEquals(WorkoutTargetKind.DURATION_MIN, WorkoutType.STRETCHING_MOBILITY.targetKind)
        assertEquals(WorkoutTargetKind.DURATION_MIN, WorkoutType.PILATES.targetKind)
        assertEquals(WorkoutTargetKind.DURATION_WITH_LAPS, WorkoutType.SWIMMING.targetKind)
        assertEquals(WorkoutTargetKind.DURATION_WITH_MOVEMENTS, WorkoutType.CALISTHENICS.targetKind)
    }

    @Test
    fun typesWithDistanceTargets_areDistanceFriendly() {
        assertEquals(
            setOf(
                WorkoutType.RUN,
                WorkoutType.WALKING_HIKING,
                WorkoutType.CYCLING
            ),
            WorkoutType.entries.filter { it.targetKind.supportsDistance }.toSet()
        )
    }
}