package com.example.healthjournal.domain

/** Manual-interval phases of an HIIT session; phases alternate on each advance. */
enum class WorkoutIntervalPhase {
    WORK,
    REST
}

/** Haptic cue to play when an interval boundary is crossed. */
enum class WorkoutIntervalCue {
    REST_START,
    WORK_START
}

/** Result of crossing one interval boundary: the new session state plus the cue to play. */
data class WorkoutIntervalAdvance(
    val session: WorkoutIntervalSession,
    val cue: WorkoutIntervalCue
)

/**
 * Pure manual-interval tracker for HIIT: there is no fixed plan, the user taps
 * "next interval" to move between WORK and REST, and each crossing counts as one
 * interval. A round completes whenever the user leaves WORK into REST; returning
 * to WORK keeps the round count so rounds equal completed work intervals.
 */
data class WorkoutIntervalSession(
    val phase: WorkoutIntervalPhase = WorkoutIntervalPhase.WORK,
    val rounds: Int = 0,
    val intervals: Int = 0
) {

    /** Crosses one boundary, returns the new state and the matching haptic cue. */
    fun advance(): WorkoutIntervalAdvance = when (phase) {
        WorkoutIntervalPhase.WORK -> WorkoutIntervalAdvance(
            session = copy(
                phase = WorkoutIntervalPhase.REST,
                rounds = rounds + 1,
                intervals = intervals + 1
            ),
            cue = WorkoutIntervalCue.REST_START
        )
        WorkoutIntervalPhase.REST -> WorkoutIntervalAdvance(
            session = copy(
                phase = WorkoutIntervalPhase.WORK,
                intervals = intervals + 1
            ),
            cue = WorkoutIntervalCue.WORK_START
        )
    }
}