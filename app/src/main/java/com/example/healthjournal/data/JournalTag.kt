package com.example.healthjournal.data

import androidx.annotation.StringRes
import com.example.healthjournal.R
import com.example.healthjournal.domain.TagIdentity

/**
 * Represents the predefined categories for health journal entries.
 *
 * Enum names are persisted tag values and must never change with locale;
 * [labelRes] supplies the localized display label.
 */
enum class JournalTag(@StringRes val labelRes: Int) {
    ILLNESS(R.string.tag_illness),
    CHECKUP(R.string.tag_checkup),
    DOCTOR(R.string.tag_doctor),
    EXERCISES(R.string.tag_exercises),
    FITNESS(R.string.auto_tag_fitness),
    HEALTH(R.string.auto_tag_health),
    MEDICATION(R.string.auto_tag_medication);

    /** Canonical stored value; enum names stay stable identifiers. */
    val canonical: String get() = TagIdentity.canonical(name)
}
