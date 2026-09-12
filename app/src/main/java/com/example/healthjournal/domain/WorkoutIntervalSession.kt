package com.example.healthjournal.domain

/** Manual-interval phases of an HIIT session; phases alternate on each advance. */
enum class WorkoutIntervalPhase {
    WORK,
    REST
}

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

    /** Crosses one boundary and returns the new session state. */
    fun advance(): WorkoutIntervalSession = when (phase) {
        WorkoutIntervalPhase.WORK -> copy(
            phase = WorkoutIntervalPhase.REST,
            rounds = rounds + 1,
            intervals = intervals + 1
        )
        WorkoutIntervalPhase.REST -> copy(
            phase = WorkoutIntervalPhase.WORK,
            intervals = intervals + 1
        )
    }
}
