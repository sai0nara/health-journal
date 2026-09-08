package com.example.healthjournal.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ValidateWorkout, defining the acceptance rules for workout
 * capture: manual-log and configuration input must name a type, use positive
 * decimals for duration/targets, non-negative calories, and must not be
 * dated in the future.
 */
class ValidateWorkoutTest {

    @Test
    fun validManualLog_returnsNoErrors() {
        val errors = ValidateWorkout.validateManualLog(
            type = WorkoutType.RUN,
            durationMinutes = "30",
            calories = "250",
            timestamp = 1_000L,
            now = 2_000L
        )
        assertTrue(errors.isEmpty())
    }

    @Test
    fun blankType_returnsTypeError() {
        val errors = ValidateWorkout.validateManualLog(
            type = null,
            durationMinutes = "30",
            calories = "250",
            timestamp = 1_000L,
            now = 2_000L
        )
        assertEquals(ValidateWorkout.ERROR_TYPE_REQUIRED, errors["type"])
    }

    @Test
    fun malformedDuration_returnsFormatError() {
        val errors = ValidateWorkout.validateManualLog(
            type = WorkoutType.YOGA,
            durationMinutes = "abc",
            calories = "",
            timestamp = 1_000L,
            now = 2_000L
        )
        assertEquals(ValidateWorkout.ERROR_INVALID_FORMAT, errors["duration"])
    }

    @Test
    fun nonPositiveDuration_returnsNonPositiveError() {
        assertEquals(
            ValidateWorkout.ERROR_NON_POSITIVE,
            ValidateWorkout.validateManualLog(
                type = WorkoutType.RUN,
                durationMinutes = "0",
                calories = "",
                timestamp = 1_000L,
                now = 2_000L
            )["duration"]
        )
        assertEquals(
            ValidateWorkout.ERROR_NON_POSITIVE,
            ValidateWorkout.validateManualLog(
                type = WorkoutType.RUN,
                durationMinutes = "-5",
                calories = "",
                timestamp = 1_000L,
                now = 2_000L
            )["duration"]
        )
    }

    @Test
    fun negativeCalories_returnsNegativeError() {
        val errors = ValidateWorkout.validateManualLog(
            type = WorkoutType.FITNESS,
            durationMinutes = "45",
            calories = "-10",
            timestamp = 1_000L,
            now = 2_000L
        )
        assertEquals(ValidateWorkout.ERROR_NEGATIVE, errors["calories"])
    }

    @Test
    fun futureTimestamp_returnsFutureDateError() {
        val errors = ValidateWorkout.validateManualLog(
            type = WorkoutType.RUN,
            durationMinutes = "30",
            calories = "",
            timestamp = 3_000L,
            now = 2_000L
        )
        assertEquals(ValidateWorkout.ERROR_FUTURE_DATE, errors["timestamp"])
    }

    @Test
    fun blankCaloriesAndNotes_areAllowed() {
        val errors = ValidateWorkout.validateManualLog(
            type = WorkoutType.YOGA,
            durationMinutes = "60",
            calories = "   ",
            timestamp = 1_000L,
            now = 2_000L
        )
        assertTrue(errors.isEmpty())
    }

    @Test
    fun validTarget_returnsNoErrors() {
        assertTrue(ValidateWorkout.validateTarget(WorkoutType.RUN, "5").isEmpty())
        assertTrue(ValidateWorkout.validateTarget(WorkoutType.YOGA, "45").isEmpty())
    }

    @Test
    fun blankTargetType_returnsTypeError() {
        assertEquals(
            ValidateWorkout.ERROR_TYPE_REQUIRED,
            ValidateWorkout.validateTarget(null, "5")["type"]
        )
    }

    @Test
    fun malformedOrNonPositiveTarget_returnsError() {
        assertEquals(
            ValidateWorkout.ERROR_INVALID_FORMAT,
            ValidateWorkout.validateTarget(WorkoutType.RUN, "far")["target"]
        )
        assertEquals(
            ValidateWorkout.ERROR_NON_POSITIVE,
            ValidateWorkout.validateTarget(WorkoutType.FITNESS, "0")["target"]
        )
    }

    @Test
    fun durationOverOneDay_returnsTooLongError() {
        val errors = ValidateWorkout.validateManualLog(
            type = WorkoutType.RUN,
            durationMinutes = "2000",
            calories = "",
            timestamp = 1_000L,
            now = 2_000L
        )
        assertEquals(ValidateWorkout.ERROR_DURATION_TOO_LONG, errors["duration"])
    }

    @Test
    fun durationAtOneDay_isAllowed() {
        val errors = ValidateWorkout.validateManualLog(
            type = WorkoutType.RUN,
            durationMinutes = "1440",
            calories = "",
            timestamp = 1_000L,
            now = 2_000L
        )
        assertTrue(errors.isEmpty())
    }

    @Test
    fun caloriesOverCap_returnsTooLargeError() {
        val errors = ValidateWorkout.validateManualLog(
            type = WorkoutType.FITNESS,
            durationMinutes = "60",
            calories = "99999",
            timestamp = 1_000L,
            now = 2_000L
        )
        assertEquals(ValidateWorkout.ERROR_CALORIES_TOO_LARGE, errors["calories"])
    }

    @Test
    fun caloriesAtCap_isAllowed() {
        val errors = ValidateWorkout.validateManualLog(
            type = WorkoutType.FITNESS,
            durationMinutes = "60",
            calories = "50000",
            timestamp = 1_000L,
            now = 2_000L
        )
        assertTrue(errors.isEmpty())
    }
}
