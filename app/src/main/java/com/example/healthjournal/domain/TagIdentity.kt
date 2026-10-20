package com.example.healthjournal.domain

import java.util.Locale

/**
 * Single canonical form for stored tags: Unicode lowercase under
 * [Locale.ROOT]. Presets, inline hashtags, system auto-tags, sync and restore
 * all pass through here so one concept maps to one stored value.
 */
object TagIdentity {

    /** Canonical stored value for [tag]; empty for blank input. */
    fun canonical(tag: String): String = tag.trim().lowercase(Locale.ROOT)

    /**
     * Canonicalizes [tags], dropping blanks and case-variant duplicates while
     * keeping first-seen order.
     */
    fun canonicalDistinct(tags: Iterable<String>): List<String> =
        tags.map { canonical(it) }
            .filter { it.isNotEmpty() }
            .distinct()
}
