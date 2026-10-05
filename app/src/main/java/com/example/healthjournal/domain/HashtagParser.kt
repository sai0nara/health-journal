package com.example.healthjournal.domain

/**
 * Pure extraction of inline hashtags from entry text: `#word` tokens where
 * a word is Unicode letters, digits, or underscores. Matching is
 * case-insensitive with dedupe; tags are stored lowercase. No Android
 * dependencies so parsing stays JVM-testable.
 */
object HashtagParser {

    private val hashtag = Regex("#([\\p{L}\\p{N}_]+)")

    /** Lowercase tags found in [text], in first-seen order. */
    fun extractHashtags(text: String): Set<String> =
        hashtag.findAll(text)
            .map { it.groupValues[1].lowercase(java.util.Locale.ROOT) }
            .toSet()
}

/**
 * System-owned auto-tags by entry kind. Labels are canonical English;
 * display resolution (including Russian) happens at the UI layer.
 */
object EntryKindTag {

    const val FITNESS = "Fitness"
    const val HEALTH = "Health"
    const val MEDICATION = "Medication"

    fun forWorkout(): String = FITNESS

    fun forMeasurement(): String = HEALTH

    fun forMedication(): String = MEDICATION

    /** Tags owned by the system; preserved across edits, never user-removed. */
    val SYSTEM_TAGS: Set<String> = setOf(FITNESS, HEALTH, MEDICATION)
}
