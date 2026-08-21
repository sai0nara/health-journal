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

    fun validate(rawValues: Map<MeasurementField, String>): Map<MeasurementField, String> {
        // TODO: implement in Green phase
        return emptyMap()
    }

    fun hasAtLeastOneMeasurement(rawValues: Map<MeasurementField, String>): Boolean {
        // TODO: implement in Green phase
        return false
    }
}
