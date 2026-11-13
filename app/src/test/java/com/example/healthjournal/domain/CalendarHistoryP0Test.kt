package com.example.healthjournal.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * P0 Automated Unit Tests for Calendar History (`Docs/prd/calendar-history.md`).
 * Generated from `qa-artifacts/2026-11-10-calendar-history/test-suite.md`.
 * Follows strict Arrange–Act–Assert (AAA) pattern.
 */
class CalendarHistoryP0Test {

    private val utcZone = ZoneId.of("UTC")
    private val newYorkZone = ZoneId.of("America/New_York")
    private val moscowZone = ZoneId.of("Europe/Moscow")

    // =========================================================================
    // TC-001: Single-day filter includes exact start and end of local day
    // =========================================================================
    @Test
    fun tc001_singleDayFilter_exactStartAndEndOfDay_includedInEpochRange() {
        // Arrange
        val date = LocalDate.of(2026, 11, 10)
        val filter = HistoryDateFilter.SingleDay(date)

        // Act
        val range = filter.toEpochRange(newYorkZone)

        // Assert
        val startExpected = ZonedDateTime.of(2026, 11, 10, 0, 0, 0, 0, newYorkZone).toInstant().toEpochMilli()
        val endExpected = ZonedDateTime.of(2026, 11, 10, 23, 59, 59, 999_000_000, newYorkZone).toInstant().toEpochMilli()

        assertEquals("Range start must be exact local midnight (00:00:00.000)", startExpected, range.startMillis)
        assertEquals("Range end must be exact local 23:59:59.999", endExpected, range.endMillis)
        assertTrue("Start timestamp must be within range", range.contains(startExpected))
        assertTrue("Midday timestamp must be within range", range.contains(startExpected + 12 * 3600 * 1000L))
        assertTrue("End timestamp must be within range", range.contains(endExpected))
    }

    // =========================================================================
    // TC-002: Single-day lower boundary excludes 23:59:59.999 of previous day
    // =========================================================================
    @Test
    fun tc002_singleDayFilter_lowerBoundaryExcludesPreviousDayLastMillisecond() {
        // Arrange
        val date = LocalDate.of(2026, 11, 10)
        val filter = HistoryDateFilter.SingleDay(date)
        val range = filter.toEpochRange(newYorkZone)

        val previousDayLastMillis = range.startMillis - 1
        val firstMillisOfTargetDay = range.startMillis

        // Act & Assert
        assertFalse("Previous day 23:59:59.999 must be excluded", range.contains(previousDayLastMillis))
        assertTrue("Target day 00:00:00.000 must be included", range.contains(firstMillisOfTargetDay))
        assertTrue("Target day 00:00:00.001 must be included", range.contains(firstMillisOfTargetDay + 1))
    }

    // =========================================================================
    // TC-003: Single-day upper boundary excludes 00:00:00.000 of next day
    // =========================================================================
    @Test
    fun tc003_singleDayFilter_upperBoundaryExcludesNextDayFirstMillisecond() {
        // Arrange
        val date = LocalDate.of(2026, 11, 10)
        val filter = HistoryDateFilter.SingleDay(date)
        val range = filter.toEpochRange(newYorkZone)

        val lastMillisOfTargetDay = range.endMillis
        val nextDayFirstMillis = range.endMillis + 1

        // Act & Assert
        assertTrue("Target day 23:59:59.998 must be included", range.contains(lastMillisOfTargetDay - 1))
        assertTrue("Target day 23:59:59.999 must be included", range.contains(lastMillisOfTargetDay))
        assertFalse("Next day 00:00:00.000 must be excluded", range.contains(nextDayFirstMillis))
    }

    // =========================================================================
    // TC-004: Multi-day period filter includes entries across [startDate, endDate]
    // =========================================================================
    @Test
    fun tc004_multiDayPeriod_includesEntriesAcrossDateRange() {
        // Arrange
        val start = LocalDate.of(2026, 11, 1)
        val end = LocalDate.of(2026, 11, 10)
        val filter = HistoryDateFilter.Period(start, end)

        // Act
        val range = filter.toEpochRange(newYorkZone)
        val day1Millis = ZonedDateTime.of(2026, 11, 1, 10, 0, 0, 0, newYorkZone).toInstant().toEpochMilli()
        val day5Millis = ZonedDateTime.of(2026, 11, 5, 14, 0, 0, 0, newYorkZone).toInstant().toEpochMilli()
        val day10Millis = ZonedDateTime.of(2026, 11, 10, 20, 0, 0, 0, newYorkZone).toInstant().toEpochMilli()
        val day11Millis = ZonedDateTime.of(2026, 11, 11, 8, 0, 0, 0, newYorkZone).toInstant().toEpochMilli()

        // Assert
        assertTrue("Day 1 entry within range", range.contains(day1Millis))
        assertTrue("Day 5 entry within range", range.contains(day5Millis))
        assertTrue("Day 10 entry within range", range.contains(day10Millis))
        assertFalse("Day 11 entry outside range", range.contains(day11Millis))
    }

    // =========================================================================
    // TC-005: Multi-day period start and end millisecond boundary precision
    // =========================================================================
    @Test
    fun tc005_multiDayPeriod_exactStartAndEndMillisecondBoundaries() {
        // Arrange
        val start = LocalDate.of(2026, 11, 1)
        val end = LocalDate.of(2026, 11, 10)
        val filter = HistoryDateFilter.Period(start, end)

        // Act
        val range = filter.toEpochRange(utcZone)

        // Assert
        val expectedStart = ZonedDateTime.of(2026, 11, 1, 0, 0, 0, 0, utcZone).toInstant().toEpochMilli()
        val expectedEnd = ZonedDateTime.of(2026, 11, 10, 23, 59, 59, 999_000_000, utcZone).toInstant().toEpochMilli()

        assertEquals(expectedStart, range.startMillis)
        assertEquals(expectedEnd, range.endMillis)
        assertFalse("Oct 31 23:59:59.999 must be excluded", range.contains(expectedStart - 1))
        assertTrue("Nov 1 00:00:00.000 must be included", range.contains(expectedStart))
        assertTrue("Nov 10 23:59:59.999 must be included", range.contains(expectedEnd))
        assertFalse("Nov 11 00:00:00.000 must be excluded", range.contains(expectedEnd + 1))
    }

    // =========================================================================
    // TC-007: Inverted period end date (endDate < startDate) auto-normalizes
    // =========================================================================
    @Test
    fun tc007_invertedPeriod_autoNormalizesStartDateAndEndDate() {
        // Arrange & Act
        val start = LocalDate.of(2026, 11, 10)
        val end = LocalDate.of(2026, 11, 3)
        val filter = HistoryDateFilter.periodOf(startDate = start, endDate = end)

        // Assert
        assertEquals("Normalized start date must be the earlier date", LocalDate.of(2026, 11, 3), filter.startDate)
        assertEquals("Normalized end date must be the later date", LocalDate.of(2026, 11, 10), filter.endDate)
        assertTrue("startDate <= endDate invariant must hold", filter.startDate <= filter.endDate)
    }

    // =========================================================================
    // TC-021: Timezone offset safety (negative and positive UTC offsets)
    // =========================================================================
    @Test
    fun tc021_timezoneOffsets_negativeAndPositiveLocalMidnightSafety() {
        // Arrange
        val date = LocalDate.of(2026, 11, 10)
        val filter = HistoryDateFilter.SingleDay(date)

        // Act
        val rangeNY = filter.toEpochRange(newYorkZone)
        val rangeMoscow = filter.toEpochRange(moscowZone)

        // Assert
        // In NY (UTC-5), 2026-11-10 00:00:00 is 2026-11-10 05:00:00 UTC
        val nyStartUtc = ZonedDateTime.of(2026, 11, 10, 0, 0, 0, 0, newYorkZone).toInstant().toEpochMilli()
        assertEquals(nyStartUtc, rangeNY.startMillis)

        // In Moscow (UTC+3), 2026-11-10 00:00:00 is 2026-11-09 21:00:00 UTC
        val moscowStartUtc = ZonedDateTime.of(2026, 11, 10, 0, 0, 0, 0, moscowZone).toInstant().toEpochMilli()
        assertEquals(moscowStartUtc, rangeMoscow.startMillis)

        // Both local ranges span exactly 24 hours (86,400,000 ms - 1 ms for inclusive end)
        assertEquals(86_400_000L - 1, rangeNY.endMillis - rangeNY.startMillis)
        assertEquals(86_400_000L - 1, rangeMoscow.endMillis - rangeMoscow.startMillis)
    }

    // =========================================================================
    // TC-022: DST Spring-Forward (23-hour day) midnight boundary calculation
    // =========================================================================
    @Test
    fun tc022_dstSpringForward_23HourDayHandledAccurately() {
        // Arrange: America/New_York DST start in 2026 is Sunday, March 8 (clocks jump 02:00 -> 03:00)
        val dstSpringDate = LocalDate.of(2026, 3, 8)
        val filter = HistoryDateFilter.SingleDay(dstSpringDate)

        // Act
        val range = filter.toEpochRange(newYorkZone)

        // Assert: 23 hours in milliseconds = 23 * 3600 * 1000 = 82,800,000 ms
        val expectedDurationMillis = 23 * 3_600_000L
        val actualDurationMillis = (range.endMillis - range.startMillis) + 1

        assertEquals("DST Spring-forward day must span exactly 23 hours", expectedDurationMillis, actualDurationMillis)
    }

    // =========================================================================
    // TC-023: DST Fall-Back (25-hour day) midnight boundary calculation
    // =========================================================================
    @Test
    fun tc023_dstFallBack_25HourDayHandledAccurately() {
        // Arrange: America/New_York DST end in 2026 is Sunday, November 1 (clocks jump 02:00 -> 01:00)
        val dstFallDate = LocalDate.of(2026, 11, 1)
        val filter = HistoryDateFilter.SingleDay(dstFallDate)

        // Act
        val range = filter.toEpochRange(newYorkZone)

        // Assert: 25 hours in milliseconds = 25 * 3600 * 1000 = 90,000,000 ms
        val expectedDurationMillis = 25 * 3_600_000L
        val actualDurationMillis = (range.endMillis - range.startMillis) + 1

        assertEquals("DST Fall-back day must span exactly 25 hours", expectedDurationMillis, actualDurationMillis)
    }

    // =========================================================================
    // TC-030: Domain guard against direct inverted Period construction
    // =========================================================================
    @Test
    fun tc030_domainGuard_periodNormalizedOrThrowsWhenInverted() {
        // Arrange & Act
        val period = HistoryDateFilter.periodOf(
            startDate = LocalDate.of(2026, 11, 10),
            endDate = LocalDate.of(2026, 11, 3)
        )

        // Assert
        assertTrue("startDate must be <= endDate after normalization", period.startDate <= period.endDate)
        assertEquals(LocalDate.of(2026, 11, 3), period.startDate)
        assertEquals(LocalDate.of(2026, 11, 10), period.endDate)
    }

    // =========================================================================
    // Quick Presets Verification (P4, R4)
    // =========================================================================
    @Test
    fun quickPresets_resolveAccuratelyRelativeToTargetDate() {
        // Arrange
        val anchorDate = LocalDate.of(2026, 11, 10)

        // Act
        val todayFilter = HistoryDateFilter.preset(DatePreset.TODAY, anchorDate)
        val yesterdayFilter = HistoryDateFilter.preset(DatePreset.YESTERDAY, anchorDate)
        val last7DaysFilter = HistoryDateFilter.preset(DatePreset.LAST_7_DAYS, anchorDate)
        val last30DaysFilter = HistoryDateFilter.preset(DatePreset.LAST_30_DAYS, anchorDate)

        // Assert
        assertEquals(HistoryDateFilter.SingleDay(anchorDate), todayFilter)
        assertEquals(HistoryDateFilter.SingleDay(anchorDate.minusDays(1)), yesterdayFilter)
        assertEquals(HistoryDateFilter.Period(anchorDate.minusDays(6), anchorDate), last7DaysFilter)
        assertEquals(HistoryDateFilter.Period(anchorDate.minusDays(29), anchorDate), last30DaysFilter)
    }
}
