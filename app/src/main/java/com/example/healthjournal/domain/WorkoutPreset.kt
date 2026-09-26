package com.example.healthjournal.domain

import androidx.annotation.StringRes
import com.example.healthjournal.R

/**
 * Days a workout preset can be scheduled against; ANY means no fixed day.
 * A preset is a saved routine that drives the structured execution flow.
 *
 * Only [labelRes] is shown in UI; the persisted [name] stays canonical
 * English so stored presets, backups, and sync payloads never change.
 */
enum class ScheduledDay(@StringRes val labelRes: Int) {
    MONDAY(R.string.day_monday),
    TUESDAY(R.string.day_tuesday),
    WEDNESDAY(R.string.day_wednesday),
    THURSDAY(R.string.day_thursday),
    FRIDAY(R.string.day_friday),
    SATURDAY(R.string.day_saturday),
    SUNDAY(R.string.day_sunday),
    ANY(R.string.day_any)
}

/**
 * One exercise inside a preset: which catalog exercise to perform and the
 * progressive-overload defaults applied to each performed set (target count,
 * reps, weight) plus the rest length between sets. The executed session
 * records actuals; these are the planned targets.
 */
data class PresetExercise(
    val exerciseId: String,
    val targetSets: Int,
    val defaultReps: Int,
    val defaultWeightKg: Double,
    val restSeconds: Int
)