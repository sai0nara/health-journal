package com.example.healthjournal.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidatePresetTest {

    @Test
    fun validateWarmupName_blank_isRejected() {
        assertEquals(
            ValidatePreset.ERROR_WARMUP_NAME_REQUIRED,
            ValidatePreset.validateWarmupName("   ")
        )
    }

    @Test
    fun validateWarmupName_acceptsSixtyCharacters() {
        assertNull(ValidatePreset.validateWarmupName("a".repeat(60)))
    }

    @Test
    fun validateWarmupName_rejectsSixtyOneCharacters() {
        assertEquals(
            ValidatePreset.ERROR_WARMUP_NAME_TOO_LONG,
            ValidatePreset.validateWarmupName("a".repeat(61))
        )
    }

    @Test
    fun validateWarmupDuration_acceptsBounds() {
        assertNull(ValidatePreset.validateWarmupDuration(1))
        assertNull(ValidatePreset.validateWarmupDuration(60))
    }

    @Test
    fun validateWarmupDuration_rejectsOutOfRange() {
        assertEquals(
            ValidatePreset.ERROR_WARMUP_DURATION_RANGE,
            ValidatePreset.validateWarmupDuration(0)
        )
        assertEquals(
            ValidatePreset.ERROR_WARMUP_DURATION_RANGE,
            ValidatePreset.validateWarmupDuration(61)
        )
    }
}
