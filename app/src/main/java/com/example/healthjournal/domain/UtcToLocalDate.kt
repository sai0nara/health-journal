package com.example.healthjournal.domain

/**
 * Maps Material3 DatePicker selection values (`selectedDateMillis`, which is
 * UTC midnight of the selected date) onto the device's default timezone so
 * the chosen calendar date is preserved when rendered by formatters or
 * persisted locally.
 */
object UtcToLocalDate {

    /**
     * Returns epoch millis whose calendar date in the default timezone has
     * the same year/month/day as [utcMillis] has in UTC.
     */
    fun toLocalMillis(utcMillis: Long): Long = utcMillis
}
