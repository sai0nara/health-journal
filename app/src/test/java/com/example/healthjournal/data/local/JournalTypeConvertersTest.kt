package com.example.healthjournal.data.local

import com.example.healthjournal.domain.StrengthExercise
import com.example.healthjournal.domain.StrengthSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for the set-matrix JSON converter: a WorkoutSession persists its
 * strength exercises/sets (kg + reps) in a single text column, serialized
 * with Gson.
 */
class JournalTypeConvertersTest {

    private val converters = JournalTypeConverters()

    @Test
    fun setMatrix_roundTripsLosslessly() {
        val exercises = listOf(
            StrengthExercise(
                name = "Squat",
                sets = listOf(StrengthSet(kg = 60.0, reps = 10), StrengthSet(kg = 70.0, reps = 8))
            ),
            StrengthExercise(name = "Plank", sets = emptyList())
        )

        val json = converters.fromStrengthExercises(exercises)
        assertEquals(exercises, converters.toStrengthExercises(json))
    }

    @Test
    fun setMatrix_nullColumn_readsBackNull() {
        assertNull(converters.toStrengthExercises(null))
    }

    @Test
    fun setMatrix_emptyMatrix_roundTripsAsEmptyList() {
        val json = converters.fromStrengthExercises(emptyList())
        assertEquals(emptyList<StrengthExercise>(), converters.toStrengthExercises(json))
    }
}