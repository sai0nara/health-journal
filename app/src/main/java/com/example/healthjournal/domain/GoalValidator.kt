package com.example.healthjournal.domain

import com.example.healthjournal.data.local.UnitSystem

/**
 * Validates Body Analytics goal input for a single [MeasurementField].
 * Reuses the measurement capture bounds and error copy so goals feel
 * identical to the rest of the capture UX; a goal is a strictly positive
 * decimal within the field's sanity bound.
 */
object GoalValidator {

    const val ERROR_REQUIRED = "Enter a goal value"

    /** Metric unit for the field's goal, shown in chart label and dialog. */
    fun unitLabel(field: MeasurementField): String =
        unitLabel(field, UnitSystem.METRIC)

    /** Display unit for the field's goal in the given system. */
    fun unitLabel(field: MeasurementField, unitSystem: UnitSystem): String =
        if (unitSystem == UnitSystem.IMPERIAL) {
            if (field == MeasurementField.WEIGHT) "lb" else "in"
        } else {
            if (field == MeasurementField.WEIGHT) "kg" else "cm"
        }

    /**
     * Returns the inline error message for invalid input, or null when the
     * text parses as a valid goal value for [field]. Blank input is an error:
     * unlike capture fields a goal cannot be partially blank.
     */
    fun validate(field: MeasurementField, rawText: String): String? =
        validate(field, rawText, UnitSystem.METRIC)

    /**
     * Unit-aware goal validation: display-unit input is parsed back to
     * canonical metric before the shared sanity bounds apply.
     */
    fun validate(field: MeasurementField, rawText: String, unitSystem: UnitSystem): String? {
        val text = rawText.trim()
        if (text.isEmpty()) return ERROR_REQUIRED

        val value = ValidateMeasurements.parseMetric(text, field, unitSystem)
        return when {
            value == null -> ValidateMeasurements.ERROR_INVALID_FORMAT
            value <= 0.0 -> ValidateMeasurements.ERROR_NEGATIVE
            value > ValidateMeasurements.maxFor(field) ->
                ValidateMeasurements.maxExceededMessage(field, unitSystem)
            else -> null
        }
    }

    /**
     * Canonical metric value for a goal input, or null when the input is
     * blank, malformed, or outside the valid range.
     */
    fun parseGoal(field: MeasurementField, rawText: String, unitSystem: UnitSystem): Double? {
        val text = rawText.trim()
        if (text.isEmpty()) return null
        return ValidateMeasurements.parseMetric(text, field, unitSystem)
            ?.takeIf { it > 0.0 && it <= ValidateMeasurements.maxFor(field) }
    }
}
