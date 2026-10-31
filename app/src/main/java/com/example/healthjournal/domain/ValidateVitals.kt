package com.example.healthjournal.domain

import com.example.healthjournal.data.local.UnitConverter
import com.example.healthjournal.data.local.UnitSystem
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

/** Raw display-unit text for the five editable manual vitals. */
data class VitalsInput(
    val systolic: String = "",
    val diastolic: String = "",
    val heartRate: String = "",
    val glucose: String = "",
    val temperature: String = "",
    val spo2: String = ""
)

/** Canonical-metric vitals attached to one journal entry. */
data class EntryVitals(
    val bpSystolic: Double? = null,
    val bpDiastolic: Double? = null,
    val heartRate: Int? = null,
    val bloodGlucoseMmol: Double? = null,
    val bodyTemperatureC: Double? = null,
    val spo2Percent: Int? = null
)

enum class VitalField {
    SYSTOLIC,
    DIASTOLIC,
    HEART_RATE,
    GLUCOSE,
    TEMPERATURE,
    SPO2
}

sealed interface VitalsError {
    data object InvalidFormat : VitalsError
    data object IntegerRequired : VitalsError
    data object BloodPressurePairRequired : VitalsError
    data object SystolicNotGreater : VitalsError
    data class OutOfRange(val min: Double, val max: Double) : VitalsError
}

object ValidateVitals {

    private val SYSTOLIC_RANGE = 50.0..300.0
    private val DIASTOLIC_RANGE = 30.0..200.0
    private val HEART_RATE_RANGE = 20.0..300.0
    private val GLUCOSE_RANGE = 1.0..50.0
    private val TEMPERATURE_RANGE = 30.0..45.0
    private val SPO2_RANGE = 0.0..100.0

    fun validate(input: VitalsInput, unitSystem: UnitSystem): Map<VitalField, VitalsError> {
        val errors = linkedMapOf<VitalField, VitalsError>()
        validateBloodPressure(input, errors)
        validateIntegerField(input.heartRate, VitalField.HEART_RATE, HEART_RATE_RANGE, errors)
        validateDecimalField(
            raw = input.glucose,
            field = VitalField.GLUCOSE,
            range = GLUCOSE_RANGE,
            errors = errors
        ) { value ->
            if (unitSystem == UnitSystem.IMPERIAL) UnitConverter.mgdlToMmol(value) else value
        }
        validateDecimalField(
            raw = input.temperature,
            field = VitalField.TEMPERATURE,
            range = TEMPERATURE_RANGE,
            errors = errors
        ) { value ->
            if (unitSystem == UnitSystem.IMPERIAL) UnitConverter.fahrenheitToCelsius(value) else value
        }
        validateIntegerField(input.spo2, VitalField.SPO2, SPO2_RANGE, errors)
        return errors
    }

    fun toEntryVitals(input: VitalsInput, unitSystem: UnitSystem): EntryVitals? {
        if (validate(input, unitSystem).isNotEmpty()) return null

        return EntryVitals(
            bpSystolic = parseDecimal(input.systolic),
            bpDiastolic = parseDecimal(input.diastolic),
            heartRate = input.heartRate.trim().toIntOrNull(),
            bloodGlucoseMmol = parseDecimal(input.glucose)?.let { value ->
                round(
                    if (unitSystem == UnitSystem.IMPERIAL) UnitConverter.mgdlToMmol(value) else value,
                    scale = 2
                )
            },
            bodyTemperatureC = parseDecimal(input.temperature)?.let { value ->
                round(
                    if (unitSystem == UnitSystem.IMPERIAL) UnitConverter.fahrenheitToCelsius(value) else value,
                    scale = 1
                )
            },
            spo2Percent = input.spo2.trim().toIntOrNull()
        )
    }

    fun formatVitals(vitals: EntryVitals, unitSystem: UnitSystem): VitalsInput {
        return VitalsInput(
            systolic = formatBp(vitals.bpSystolic),
            diastolic = formatBp(vitals.bpDiastolic),
            heartRate = vitals.heartRate?.toString().orEmpty(),
            glucose = formatGlucose(vitals.bloodGlucoseMmol, unitSystem),
            temperature = formatTemperature(vitals.bodyTemperatureC, unitSystem),
            spo2 = vitals.spo2Percent?.toString().orEmpty()
        )
    }

    private fun validateBloodPressure(
        input: VitalsInput,
        errors: MutableMap<VitalField, VitalsError>
    ) {
        val systolicBlank = input.systolic.isBlank()
        val diastolicBlank = input.diastolic.isBlank()

        if (systolicBlank && diastolicBlank) return
        if (systolicBlank) {
            errors[VitalField.SYSTOLIC] = VitalsError.BloodPressurePairRequired
            return
        }
        if (diastolicBlank) {
            errors[VitalField.DIASTOLIC] = VitalsError.BloodPressurePairRequired
            return
        }

        val systolic = parseDecimal(input.systolic)
        val diastolic = parseDecimal(input.diastolic)
        var validPair = true

        when {
            systolic == null -> {
                errors[VitalField.SYSTOLIC] = VitalsError.InvalidFormat
                validPair = false
            }
            systolic !in SYSTOLIC_RANGE -> {
                errors[VitalField.SYSTOLIC] = VitalsError.OutOfRange(SYSTOLIC_RANGE.start, SYSTOLIC_RANGE.endInclusive)
                validPair = false
            }
        }
        when {
            diastolic == null -> {
                errors[VitalField.DIASTOLIC] = VitalsError.InvalidFormat
                validPair = false
            }
            diastolic !in DIASTOLIC_RANGE -> {
                errors[VitalField.DIASTOLIC] = VitalsError.OutOfRange(DIASTOLIC_RANGE.start, DIASTOLIC_RANGE.endInclusive)
                validPair = false
            }
        }

        if (validPair && systolic!! <= diastolic!!) {
            errors[VitalField.SYSTOLIC] = VitalsError.SystolicNotGreater
        }
    }

    private fun validateIntegerField(
        raw: String,
        field: VitalField,
        range: ClosedFloatingPointRange<Double>,
        errors: MutableMap<VitalField, VitalsError>
    ) {
        val text = raw.trim()
        if (text.isEmpty()) return

        val value = text.toIntOrNull()
        if (value == null) {
            errors[field] = if (parseDecimal(text) != null) {
                VitalsError.IntegerRequired
            } else {
                VitalsError.InvalidFormat
            }
            return
        }
        if (value.toDouble() !in range) {
            errors[field] = VitalsError.OutOfRange(range.start, range.endInclusive)
        }
    }

    private fun validateDecimalField(
        raw: String,
        field: VitalField,
        range: ClosedFloatingPointRange<Double>,
        errors: MutableMap<VitalField, VitalsError>,
        toCanonical: (Double) -> Double
    ) {
        val text = raw.trim()
        if (text.isEmpty()) return

        val displayValue = parseDecimal(text)
        if (displayValue == null) {
            errors[field] = VitalsError.InvalidFormat
            return
        }
        val canonicalValue = toCanonical(displayValue)
        if (canonicalValue !in range) {
            errors[field] = VitalsError.OutOfRange(range.start, range.endInclusive)
        }
    }

    private fun parseDecimal(raw: String): Double? =
        raw.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

    private fun round(value: Double, scale: Int): Double =
        BigDecimal(value).setScale(scale, RoundingMode.HALF_UP).toDouble()

    private fun formatBp(value: Double?): String =
        value?.let { UnitConverter.formatDouble(it) }.orEmpty()

    private fun formatGlucose(mmol: Double?, unitSystem: UnitSystem): String {
        if (mmol == null) return ""
        return if (unitSystem == UnitSystem.IMPERIAL) {
            UnitConverter.mmolToMgdl(mmol).roundToInt().toString()
        } else {
            formatOneDecimal(mmol)
        }
    }

    private fun formatTemperature(celsius: Double?, unitSystem: UnitSystem): String {
        if (celsius == null) return ""
        return if (unitSystem == UnitSystem.IMPERIAL) {
            formatOneDecimal(UnitConverter.celsiusToFahrenheit(celsius))
        } else {
            formatOneDecimal(celsius)
        }
    }

    private fun formatOneDecimal(value: Double): String =
        BigDecimal(value)
            .setScale(1, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
}
