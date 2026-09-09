package com.example.healthjournal.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the manual-interval HIIT domain logic: there is no fixed
 * plan, the user advances between work/rest phases by hand, and every
 * boundary crossing emits a haptic cue. A round completes each time the
 * user finishes a WORK interval and crosses into REST.
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
        val advance = WorkoutIntervalSession().advance()

        assertEquals(WorkoutIntervalPhase.REST, advance.session.phase)
        assertEquals(1, advance.session.rounds)
        assertEquals(1, advance.session.intervals)
        assertEquals(WorkoutIntervalCue.REST_START, advance.cue)
    }

    @Test
    fun advanceFromRest_returnsToWorkKeepingRoundCount() {
        val advance = WorkoutIntervalSession(
            phase = WorkoutIntervalPhase.REST,
            rounds = 1,
            intervals = 1
        ).advance()

        assertEquals(WorkoutIntervalPhase.WORK, advance.session.phase)
        assertEquals(1, advance.session.rounds)
        assertEquals(2, advance.session.intervals)
        assertEquals(WorkoutIntervalCue.WORK_START, advance.cue)
    }

    @Test
    fun alternatingAdvances_accumulateRoundsAndIntervalsCorrectly() {
        var session = WorkoutIntervalSession()
        repeat(4) { session = session.advance().session }

        // WORK -> REST(round 1) -> WORK -> REST(round 2) -> WORK
        assertEquals(WorkoutIntervalPhase.WORK, session.phase)
        assertEquals(2, session.rounds)
        assertEquals(4, session.intervals)
    }

    @Test
    fun everyAdvance_emitsAHapticCue() {
        var session = WorkoutIntervalSession()
        val cues = mutableListOf<WorkoutIntervalCue>()
        repeat(6) {
            val advance = session.advance()
            cues += advance.cue
            session = advance.session
        }

        assertEquals(
            listOf(
                WorkoutIntervalCue.REST_START,
                WorkoutIntervalCue.WORK_START,
                WorkoutIntervalCue.REST_START,
                WorkoutIntervalCue.WORK_START,
                WorkoutIntervalCue.REST_START,
                WorkoutIntervalCue.WORK_START
            ),
            cues
        )
        assertEquals(3, session.rounds)
        assertEquals(6, session.intervals)
    }
}