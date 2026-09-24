package com.example.healthjournal.domain

import com.example.healthjournal.data.local.UnitSystem
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
                assertNull("Expected $field to accept 50", GoalValidator.validate(field, "50"))
                assertNull(
                    "Expected $field to accept its cap",
                    GoalValidator.validate(field, "${ValidateMeasurements.maxFor(field).toLong()}")
                )
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
            ValidateMeasurements.maxExceededMessage(MeasurementField.WEIGHT),
            GoalValidator.validate(MeasurementField.WEIGHT, "500.1")
        )
    }

    @Test
    fun overBoundGirth_returnsPerFieldMaxError() {
        // Torso girths share a generous cap...
        assertEquals(
            ValidateMeasurements.maxExceededMessage(MeasurementField.WAIST),
            GoalValidator.validate(MeasurementField.WAIST, "200.1")
        )
        // ...while small limbs are capped realistically (user feedback:
        // a 99 cm calf goal must be rejected).
        assertEquals(
            ValidateMeasurements.maxExceededMessage(MeasurementField.CALF),
            GoalValidator.validate(MeasurementField.CALF, "99")
        )
    }

    @Test
    fun perFieldCaps_areRealistic() {
        assertEquals(500.0, ValidateMeasurements.maxFor(MeasurementField.WEIGHT), 0.0)
        assertEquals(200.0, ValidateMeasurements.maxFor(MeasurementField.CHEST), 0.0)
        assertEquals(200.0, ValidateMeasurements.maxFor(MeasurementField.WAIST), 0.0)
        assertEquals(200.0, ValidateMeasurements.maxFor(MeasurementField.GLUTE), 0.0)
        assertEquals(120.0, ValidateMeasurements.maxFor(MeasurementField.THIGH), 0.0)
        assertEquals(75.0, ValidateMeasurements.maxFor(MeasurementField.CALF), 0.0)
        assertEquals(75.0, ValidateMeasurements.maxFor(MeasurementField.BICEP), 0.0)
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

    @Test
    fun unitLabels_mapImperialPerParameter() {
        assertEquals("lb", GoalValidator.unitLabel(MeasurementField.WEIGHT, UnitSystem.IMPERIAL))
        MeasurementField.entries
            .filter { it != MeasurementField.WEIGHT }
            .forEach { field ->
                assertEquals("in", GoalValidator.unitLabel(field, UnitSystem.IMPERIAL))
            }
    }

    @Test
    fun imperialWeightGoal_validatesAndParsesToKg() {
        assertNull(GoalValidator.validate(MeasurementField.WEIGHT, "154.3", UnitSystem.IMPERIAL))
        assertEquals(
            69.99,
            GoalValidator.parseGoal(MeasurementField.WEIGHT, "154.3", UnitSystem.IMPERIAL)!!,
            0.001
        )
    }

    @Test
    fun imperialGirthGoal_validatesAndParsesToCm() {
        assertNull(GoalValidator.validate(MeasurementField.WAIST, "33.5", UnitSystem.IMPERIAL))
        assertEquals(
            85.1,
            GoalValidator.parseGoal(MeasurementField.WAIST, "33.5", UnitSystem.IMPERIAL)!!,
            0.001
        )
    }

    @Test
    fun imperialOverCap_reportsDisplayUnitError() {
        assertEquals(
            ValidateMeasurements.maxExceededMessage(MeasurementField.WEIGHT, UnitSystem.IMPERIAL),
            GoalValidator.validate(MeasurementField.WEIGHT, "2000", UnitSystem.IMPERIAL)
        )
    }

    @Test
    fun imperialBlankMalformedAndNonPositive_matchMetricSemantics() {
        assertEquals(
            GoalValidator.ERROR_REQUIRED,
            GoalValidator.validate(MeasurementField.WEIGHT, "   ", UnitSystem.IMPERIAL)
        )
        assertEquals(
            ValidateMeasurements.ERROR_INVALID_FORMAT,
            GoalValidator.validate(MeasurementField.WEIGHT, "abc", UnitSystem.IMPERIAL)
        )
        assertEquals(
            ValidateMeasurements.ERROR_NEGATIVE,
            GoalValidator.validate(MeasurementField.WAIST, "0", UnitSystem.IMPERIAL)
        )
        assertNull(GoalValidator.parseGoal(MeasurementField.WEIGHT, "abc", UnitSystem.IMPERIAL))
        assertNull(GoalValidator.parseGoal(MeasurementField.WEIGHT, "", UnitSystem.IMPERIAL))
    }
}
