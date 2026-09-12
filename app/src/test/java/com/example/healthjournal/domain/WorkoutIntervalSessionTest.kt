package com.example.healthjournal.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the manual-interval HIIT domain logic: there is no fixed
 * plan, the user advances between work/rest phases by hand, and every
 * crossing counts as one interval. A round completes each time the user
 * finishes a WORK interval and crosses into REST.
 */
class WorkoutIntervalSessionTest {

    @Test
    fun initialSession_isWorkWithNoRoundsOrIntervals() {
        val session = WorkoutIntervalSession()

        assertEquals(WorkoutIntervalPhase.WORK, session.phase)
        assertEquals(0, session.rounds)
        assertEquals(0, session.intervals)
    }

    @Test
    fun advanceFromWork_startsRestAndCompletesRound() {
        val advanced = WorkoutIntervalSession().advance()

        assertEquals(WorkoutIntervalPhase.REST, advanced.phase)
        assertEquals(1, advanced.rounds)
        assertEquals(1, advanced.intervals)
    }

    @Test
    fun advanceFromRest_returnsToWorkKeepingRoundCount() {
        val advanced = WorkoutIntervalSession(
            phase = WorkoutIntervalPhase.REST,
            rounds = 1,
            intervals = 1
        ).advance()

        assertEquals(WorkoutIntervalPhase.WORK, advanced.phase)
        assertEquals(1, advanced.rounds)
        assertEquals(2, advanced.intervals)
    }

    @Test
    fun alternatingAdvances_accumulateRoundsAndIntervalsCorrectly() {
        var session = WorkoutIntervalSession()
        repeat(4) { session = session.advance() }

        // WORK -> REST(round 1) -> WORK -> REST(round 2) -> WORK
        assertEquals(WorkoutIntervalPhase.WORK, session.phase)
        assertEquals(2, session.rounds)
        assertEquals(4, session.intervals)
    }

    @Test
    fun sixAdvances_accumulateThreeRoundsAndSixIntervals() {
        var session = WorkoutIntervalSession()
        repeat(6) { session = session.advance() }

        assertEquals(WorkoutIntervalPhase.WORK, session.phase)
        assertEquals(3, session.rounds)
        assertEquals(6, session.intervals)
    }
}