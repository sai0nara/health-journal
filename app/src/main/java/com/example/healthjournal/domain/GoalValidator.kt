package com.example.healthjournal.domain

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
        if (field == MeasurementField.WEIGHT) "kg" else "cm"

    /**
     * Returns the inline error message for invalid input, or null when the
     * text parses as a valid goal value for [field]. Blank input is an error:
     * unlike capture fields a goal cannot be partially blank.
     */
    fun validate(field: MeasurementField, rawText: String): String? {
        val text = rawText.trim()
        if (text.isEmpty()) return ERROR_REQUIRED

        val value = ValidateMeasurements.parseDecimal(text)
        return when {
            value == null -> ValidateMeasurements.ERROR_INVALID_FORMAT
            value <= 0.0 -> ValidateMeasurements.ERROR_NEGATIVE
            value > ValidateMeasurements.maxFor(field) ->
                ValidateMeasurements.maxExceededMessage(field)
            else -> null
        }
    }
}
