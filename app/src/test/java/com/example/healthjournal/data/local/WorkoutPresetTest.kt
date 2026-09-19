package com.example.healthjournal.data.local

import com.example.healthjournal.domain.PresetExercise
import com.example.healthjournal.domain.ScheduledDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the WorkoutPreset entity defaults and the PresetExercise
 * value object: a fresh preset carries generated identity, an unassigned
 * scheduled day, and the user's default target sets/reps/weight/rest per
 * exercise.
 */
class WorkoutPresetTest {

    @Test
    fun freshPreset_hasGeneratedIdAndUnassignedDay() {
        val preset = WorkoutPreset(name = "Leg Day")

        assertTrue(preset.id.isNotBlank())
        assertEquals("Leg Day", preset.name)
        assertEquals(ScheduledDay.ANY.name, preset.scheduledDay)
        assertTrue(preset.exercises.isEmpty())
    }

    @Test
    fun preset_preservesConfiguredExercises() {
        val exercise = PresetExercise(
            exerciseId = "exercise-1",
            targetSets = 4,
            defaultReps = 8,
            defaultWeightKg = 60.0,
            restSeconds = 90
        )

        val preset = WorkoutPreset(name = "Chest Day", scheduledDay = ScheduledDay.TUESDAY.name, exercises = listOf(exercise))

        assertEquals(1, preset.exercises.size)
        assertEquals(ScheduledDay.TUESDAY.name, preset.scheduledDay)
        assertEquals(exercise, preset.exercises.single())
    }

    @Test
    fun freshPreset_timestampDefaultsToNow() {
        val before = System.currentTimeMillis()
        val preset = WorkoutPreset(name = "Push Day")
        val after = System.currentTimeMillis()

        assertTrue(preset.lastModified in before..after)
    }

    @Test
    fun presetExercise_carriesPerExerciseTargets() {
        val exercise = PresetExercise(
            exerciseId = "squat",
            targetSets = 3,
            defaultReps = 5,
            defaultWeightKg = 100.0,
            restSeconds = 120
        )

        assertEquals(3, exercise.targetSets)
        assertEquals(5, exercise.defaultReps)
        assertEquals(100.0, exercise.defaultWeightKg, 0.0)
        assertEquals(120, exercise.restSeconds)
    }
}