package com.example.healthjournal.ui.components

import com.example.healthjournal.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Stored tag values arrive either as capitalized system auto-tags ("Health")
 * or as journal-chip enum names ("HEALTH"); both must resolve to the same
 * localized label, and free-form user tags must fall through to the
 * capitalized-raw fallback (null resource).
 */
class TagLabelResTest {

    @Test
    fun chipEnumNames_resolveToLabelRes() {
        assertEquals(R.string.auto_tag_health, tagLabelRes("HEALTH"))
        assertEquals(R.string.auto_tag_fitness, tagLabelRes("FITNESS"))
        assertEquals(R.string.auto_tag_medication, tagLabelRes("MEDICATION"))
        assertEquals(R.string.tag_illness, tagLabelRes("ILLNESS"))
        assertEquals(R.string.tag_checkup, tagLabelRes("CHECKUP"))
        assertEquals(R.string.tag_doctor, tagLabelRes("DOCTOR"))
        assertEquals(R.string.tag_exercises, tagLabelRes("EXERCISES"))
    }

    @Test
    fun storedAutoTagValues_resolveCaseInsensitively() {
        assertEquals(R.string.auto_tag_health, tagLabelRes("Health"))
        assertEquals(R.string.auto_tag_fitness, tagLabelRes("fitness"))
        assertEquals(R.string.auto_tag_medication, tagLabelRes("Medication"))
    }

    @Test
    fun freeFormUserTags_haveNoResource() {
        assertNull(tagLabelRes("holiday"))
        assertNull(tagLabelRes(""))
    }
}
