package com.example.healthjournal.domain

enum class MeasurementField(val label: String) {
    WEIGHT("Weight"),
    CHEST("Chest"),
    WAIST("Waist"),
    GLUTE("Glute"),
    THIGH("Thighs"),
    CALF("Calves"),
    BICEP("Biceps")
}

object ValidateMeasurements {

    const val ERROR_INVALID_FORMAT = "Invalid decimal format"
    const val ERROR_NEGATIVE = "Cannot be negative"

    /** Realistic per-parameter sanity caps (user-reviewed 2026-08): a calf
     *  cannot approach 100 cm, while torso girths get headroom. */
    private val MAX_BOUNDS = mapOf(
        MeasurementField.WEIGHT to 500.0,
        MeasurementField.CHEST to 200.0,
        MeasurementField.WAIST to 200.0,
        MeasurementField.GLUTE to 200.0,
        MeasurementField.THIGH to 120.0,
        MeasurementField.CALF to 75.0,
        MeasurementField.BICEP to 75.0
    )

    /** Sanity bound for [field], shared by capture and goal validation. */
    fun maxFor(field: MeasurementField): Double = MAX_BOUNDS.getValue(field)

    /** Inline error copy for values above the field's cap. */
    fun maxExceededMessage(field: MeasurementField): String =
        if (field == MeasurementField.WEIGHT) {
            "Too large (max ${maxFor(field).toLong()} kg)"
        } else {
            "Too large (max ${maxFor(field).toLong()} cm)"
        }

    /**
     * Validates raw text-field input per measurement field. Blank fields are
     * skipped (partial entries are allowed); any non-blank field must parse as
     * a non-negative decimal within its sanity bound. Returns a map of
     * field -> inline error message; an empty map means all input is valid.
     */
    fun validate(rawValues: Map<MeasurementField, String>): Map<MeasurementField, String> {
        val errors = linkedMapOf<MeasurementField, String>()
        rawValues.forEach { (field, raw) ->
            val text = raw.trim()
            if (text.isEmpty()) return@forEach

            val value = parseDecimal(text)
            when {
                value == null -> errors[field] = ERROR_INVALID_FORMAT
                value < 0.0 -> errors[field] = ERROR_NEGATIVE
                value > maxFor(field) ->
                    errors[field] = maxExceededMessage(field)
            }
        }
        return errors
    }

    /**
     * Presence check independent of validity: true when at least one field
     * has non-blank text. Save stays blocked while [validate] reports errors.
     */
    fun hasAtLeastOneMeasurement(rawValues: Map<MeasurementField, String>): Boolean =
        rawValues.values.any { it.isNotBlank() }

    /** Parses user input accepting both '.' and ',' decimal separators. */
    fun parseDecimal(text: String): Double? =
        text.trim().replace(',', '.').toDoubleOrNull()
}
