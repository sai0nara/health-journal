package com.example.healthjournal.data.local

import org.junit.Assert.*
import org.junit.Test

class UnitConverterTest {
    @Test
    fun `cm to inches conversion is accurate`() {
        val inches = UnitConverter.cmToInches(175.0)
        assertEquals(68.9, inches, 0.1)
    }

    @Test
    fun `inches to cm conversion is accurate`() {
        val cm = UnitConverter.inchesToCm(70.0)
        assertEquals(177.8, cm, 0.1)
    }

    @Test
    fun `kg to lbs conversion is accurate`() {
        val lbs = UnitConverter.kgToLbs(70.0)
        assertEquals(154.3, lbs, 0.1)
    }

    @Test
    fun `lbs to kg conversion is accurate`() {
        val kg = UnitConverter.lbsToKg(150.0)
        assertEquals(68.04, kg, 0.01)
    }

    @Test
    fun `formatForDisplay metric height shows cm`() {
        val result = UnitConverter.formatForDisplay(175.0, UnitSystem.METRIC, isHeight = true)
        assertEquals("175", result)
    }

    @Test
    fun `formatForDisplay imperial height shows inches`() {
        val result = UnitConverter.formatForDisplay(177.8, UnitSystem.IMPERIAL, isHeight = true)
        assertEquals("70", result)
    }

    @Test
    fun `parseInput metric returns value directly`() {
        val result = UnitConverter.parseInput("175", UnitSystem.METRIC, isHeight = true)
        assertEquals(175.0, result!!, 0.001)
    }

    @Test
    fun `parseInput metric rounds to two decimals`() {
        val result = UnitConverter.parseInput("178.35745332432423", UnitSystem.METRIC, isHeight = true)
        assertEquals(178.36, result!!, 0.001)
    }

    @Test
    fun `formatDouble caps at two decimals`() {
        assertEquals("178.36", UnitConverter.formatDouble(178.35745332432423))
        assertEquals("3.14", UnitConverter.formatDouble(3.14159265358979))
    }

    @Test
    fun `formatDouble strips trailing zeros`() {
        assertEquals("178", UnitConverter.formatDouble(178.0))
        assertEquals("178.3", UnitConverter.formatDouble(178.30))
        assertEquals("178.35", UnitConverter.formatDouble(178.35))
    }

    @Test
    fun `sanitizeDecimalInput trims to two decimals`() {
        assertEquals("178.55", UnitConverter.sanitizeDecimalInput("178.5555555"))
        assertEquals("85.5", UnitConverter.sanitizeDecimalInput("85.5"))
        assertEquals("85.", UnitConverter.sanitizeDecimalInput("85."))
        assertEquals("1.23", UnitConverter.sanitizeDecimalInput("1.2.3"))
        assertEquals("", UnitConverter.sanitizeDecimalInput("abc"))
    }

    @Test
    fun `parseInput imperial converts to cm`() {
        val result = UnitConverter.parseInput("70", UnitSystem.IMPERIAL, isHeight = true)
        assertEquals(177.8, result!!, 0.1)
    }

    @Test
    fun `cmToFeetInches splits 177_8cm into 5ft 10in`() {
        val result = UnitConverter.cmToFeetInches(177.8)
        assertEquals(5, result.feet)
        assertEquals(10.0, result.inches, 0.001)
    }

    @Test
    fun `cmToFeetInches rounds sub-inch remainder to one decimal`() {
        val result = UnitConverter.cmToFeetInches(180.0)
        assertEquals(5, result.feet)
        assertEquals(10.9, result.inches, 0.001)
    }

    @Test
    fun `feetInchesToCm combines 5ft 10in into 177_8cm`() {
        assertEquals(177.8, UnitConverter.feetInchesToCm(5, 10.0), 0.001)
    }

    @Test
    fun `feetInches round trip stays within display rounding`() {
        val split = UnitConverter.cmToFeetInches(180.0)
        assertEquals(180.1, UnitConverter.feetInchesToCm(split.feet, split.inches), 0.001)
    }

    @Test
    fun `formatMeasurement imperial weight shows lbs`() {
        assertEquals("154.3", UnitConverter.formatMeasurement(70.0, UnitSystem.IMPERIAL, isWeight = true))
    }

    @Test
    fun `formatMeasurement imperial length shows inches`() {
        assertEquals("39.4", UnitConverter.formatMeasurement(100.0, UnitSystem.IMPERIAL, isWeight = false))
    }

    @Test
    fun `formatMeasurement metric passes value through`() {
        assertEquals("70", UnitConverter.formatMeasurement(70.0, UnitSystem.METRIC, isWeight = true))
        assertEquals("100", UnitConverter.formatMeasurement(100.0, UnitSystem.METRIC, isWeight = false))
    }

    @Test
    fun `formatMeasurement null returns blank`() {
        assertEquals("", UnitConverter.formatMeasurement(null, UnitSystem.IMPERIAL, isWeight = true))
    }

    @Test
    fun `parseMeasurement imperial weight converts to kg`() {
        // 154.3 lb × 0.45359237 = 69.9893 → 69.99: the 1-decimal display
        // rounding costs a hundredth of a kilo on the round trip.
        assertEquals(69.99, UnitConverter.parseMeasurement("154.3", UnitSystem.IMPERIAL, isWeight = true)!!, 0.001)
    }

    @Test
    fun `parseMeasurement imperial length converts to cm`() {
        assertEquals(100.1, UnitConverter.parseMeasurement("39.4", UnitSystem.IMPERIAL, isWeight = false)!!, 0.001)
    }

    @Test
    fun `parseMeasurement metric rounds to two decimals`() {
        assertEquals(70.57, UnitConverter.parseMeasurement("70.567", UnitSystem.METRIC, isWeight = true)!!, 0.001)
    }

    @Test
    fun `parseMeasurement blank or non-numeric returns null`() {
        assertNull(UnitConverter.parseMeasurement("", UnitSystem.IMPERIAL, isWeight = true))
        assertNull(UnitConverter.parseMeasurement("abc", UnitSystem.METRIC, isWeight = false))
    }

    @Test
    fun `mmol to mgdl conversion is accurate`() {
        assertEquals(97.3, UnitConverter.mmolToMgdl(5.4), 0.01)
    }

    @Test
    fun `mgdl to mmol conversion is accurate`() {
        assertEquals(5.38, UnitConverter.mgdlToMmol(97.0), 0.01)
    }

    @Test
    fun `glucose display round trip stays within one mgdl`() {
        val mmol = UnitConverter.mgdlToMmol(97.0)
        val mgdl = UnitConverter.mmolToMgdl(mmol)

        assertEquals(97.0, mgdl, 1.0)
    }

    @Test
    fun `celsius to fahrenheit conversion is accurate`() {
        assertEquals(97.9, UnitConverter.celsiusToFahrenheit(36.6), 0.01)
    }

    @Test
    fun `fahrenheit to celsius conversion is accurate`() {
        assertEquals(37.0, UnitConverter.fahrenheitToCelsius(98.6), 0.01)
    }

    @Test
    fun `temperature display round trip stays within one decimal`() {
        val celsius = UnitConverter.fahrenheitToCelsius(98.6)
        val fahrenheit = UnitConverter.celsiusToFahrenheit(celsius)

        assertEquals(98.6, fahrenheit, 0.1)
    }
}
