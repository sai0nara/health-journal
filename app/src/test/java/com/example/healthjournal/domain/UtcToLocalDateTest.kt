package com.example.healthjournal.domain

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Unit tests for UtcToLocalDate, verifying that UTC-midnight epoch millis
 * emitted by the Material3 DatePicker are remapped so the SAME calendar
 * date appears in the device's default timezone. Guards against the
 * off-by-one-day display bug in negative UTC-offset timezones.
 */
class UtcToLocalDateTest {

    private val originalTimeZone: TimeZone = TimeZone.getDefault()

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    private fun useDeviceTimeZone(zoneId: String) {
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId))
    }

    private fun utcMidnightMillis(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, day, 0, 0, 0)
        }.timeInMillis

    private fun localDateString(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))

    @Test
    fun negativeOffsetSevenHours_mapsToSameLocalDate() {
        useDeviceTimeZone("America/Phoenix")

        val result = UtcToLocalDate.toLocalMillis(utcMidnightMillis(2026, 8, 23))

        assertEquals("2026-08-23", localDateString(result))
    }

    @Test
    fun negativeOffsetEightHours_winterDate_mapsToSameLocalDate() {
        useDeviceTimeZone("America/Los_Angeles")

        val result = UtcToLocalDate.toLocalMillis(utcMidnightMillis(2026, 1, 15))

        assertEquals("2026-01-15", localDateString(result))
    }

    @Test
    fun negativeOffsetFiveHours_mapsToSameLocalDate() {
        useDeviceTimeZone("America/New_York")

        val result = UtcToLocalDate.toLocalMillis(utcMidnightMillis(2026, 1, 20))

        assertEquals("2026-01-20", localDateString(result))
    }

    @Test
    fun positiveOffsetTwoHours_mapsToSameLocalDate() {
        useDeviceTimeZone("Europe/Berlin")

        val result = UtcToLocalDate.toLocalMillis(utcMidnightMillis(2026, 8, 23))

        assertEquals("2026-08-23", localDateString(result))
    }

    @Test
    fun dstSpringForwardBoundary_datesOnBothSides_mapCorrectly() {
        useDeviceTimeZone("America/New_York")

        val beforeTransition = UtcToLocalDate.toLocalMillis(utcMidnightMillis(2026, 3, 7))
        val afterTransition = UtcToLocalDate.toLocalMillis(utcMidnightMillis(2026, 3, 9))

        assertEquals("2026-03-07", localDateString(beforeTransition))
        assertEquals("2026-03-09", localDateString(afterTransition))
    }

    @Test
    fun alreadyLocalTimestamp_roundTripsThroughConversion() {
        useDeviceTimeZone("America/New_York")

        val noonLocal = Calendar.getInstance().apply {
            clear()
            set(2026, 7, 23, 12, 0, 0)
        }.timeInMillis

        val result = UtcToLocalDate.toLocalMillis(noonLocal)

        assertEquals("2026-08-23", localDateString(result))
    }
}
