package com.example.healthjournal.viewmodel

import com.example.healthjournal.domain.PresetExercise
import com.example.healthjournal.domain.ScheduledDay

/**
 * MVI states for the preset-management flow. The library renders the
 * preset list from [PresetViewModel.presets]; exactly one of these
 * states drives the screen, and all transitions run through
 * [PresetViewModel].
 */
sealed interface PresetUiState {

    data object Idle : PresetUiState

    data object Library : PresetUiState

    /** Draft being created ([presetId] null) or edited ([presetId] set). */
    data class Editing(
        val presetId: String? = null,
        val name: String = "",
        val scheduledDay: ScheduledDay = ScheduledDay.ANY,
        val exercises: List<PresetExercise> = emptyList(),
        val nameError: String? = null,
        val exerciseError: String? = null
    ) : PresetUiState
}