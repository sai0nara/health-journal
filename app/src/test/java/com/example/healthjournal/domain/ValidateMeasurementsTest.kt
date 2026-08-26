package com.example.healthjournal.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the ValidateMeasurements domain component, verifying
 * inline validation rules: decimal format, non-negative values, sanity
 * upper bounds (weight <= 500 kg, girths <= 300 cm) and the
 * at-least-one-value requirement for partial entries.
 */
class ValidateMeasurementsTest {

    private fun raw(vararg pairs: Pair<MeasurementField, String>): Map<MeasurementField, String> =
        MeasurementField.entries.associateWith { field ->
            pairs.firstOrNull { it.first == field }?.second.orEmpty()
        }

    @Test
    fun validPartialEntry_producesNoErrors() {
        val errors = ValidateMeasurements.validate(raw(MeasurementField.WAIST to "85"))

        assertTrue(errors.isEmpty())
    }

    @Test
    fun allFieldsValid_producesNoErrors() {
        val errors = ValidateMeasurements.validate(
            raw(
                MeasurementField.WEIGHT to "78.5",
                MeasurementField.CHEST to "100",
                MeasurementField.WAIST to "85",
                MeasurementField.GLUTE to "95",
                MeasurementField.THIGH to "55.5",
                MeasurementField.CALF to "36",
                MeasurementField.BICEP to "32"
            )
        )

        assertTrue(errors.isEmpty())
    }

    @Test
    fun malformedDecimal_reportsInvalidFormat() {
        val errors = ValidateMeasurements.validate(raw(MeasurementField.WEIGHT to "12,3.4.5"))

        assertEquals(ValidateMeasurements.ERROR_INVALID_FORMAT, errors[MeasurementField.WEIGHT])
    }

    @Test
    fun nonNumericText_reportsInvalidFormat() {
        val errors = ValidateMeasurements.validate(raw(MeasurementField.BICEP to "abc"))

        assertEquals(ValidateMeasurements.ERROR_INVALID_FORMAT, errors[MeasurementField.BICEP])
    }

    @Test
    fun negativeValue_reportsNegativeError() {
        val errors = ValidateMeasurements.validate(raw(MeasurementField.WAIST to "-5"))

        assertEquals(ValidateMeasurements.ERROR_NEGATIVE, errors[MeasurementField.WAIST])
    }

    @Test
    fun weightAbove500kg_reportsWeightMaxError() {
        val errors = ValidateMeasurements.validate(raw(MeasurementField.WEIGHT to "501"))

        assertEquals(
            ValidateMeasurements.maxExceededMessage(MeasurementField.WEIGHT),
            errors[MeasurementField.WEIGHT]
        )
    }

    @Test
    fun girthAbovePerFieldCap_reportsGirthMaxError() {
        // Chest caps at 200 cm; small limbs cap tighter (realistic bounds).
        val errors = ValidateMeasurements.validate(
            raw(MeasurementField.CHEST to "201", MeasurementField.CALF to "76")
        )

        assertEquals(
            ValidateMeasurements.maxExceededMessage(MeasurementField.CHEST),
            errors[MeasurementField.CHEST]
        )
        assertEquals(
            ValidateMeasurements.maxExceededMessage(MeasurementField.CALF),
            errors[MeasurementField.CALF]
        )
    }

    @Test
    fun boundaryValues_atPerFieldCaps_areAccepted() {
        val errors = ValidateMeasurements.validate(
            raw(
                MeasurementField.WEIGHT to "500",
                MeasurementField.WAIST to "200",
                MeasurementField.CALF to "75"
            )
        )

        assertTrue(errors.isEmpty())
    }

    @Test
    fun blankFields_doNotProduceErrors() {
        val errors = ValidateMeasurements.validate(raw())

        assertTrue(errors.isEmpty())
    }

    @Test
    fun invalidInputOnOneField_doesNotFlagOtherFields() {
        val errors = ValidateMeasurements.validate(
            raw(
                MeasurementField.WEIGHT to "not-a-number",
                MeasurementField.WAIST to "85"
            )
        )

        assertEquals(setOf(MeasurementField.WEIGHT), errors.keys)
    }

    @Test
    fun hasAtLeastOneMeasurement_falseWhenAllBlank() {
        assertFalse(ValidateMeasurements.hasAtLeastOneMeasurement(raw()))
    }

    @Test
    fun hasAtLeastOneMeasurement_trueWhenSingleFieldFilled() {
        assertTrue(
            ValidateMeasurements.hasAtLeastOneMeasurement(raw(MeasurementField.CALF to "36"))
        )
    }

    @Test
    fun hasAtLeastOneMeasurement_trueEvenWhenValueInvalid() {
        // Presence check is independent of validity; save stays blocked by error map.
        assertTrue(
            ValidateMeasurements.hasAtLeastOneMeasurement(raw(MeasurementField.WEIGHT to "abc"))
        )
    }
}
