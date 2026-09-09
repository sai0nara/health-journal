package com.example.healthjournal.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for CalorieEstimator, defining the MET-based calorie math used
 * to pre-fill workout summaries: calories = MET x body weight (kg) x hours.
 */
class CalorieEstimatorTest {

    @Test
    fun workoutTypes_carryExpectedMetValues() {
        assertEquals(9.8, WorkoutType.RUN.met, 0.0)
        assertEquals(6.0, WorkoutType.FITNESS.met, 0.0)
        assertEquals(3.0, WorkoutType.YOGA.met, 0.0)
    }

    @Test
    fun estimate_appliesMetFormula() {
        // 9.8 MET x 70 kg x 0.5 h = 343 kcal.
        assertEquals(
            343.0,
            CalorieEstimator.estimate(WorkoutType.RUN, 30.0, 70.0),
            0.01
        )
    }

    @Test
    fun estimate_scalesWithDurationAndWeight() {
        val halfHour = CalorieEstimator.estimate(WorkoutType.YOGA, 30.0, 70.0)
        val fullHour = CalorieEstimator.estimate(WorkoutType.YOGA, 60.0, 70.0)
        assertEquals(halfHour * 2, fullHour, 0.01)

        val light = CalorieEstimator.estimate(WorkoutType.FITNESS, 60.0, 60.0)
        val heavy = CalorieEstimator.estimate(WorkoutType.FITNESS, 60.0, 90.0)
        assertEquals(light * 1.5, heavy, 0.01)
    }

    @Test
    fun estimate_withoutWeight_usesDefault() {
        assertEquals(
            CalorieEstimator.estimate(WorkoutType.RUN, 30.0, CalorieEstimator.DEFAULT_WEIGHT_KG),
            CalorieEstimator.estimate(WorkoutType.RUN, 30.0),
            0.0
        )
    }

    @Test
    fun estimate_zeroDuration_returnsZero() {
        assertEquals(0.0, CalorieEstimator.estimate(WorkoutType.YOGA, 0.0, 70.0), 0.0)
    }
}
