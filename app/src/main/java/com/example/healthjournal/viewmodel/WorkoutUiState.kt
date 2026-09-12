package com.example.healthjournal.viewmodel

import com.example.healthjournal.data.local.WorkoutSession
import com.example.healthjournal.domain.WorkoutType

/**
 * MVI states for the workout flow. Every screen renders exactly one of
 * these; all transitions run through [WorkoutViewModel].
 */
sealed interface WorkoutUiState {

    data object Idle : WorkoutUiState

    data class Configuring(
        val type: WorkoutType,
        val target: String = "",
        val targetError: String? = null
    ) : WorkoutUiState

    /** Immutable snapshot used while the pre-session 3-2-1 countdown runs. */
    data class Countdown(
        val session: WorkoutSession,
        val secondsRemaining: Int
    ) : WorkoutUiState

    data class Active(
        val session: WorkoutSession,
        /** Seconds left in the between-sets rest timer; 0 = not resting. */
        val restSeconds: Int = 0,
        /** Inline validation error from the latest set-matrix edit, if any. */
        val setMatrixError: String? = null
    ) : WorkoutUiState

    data class Paused(val session: WorkoutSession) : WorkoutUiState

    data class Summary(
        val session: WorkoutSession,
        val caloriesKcal: Double,
        val healthSynced: Boolean,
        /** Sum of kg x reps across every set (Fitness), null when unused. */
        val tonnageKg: Double? = null,
        /** Completed work intervals (HIIT), 0 when unused. */
        val intervalRounds: Int = 0,
        /** Interval boundary crossings (HIIT), 0 when unused. */
        val intervalIntervals: Int = 0
    ) : WorkoutUiState

    /** An unfinished session from a previous run awaits Resume/Discard. */
    data class RecoveryRequired(val session: WorkoutSession) : WorkoutUiState

    data class Error(val message: String) : WorkoutUiState
}
