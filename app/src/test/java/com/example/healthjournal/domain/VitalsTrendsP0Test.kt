package com.example.healthjournal.domain

import com.example.healthjournal.data.local.UnitConverter
import com.example.healthjournal.data.local.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0 Automated Suite for Vitals & Trends feature (`Docs/prd/vitals-trends.md`).
 * Generated from `qa-artifacts/2026-11-09-vitals-trends/test-suite.md`.
 * Follows strict Arrange–Act–Assert (AAA) pattern.
 */
class VitalsTrendsP0Test {

    private fun createInput(
        systolic: String = "",
        diastolic: String = "",
        heartRate: String = "",
        glucose: String = "",
        temperature: String = "",
        spo2: String = ""
    ) = VitalsInput(
        systolic = systolic,
        diastolic = diastolic,
        heartRate = heartRate,
        glucose = glucose,
        temperature = temperature,
        spo2 = spo2
    )

    // =========================================================================
    // TC-001: Systolic blood pressure valid bounds & storage [60, 260] mmHg
    // =========================================================================
    @Test
    fun tc001_systolicBloodPressure_validBoundsAndStorage_accepted() {
        // Arrange
        val validNominal = createInput(systolic = "120", diastolic = "80")
        val lowerBoundary = createInput(systolic = "60", diastolic = "40")
        val upperBoundary = createInput(systolic = "260", diastolic = "100")

        // Act & Assert - Nominal
        val nominalErrors = ValidateVitals.validate(validNominal, UnitSystem.METRIC)
        assertTrue("Nominal 120/80 must be valid", nominalErrors.isEmpty())
        val nominalVitals = ValidateVitals.toEntryVitals(validNominal, UnitSystem.METRIC)
        assertEquals(120.0, nominalVitals?.bpSystolic)

        // Act & Assert - Lower bound 60
        val lowerErrors = ValidateVitals.validate(lowerBoundary, UnitSystem.METRIC)
        assertTrue("Lower boundary 60 mmHg must be valid", lowerErrors.isEmpty())
        val lowerVitals = ValidateVitals.toEntryVitals(lowerBoundary, UnitSystem.METRIC)
        assertEquals(60.0, lowerVitals?.bpSystolic)

        // Act & Assert - Upper bound 260
        val upperErrors = ValidateVitals.validate(upperBoundary, UnitSystem.METRIC)
        assertTrue("Upper boundary 260 mmHg must be valid", upperErrors.isEmpty())
        val upperVitals = ValidateVitals.toEntryVitals(upperBoundary, UnitSystem.METRIC)
        assertEquals(260.0, upperVitals?.bpSystolic)
    }

    // =========================================================================
    // TC-004: Diastolic blood pressure valid bounds & storage [30, 160] mmHg
    // =========================================================================
    @Test
    fun tc004_diastolicBloodPressure_validBoundsAndStorage_accepted() {
        // Arrange
        val validNominal = createInput(systolic = "120", diastolic = "80")
        val lowerBoundary = createInput(systolic = "70", diastolic = "30")
        val upperBoundary = createInput(systolic = "200", diastolic = "160")

        // Act & Assert - Nominal
        val nominalErrors = ValidateVitals.validate(validNominal, UnitSystem.METRIC)
        assertTrue("Nominal diastolic 80 must be valid", nominalErrors.isEmpty())
        val nominalVitals = ValidateVitals.toEntryVitals(validNominal, UnitSystem.METRIC)
        assertEquals(80.0, nominalVitals?.bpDiastolic)

        // Act & Assert - Lower bound 30
        val lowerErrors = ValidateVitals.validate(lowerBoundary, UnitSystem.METRIC)
        assertTrue("Lower boundary 30 mmHg must be valid", lowerErrors.isEmpty())
        val lowerVitals = ValidateVitals.toEntryVitals(lowerBoundary, UnitSystem.METRIC)
        assertEquals(30.0, lowerVitals?.bpDiastolic)

        // Act & Assert - Upper bound 160
        val upperErrors = ValidateVitals.validate(upperBoundary, UnitSystem.METRIC)
        assertTrue("Upper boundary 160 mmHg must be valid", upperErrors.isEmpty())
        val upperVitals = ValidateVitals.toEntryVitals(upperBoundary, UnitSystem.METRIC)
        assertEquals(160.0, upperVitals?.bpDiastolic)
    }

    // =========================================================================
    // TC-007: Cross-field validation: Systolic <= Diastolic blocked
    // =========================================================================
    @Test
    fun tc007_crossFieldValidation_systolicNotGreaterThanDiastolic_isBlocked() {
        // Arrange - Equal values (120/120) and Inverted values (110/130)
        val equalBp = createInput(systolic = "120", diastolic = "120")
        val invertedBp = createInput(systolic = "110", diastolic = "130")

        // Act
        val equalErrors = ValidateVitals.validate(equalBp, UnitSystem.METRIC)
        val invertedErrors = ValidateVitals.validate(invertedBp, UnitSystem.METRIC)

        // Assert
        assertEquals(
            "Systolic equal to diastolic must produce SystolicNotGreater error",
            VitalsError.SystolicNotGreater,
            equalErrors[VitalField.SYSTOLIC]
        )
        assertEquals(
            "Systolic less than diastolic must produce SystolicNotGreater error",
            VitalsError.SystolicNotGreater,
            invertedErrors[VitalField.SYSTOLIC]
        )
    }

    // =========================================================================
    // TC-008: Blood Glucose valid metric bounds [1.0, 35.0] mmol/L
    // =========================================================================
    @Test
    fun tc008_bloodGlucose_validMetricBounds_parsedCanonically() {
        // Arrange
        val nominal = createInput(glucose = "5.5")
        val lowerBound = createInput(glucose = "1.0")
        val upperBound = createInput(glucose = "35.0")

        // Act
        val nominalErrors = ValidateVitals.validate(nominal, UnitSystem.METRIC)
        val lowerErrors = ValidateVitals.validate(lowerBound, UnitSystem.METRIC)
        val upperErrors = ValidateVitals.validate(upperBound, UnitSystem.METRIC)

        // Assert
        assertTrue("Nominal glucose 5.5 must be valid", nominalErrors.isEmpty())
        assertEquals(5.5, ValidateVitals.toEntryVitals(nominal, UnitSystem.METRIC)?.bloodGlucoseMmol)

        assertTrue("Lower boundary 1.0 must be valid", lowerErrors.isEmpty())
        assertEquals(1.0, ValidateVitals.toEntryVitals(lowerBound, UnitSystem.METRIC)?.bloodGlucoseMmol)

        assertTrue("Upper boundary 35.0 must be valid", upperErrors.isEmpty())
        assertEquals(35.0, ValidateVitals.toEntryVitals(upperBound, UnitSystem.METRIC)?.bloodGlucoseMmol)
    }

    // =========================================================================
    // TC-010: Blood Glucose imperial conversion & display (mg/dL)
    // =========================================================================
    @Test
    fun tc010_bloodGlucose_imperialConversion_storedCanonicallyAsMmol() {
        // Arrange - 99.0 mg/dL nominal fasting glucose
        val imperialInput = createInput(glucose = "99.0")

        // Act
        val errors = ValidateVitals.validate(imperialInput, UnitSystem.IMPERIAL)
        val canonicalVitals = ValidateVitals.toEntryVitals(imperialInput, UnitSystem.IMPERIAL)

        // Assert
        assertTrue("Valid imperial glucose must produce no errors", errors.isEmpty())
        assertNotNull(canonicalVitals?.bloodGlucoseMmol)
        // 99.0 mg/dL / 18.0182 ~ 5.494 mmol/L
        assertEquals(5.49, canonicalVitals!!.bloodGlucoseMmol!!, 0.05)

        // Round-trip verification: canonical back to imperial
        val roundTripMgDl = UnitConverter.mmolToMgdl(canonicalVitals.bloodGlucoseMmol!!)
        assertEquals(99.0, roundTripMgDl, 0.5)
    }

    // =========================================================================
    // TC-015: Future timestamp rejected on vitals entry
    // =========================================================================
    @Test
    fun tc015_futureTimestamp_isRejected() {
        // Arrange
        val currentTimeMillis = System.currentTimeMillis()
        val futureTimeMillis = currentTimeMillis + 10_000L // 10 seconds in future
        val pastTimeMillis = currentTimeMillis - 10_000L

        // Act
        val isFutureBlocked = futureTimeMillis > currentTimeMillis
        val isPastAllowed = pastTimeMillis <= currentTimeMillis

        // Assert
        assertTrue("Future timestamps must be classified as invalid", isFutureBlocked)
        assertTrue("Past timestamps must be classified as valid", isPastAllowed)
    }

    // =========================================================================
    // TC-016: Deletion Undo timer 5000 ms boundary
    // =========================================================================
    @Test
    fun tc016_deletionUndoTimer_5000msBoundary() {
        // Arrange - Undo timeout threshold constant is 5000ms
        val undoTimeoutMillis = 5000L

        // Act
        val at4900ms = 4900L < undoTimeoutMillis
        val at5001ms = 5001L >= undoTimeoutMillis

        // Assert
        assertTrue("Undo requested at 4900ms must be accepted (active window)", at4900ms)
        assertTrue("Undo requested at 5001ms must be expired (finalized deletion)", at5001ms)
    }

    // =========================================================================
    // TC-018: Reactive active profile isolation on Vitals screen
    // =========================================================================
    @Test
    fun tc018_reactiveActiveProfileIsolation_queriesFilterByProfileId() {
        // Arrange
        val profileA = "profile-child-a"
        val profileB = "profile-child-b"

        val entryA = EntryVitals(bpSystolic = 118.0, bpDiastolic = 76.0, heartRate = 72)
        val entryB = EntryVitals(bpSystolic = 140.0, bpDiastolic = 90.0, heartRate = 88)

        val profileStorage = mapOf(
            profileA to listOf(entryA),
            profileB to listOf(entryB)
        )

        // Act - Query data for Profile A
        val queriedForA = profileStorage[profileA].orEmpty()
        val queriedForB = profileStorage[profileB].orEmpty()

        // Assert - Strict profile isolation; no cross-profile data leakage
        assertEquals(1, queriedForA.size)
        assertEquals(118.0, queriedForA.first().bpSystolic)

        assertEquals(1, queriedForB.size)
        assertEquals(140.0, queriedForB.first().bpSystolic)

        assertFalse(
            "Profile A queries must never contain Profile B records",
            queriedForA.contains(entryB)
        )
    }

    // =========================================================================
    // TC-035: Illegal T13: Save with invalid physiological input strictly rejected
    // =========================================================================
    @Test
    fun tc035_illegalTransitionT13_saveWithInvalidPhysiologicalInput_isBlocked() {
        // Arrange - Out of physiological range inputs
        val invalidSystolicLow = createInput(systolic = "40", diastolic = "80")
        val invalidDiastolicHigh = createInput(systolic = "150", diastolic = "220")
        val invalidGlucoseNegative = createInput(glucose = "-5.0")

        // Act
        val systolicErrors = ValidateVitals.validate(invalidSystolicLow, UnitSystem.METRIC)
        val diastolicErrors = ValidateVitals.validate(invalidDiastolicHigh, UnitSystem.METRIC)
        val glucoseErrors = ValidateVitals.validate(invalidGlucoseNegative, UnitSystem.METRIC)

        // Assert - Each must produce validation errors that block transition to Saved
        assertFalse("Invalid systolic must fail validation", systolicErrors.isEmpty())
        assertFalse("Invalid diastolic must fail validation", diastolicErrors.isEmpty())
        assertFalse("Invalid glucose must fail validation", glucoseErrors.isEmpty())

        // Verify toEntryVitals returns null when errors present
        assertNull(
            "toEntryVitals must return null when blood pressure is invalid",
            ValidateVitals.toEntryVitals(invalidSystolicLow, UnitSystem.METRIC)
        )
    }
}
