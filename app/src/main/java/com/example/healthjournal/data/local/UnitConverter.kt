package com.example.healthjournal.data.local

import java.math.BigDecimal
import java.math.RoundingMode

/** Split imperial height entry: whole feet plus the remaining inches. */
data class FeetInches(val feet: Int, val inches: Double)

object UnitConverter {
    private const val CM_PER_INCH = 2.54
    private const val KG_PER_LB = 0.45359237
    private const val MGDL_PER_MMOL = 18.0182
    private const val FAHRENHEIT_OFFSET = 32.0
    private const val FAHRENHEIT_SCALE = 9.0 / 5.0

    fun cmToInches(cm: Double): Double {
        return BigDecimal(cm / CM_PER_INCH)
            .setScale(1, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun inchesToCm(inches: Double): Double {
        return BigDecimal(inches * CM_PER_INCH)
            .setScale(1, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun kgToLbs(kg: Double): Double {
        return BigDecimal(kg / KG_PER_LB)
            .setScale(1, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun lbsToKg(lbs: Double): Double {
        return BigDecimal(lbs * KG_PER_LB)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun mmolToMgdl(mmol: Double): Double {
        return BigDecimal(mmol * MGDL_PER_MMOL)
            .setScale(1, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun mgdlToMmol(mgdl: Double): Double {
        return BigDecimal(mgdl / MGDL_PER_MMOL)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun celsiusToFahrenheit(celsius: Double): Double {
        return BigDecimal(celsius * FAHRENHEIT_SCALE + FAHRENHEIT_OFFSET)
            .setScale(1, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun fahrenheitToCelsius(fahrenheit: Double): Double {
        return BigDecimal((fahrenheit - FAHRENHEIT_OFFSET) / FAHRENHEIT_SCALE)
            .setScale(1, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun formatForDisplay(value: Double?, unitSystem: UnitSystem, isHeight: Boolean): String {
        if (value == null) return ""
        return if (unitSystem == UnitSystem.IMPERIAL) {
            val displayValue = if (isHeight) cmToInches(value) else kgToLbs(value)
            formatDouble(displayValue)
        } else {
            formatDouble(value)
        }
    }

    fun parseInput(input: String, unitSystem: UnitSystem, isHeight: Boolean): Double? {
        if (input.isEmpty()) return null
        val value = input.toDoubleOrNull() ?: return null
        return if (unitSystem == UnitSystem.IMPERIAL) {
            if (isHeight) inchesToCm(value) else lbsToKg(value)
        } else {
            BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toDouble()
        }
    }

    fun formatDouble(value: Double): String {
        return BigDecimal(value)
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }

    /**
     * Splits a metric height into whole feet plus remaining inches, rounding
     * the total to one decimal first so the parts always sum back cleanly.
     */
    fun cmToFeetInches(cm: Double): FeetInches {
        val totalInches = BigDecimal(cm / CM_PER_INCH)
            .setScale(1, RoundingMode.HALF_UP)
            .toDouble()
        val feet = (totalInches / 12).toInt()
        val inches = BigDecimal(totalInches - feet * 12)
            .setScale(1, RoundingMode.HALF_UP)
            .toDouble()
        return FeetInches(feet, inches)
    }

    /** Combines split imperial height entry back to metric centimetres. */
    fun feetInchesToCm(feet: Int, inches: Double): Double {
        return BigDecimal((feet * 12 + inches) * CM_PER_INCH)
            .setScale(1, RoundingMode.HALF_UP)
            .toDouble()
    }

    /**
     * Display formatting for body measurements: weight converts kg to lb,
     * lengths (circumferences) convert cm to inches; metric passes through.
     */
    fun formatMeasurement(value: Double?, unitSystem: UnitSystem, isWeight: Boolean): String {
        if (value == null) return ""
        return if (unitSystem == UnitSystem.IMPERIAL) {
            formatDouble(if (isWeight) kgToLbs(value) else cmToInches(value))
        } else {
            formatDouble(value)
        }
    }

    /**
     * Parses body-measurement input back to canonical metric storage:
     * imperial weight parses lb to kg, lengths parse inches to cm.
     */
    fun parseMeasurement(input: String, unitSystem: UnitSystem, isWeight: Boolean): Double? {
        if (input.isEmpty()) return null
        val value = input.toDoubleOrNull() ?: return null
        return if (unitSystem == UnitSystem.IMPERIAL) {
            if (isWeight) lbsToKg(value) else inchesToCm(value)
        } else {
            BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toDouble()
        }
    }

    fun sanitizeDecimalInput(input: String): String {
        val cleaned = buildString {
            var decimalPointSeen = false
            for (ch in input) {
                when {
                    ch.isDigit() -> append(ch)
                    ch == '.' && !decimalPointSeen -> {
                        append('.')
                        decimalPointSeen = true
                    }
                }
            }
        }
        val dotIndex = cleaned.indexOf('.')
        if (dotIndex < 0) return cleaned
        val fraction = cleaned.substring(dotIndex + 1).take(2)
        return if (fraction.isEmpty()) {
            cleaned.substring(0, dotIndex + 1)
        } else {
            cleaned.substring(0, dotIndex + 1) + fraction
        }
    }
}
