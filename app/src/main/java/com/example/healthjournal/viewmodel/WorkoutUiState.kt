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

    data class Active(val session: WorkoutSession) : WorkoutUiState

    data class Paused(val session: WorkoutSession) : WorkoutUiState

    data class Summary(
        val session: WorkoutSession,
        val caloriesKcal: Double,
        val healthSynced: Boolean
    ) : WorkoutUiState

    /** An unfinished session from a previous run awaits Resume/Discard. */
    data class RecoveryRequired(val session: WorkoutSession) : WorkoutUiState

    data class Error(val message: String) : WorkoutUiState
}
