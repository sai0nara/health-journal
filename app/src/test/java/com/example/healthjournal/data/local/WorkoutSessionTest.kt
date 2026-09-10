package com.example.healthjournal.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the WorkoutSession entity defaults: a fresh session is an
 * unfinished active record with generated identity and timestamps.
 */
class WorkoutSessionTest {

    @Test
    fun freshSession_hasActiveDefaults() {
        val session = WorkoutSession()

        assertTrue(session.session_id.isNotBlank())
        assertEquals(WorkoutStatus.ACTIVE.name, session.status)
        assertNull(session.endTimestamp)
        assertEquals(0L, session.elapsedSeconds)
        assertNull("Fresh sessions have no set matrix", session.setMatrix)
    }

    @Test
    fun freshSession_timestampsDefaultToNow() {
        val before = System.currentTimeMillis()
        val session = WorkoutSession()
        val after = System.currentTimeMillis()

        assertTrue(session.startTimestamp in before..after)
        assertEquals(session.startTimestamp, session.lastModified)
    }
}
