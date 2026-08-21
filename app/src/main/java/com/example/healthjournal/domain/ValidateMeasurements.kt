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
    const val ERROR_WEIGHT_MAX = "Too large (max 500 kg)"
    const val ERROR_GIRTH_MAX = "Too large (max 300 cm)"

    private const val MAX_WEIGHT_KG = 500.0
    private const val MAX_GIRTH_CM = 300.0
    private val GIRTH_FIELDS = setOf(
        MeasurementField.CHEST,
        MeasurementField.WAIST,
        MeasurementField.GLUTE,
        MeasurementField.THIGH,
        MeasurementField.CALF,
        MeasurementField.BICEP
    )

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
                field == MeasurementField.WEIGHT && value > MAX_WEIGHT_KG ->
                    errors[field] = ERROR_WEIGHT_MAX
                field in GIRTH_FIELDS && value > MAX_GIRTH_CM ->
                    errors[field] = ERROR_GIRTH_MAX
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
