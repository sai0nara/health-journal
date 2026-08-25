package com.example.healthjournal.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for GoalValidator, defining the acceptance rules for the
 * Body Analytics goal dialog: a goal must parse as a strictly positive
 * decimal within the same sanity bounds used for measurement capture
 * (500 kg for Weight, 300 cm for girths), and each parameter maps to its
 * metric unit label (kg vs cm) shown in the chart and dialog.
 */
class GoalValidatorTest {

    @Test
    fun validWeightGoal_returnsNoError() {
        assertNull(GoalValidator.validate(MeasurementField.WEIGHT, "78.5"))
        assertNull(GoalValidator.validate(MeasurementField.WEIGHT, "500"))
        assertNull(GoalValidator.validate(MeasurementField.WEIGHT, "80,5"))
    }

    @Test
    fun validGirthGoals_returnNoError() {
        MeasurementField.entries
            .filter { it != MeasurementField.WEIGHT }
            .forEach { field ->
                assertNull("Expected $field to accept 85", GoalValidator.validate(field, "85"))
                assertNull("Expected $field to accept 300", GoalValidator.validate(field, "300"))
            }
    }

    @Test
    fun blankInput_returnsError() {
        assertEquals(
            GoalValidator.ERROR_REQUIRED,
            GoalValidator.validate(MeasurementField.WEIGHT, "")
        )
        assertEquals(
            GoalValidator.ERROR_REQUIRED,
            GoalValidator.validate(MeasurementField.WAIST, "   ")
        )
    }

    @Test
    fun malformedInput_returnsFormatError() {
        assertEquals(
            ValidateMeasurements.ERROR_INVALID_FORMAT,
            GoalValidator.validate(MeasurementField.WEIGHT, "abc")
        )
    }

    @Test
    fun nonPositiveInput_returnsNegativeError() {
        assertEquals(
            ValidateMeasurements.ERROR_NEGATIVE,
            GoalValidator.validate(MeasurementField.WEIGHT, "-5")
        )
        assertEquals(
            ValidateMeasurements.ERROR_NEGATIVE,
            GoalValidator.validate(MeasurementField.WAIST, "0")
        )
    }

    @Test
    fun overBoundWeight_returnsWeightMaxError() {
        assertEquals(
            ValidateMeasurements.ERROR_WEIGHT_MAX,
            GoalValidator.validate(MeasurementField.WEIGHT, "500.1")
        )
    }

    @Test
    fun overBoundGirth_returnsGirthMaxError() {
        assertEquals(
            ValidateMeasurements.ERROR_GIRTH_MAX,
            GoalValidator.validate(MeasurementField.WAIST, "300.1")
        )
    }

    @Test
    fun unitLabels_mapMetricPerParameter() {
        assertEquals("kg", GoalValidator.unitLabel(MeasurementField.WEIGHT))
        MeasurementField.entries
            .filter { it != MeasurementField.WEIGHT }
            .forEach { field ->
                assertEquals("cm", GoalValidator.unitLabel(field))
            }
    }
}
