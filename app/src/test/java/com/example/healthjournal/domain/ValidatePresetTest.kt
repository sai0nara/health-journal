package com.example.healthjournal.domain

import com.example.healthjournal.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidatePresetTest {

    @Test
    fun validateWarmupName_blank_isRejected() {
        assertEquals(
            R.string.preset_warmup_error_name_required,
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
            R.string.preset_warmup_error_name_too_long,
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
            R.string.preset_warmup_error_duration_range,
            ValidatePreset.validateWarmupDuration(0)
        )
        assertEquals(
            R.string.preset_warmup_error_duration_range,
            ValidatePreset.validateWarmupDuration(61)
        )
    }
}
