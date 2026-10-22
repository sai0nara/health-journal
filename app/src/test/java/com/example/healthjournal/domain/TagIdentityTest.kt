package com.example.healthjournal.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the canonical tag identity: every source (preset chips,
 * inline hashtags, system auto-tags and restored/synced values) must map to
 * one Unicode-lowercase stored value.
 */
class TagIdentityTest {

    @Test
    fun canonical_lowercasesPresetAndAutoTagVariants() {
        assertEquals("fitness", TagIdentity.canonical("FITNESS"))
        assertEquals("fitness", TagIdentity.canonical("Fitness"))
        assertEquals("fitness", TagIdentity.canonical("fitness"))
    }

    @Test
    fun canonical_lowercasesCyrillic() {
        assertEquals("тренировка", TagIdentity.canonical("ТРЕНИРОВКА"))
    }

    @Test
    fun canonical_turkishDottedI_usesRootLocale() {
        // Root-locale lowercasing yields "i" plus a combining dot, never the
        // Turkish dotless "ı".
        assertEquals("i\u0307", TagIdentity.canonical("İ"))
    }

    @Test
    fun canonical_freeFormTag_lowercasesWithoutChangingCharacters() {
        assertEquals("morningrun", TagIdentity.canonical("MorningRun"))
        assertEquals("leg_day2", TagIdentity.canonical("Leg_Day2"))
    }

    @Test
    fun canonical_isIdempotent() {
        assertEquals(TagIdentity.canonical("FITNESS"), TagIdentity.canonical(TagIdentity.canonical("FITNESS")))
    }

    @Test
    fun canonicalDistinct_collapsesCaseVariants() {
        assertEquals(
            listOf("fitness"),
            TagIdentity.canonicalDistinct(listOf("FITNESS", "fitness", "Fitness"))
        )
    }

    @Test
    fun canonicalDistinct_keepsDistinctTagsInFirstSeenOrder() {
        assertEquals(
            listOf("morningrun", "evening", "leg_day"),
            TagIdentity.canonicalDistinct(listOf("MorningRun", "evening", "Leg_Day", "EVENING"))
        )
    }

    @Test
    fun canonicalDistinct_dropsBlankTags() {
        assertEquals(
            listOf("fitness"),
            TagIdentity.canonicalDistinct(listOf("", "  ", "Fitness"))
        )
    }
}
