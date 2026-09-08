package com.example.healthjournal.health

import com.example.healthjournal.data.local.WorkoutSession
import com.example.healthjournal.domain.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for the session -> Health Connect record mapping: every
 * workout type maps to its exercise counterpart, timestamps bound the
 * record, and optional metrics pass through untouched.
 */
class WorkoutHealthMapperTest {

    @Test
    fun allTypes_mapToExerciseCounterparts() {
        assertEquals(
            HealthExerciseType.RUNNING,
            WorkoutSession(type = WorkoutType.RUN.name).toHealthRecord(now = 2_000L).exerciseType
        )
        assertEquals(
            HealthExerciseType.STRENGTH_TRAINING,
            WorkoutSession(type = WorkoutType.FITNESS.name).toHealthRecord(now = 2_000L).exerciseType
        )
        assertEquals(
            HealthExerciseType.YOGA,
            WorkoutSession(type = WorkoutType.YOGA.name).toHealthRecord(now = 2_000L).exerciseType
        )
    }

    @Test
    fun finishedSession_boundsRecordByStartAndEnd() {
        val session = WorkoutSession(
            type = WorkoutType.RUN.name,
            startTimestamp = 1_000L,
            endTimestamp = 1_600L
        )

        val record = session.toHealthRecord(now = 9_999L)

        assertEquals(1_000L, record.startTimeMillis)
        assertEquals(1_600L, record.endTimeMillis)
    }

    @Test
    fun unfinishedSession_endsRecordAtNow() {
        val session = WorkoutSession(
            type = WorkoutType.FITNESS.name,
            startTimestamp = 1_000L,
            endTimestamp = null
        )

        val record = session.toHealthRecord(now = 2_000L)

        assertEquals(2_000L, record.endTimeMillis)
    }

    @Test
    fun optionalMetrics_passThrough() {
        val session = WorkoutSession(
            type = WorkoutType.RUN.name,
            calories = 343.0,
            targetDistanceM = 5_000.0
        )

        val record = session.toHealthRecord(now = 2_000L)

        assertEquals(343.0, record.caloriesKcal!!, 0.0)
        assertEquals(5_000.0, record.distanceMeters!!, 0.0)
    }

    @Test
    fun missingMetrics_stayNull() {
        val record = WorkoutSession(type = WorkoutType.YOGA.name).toHealthRecord(now = 2_000L)

        assertNull(record.caloriesKcal)
        assertNull(record.distanceMeters)
    }
}
