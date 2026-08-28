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
}
