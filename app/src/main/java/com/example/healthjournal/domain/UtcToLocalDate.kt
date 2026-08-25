package com.example.healthjournal.domain

import java.util.Calendar
import java.util.TimeZone

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
     *
     * Reading the components with a UTC calendar and re-applying them via a
     * default-timezone calendar prevents the off-by-one-day shift that occurs
     * in negative UTC offsets when raw UTC-midnight millis are displayed or
     * stored as-is.
     */
    fun toLocalMillis(utcMillis: Long): Long {
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        return Calendar.getInstance().apply {
            clear()
            set(
                utcCalendar.get(Calendar.YEAR),
                utcCalendar.get(Calendar.MONTH),
                utcCalendar.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
        }.timeInMillis
    }
}
