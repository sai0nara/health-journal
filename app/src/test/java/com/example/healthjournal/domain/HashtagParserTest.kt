package com.example.healthjournal.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for hashtag extraction and kind-based auto-tags.
 * RED until HashtagParser and EntryKindTag exist in domain.
 */
class HashtagParserTest {

    @Test
    fun plainText_yieldsNoTags() {
        assertTrue(HashtagParser.extractHashtags("Morning jog, felt great").isEmpty())
    }

    @Test
    fun singleHashtag_isExtractedLowercased() {
        assertEquals(setOf("recovery"), HashtagParser.extractHashtags("Evening #Recovery walk"))
    }

    @Test
    fun duplicateTags_differingOnlyByCase_yieldOneTag() {
        assertEquals(
            setOf("recovery"),
            HashtagParser.extractHashtags("#Recovery and #recovery")
        )
    }

    @Test
    fun multipleHashtags_allExtracted() {
        assertEquals(
            setOf("fitness", "morning"),
            HashtagParser.extractHashtags("#fitness then #morning stretch")
        )
    }

    @Test
    fun cyrillicHashtag_isExtracted() {
        assertEquals(
            setOf("тренировка"),
            HashtagParser.extractHashtags("Утренняя #Тренировка")
        )
    }

    @Test
    fun digitsAndUnderscores_areTagCharacters() {
        assertEquals(
            setOf("week2", "leg_day"),
            HashtagParser.extractHashtags("#week2 #leg_day done")
        )
    }

    @Test
    fun loneHash_yieldsNoTags() {
        assertTrue(HashtagParser.extractHashtags("C# programming # done").isEmpty())
    }

    @Test
    fun workoutKind_mapsToFitness() {
        assertEquals("Fitness", EntryKindTag.forWorkout())
    }

    @Test
    fun measurementKind_mapsToHealth() {
        assertEquals("Health", EntryKindTag.forMeasurement())
    }

    @Test
    fun medicationKind_mapsToMedication() {
        assertEquals("Medication", EntryKindTag.forMedication())
    }
}
