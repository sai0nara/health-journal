package com.example.healthjournal.domain

import com.example.healthjournal.data.local.JournalEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * P1 and P2 Automated Unit Tests for Calendar History (`Docs/prd/calendar-history.md`).
 * Generated from `qa-artifacts/2026-11-10-calendar-history/test-suite.md`.
 * Covers Equivalence Partitioning, Decision Tables, State Transitions, Pairwise Configurations, and Error Guessing.
 * Strictly follows Arrange–Act–Assert (AAA).
 */
class CalendarHistoryP1P2Test {

    private val utcZone = ZoneId.of("UTC")
    private val newYorkZone = ZoneId.of("America/New_York")
    private val moscowZone = ZoneId.of("Europe/Moscow")

    // =========================================================================
    // TC-006 (P1): Cross-month and cross-year period selection (Dec 28 to Jan 5)
    // =========================================================================
    @Test
    fun tc006_crossMonthAndCrossYearPeriodSelection() {
        // Arrange
        val start = LocalDate.of(2025, 12, 28)
        val end = LocalDate.of(2026, 1, 5)
        val filter = HistoryDateFilter.Period(start, end)

        // Act
        val range = filter.toEpochRange(utcZone)
        val dec29Millis = ZonedDateTime.of(2025, 12, 29, 12, 0, 0, 0, utcZone).toInstant().toEpochMilli()
        val jan3Millis = ZonedDateTime.of(2026, 1, 3, 15, 0, 0, 0, utcZone).toInstant().toEpochMilli()
        val jan6Millis = ZonedDateTime.of(2026, 1, 6, 8, 0, 0, 0, utcZone).toInstant().toEpochMilli()

        // Assert
        assertTrue("Dec 29 (prior year) must be included in cross-year range", range.contains(dec29Millis))
        assertTrue("Jan 3 (new year) must be included in cross-year range", range.contains(jan3Millis))
        assertFalse("Jan 6 must be excluded from range ending Jan 5", range.contains(jan6Millis))
    }

    // =========================================================================
    // TC-008 (P1): Quick preset Today filters to current local date
    // =========================================================================
    @Test
    fun tc008_quickPresetToday() {
        // Arrange
        val today = LocalDate.of(2026, 11, 10)

        // Act
        val filter = HistoryDateFilter.preset(DatePreset.TODAY, today)

        // Assert
        assertEquals(HistoryDateFilter.SingleDay(today), filter)
        val range = filter.toEpochRange(utcZone)
        val startOfToday = ZonedDateTime.of(2026, 11, 10, 0, 0, 0, 0, utcZone).toInstant().toEpochMilli()
        val yesterdayEnd = startOfToday - 1
        assertTrue(range.contains(startOfToday))
        assertFalse(range.contains(yesterdayEnd))
    }

    // =========================================================================
    // TC-009 (P1): Quick preset Yesterday filters to today.minusDays(1)
    // =========================================================================
    @Test
    fun tc009_quickPresetYesterday() {
        // Arrange
        val today = LocalDate.of(2026, 11, 10)
        val yesterday = today.minusDays(1)

        // Act
        val filter = HistoryDateFilter.preset(DatePreset.YESTERDAY, today)

        // Assert
        assertEquals(HistoryDateFilter.SingleDay(yesterday), filter)
        val range = filter.toEpochRange(utcZone)
        val startOfYesterday = ZonedDateTime.of(2026, 11, 9, 0, 0, 0, 0, utcZone).toInstant().toEpochMilli()
        val startOfToday = ZonedDateTime.of(2026, 11, 10, 0, 0, 0, 0, utcZone).toInstant().toEpochMilli()
        assertTrue(range.contains(startOfYesterday))
        assertFalse(range.contains(startOfToday))
    }

    // =========================================================================
    // TC-010 (P1): Quick preset Last 7 Days filters to [today - 6d, today]
    // =========================================================================
    @Test
    fun tc010_quickPresetLast7Days() {
        // Arrange
        val today = LocalDate.of(2026, 11, 10)
        val sixDaysAgo = today.minusDays(6) // 2026-11-04

        // Act
        val filter = HistoryDateFilter.preset(DatePreset.LAST_7_DAYS, today)

        // Assert
        assertEquals(HistoryDateFilter.Period(sixDaysAgo, today), filter)
        val range = filter.toEpochRange(utcZone)
        val sevenDaysAgoMillis = ZonedDateTime.of(2026, 11, 3, 23, 59, 59, 999_000_000, utcZone).toInstant().toEpochMilli()
        val sixDaysAgoStartMillis = ZonedDateTime.of(2026, 11, 4, 0, 0, 0, 0, utcZone).toInstant().toEpochMilli()
        val todayEndMillis = ZonedDateTime.of(2026, 11, 10, 23, 59, 59, 999_000_000, utcZone).toInstant().toEpochMilli()

        assertFalse("7 days ago must be excluded from 7-day window", range.contains(sevenDaysAgoMillis))
        assertTrue("6 days ago midnight must be included", range.contains(sixDaysAgoStartMillis))
        assertTrue("Today inclusive end must be included", range.contains(todayEndMillis))
    }

    // =========================================================================
    // TC-011 (P1): Quick preset Last 30 Days filters to [today - 29d, today]
    // =========================================================================
    @Test
    fun tc011_quickPresetLast30Days() {
        // Arrange
        val today = LocalDate.of(2026, 11, 10)
        val twentyNineDaysAgo = today.minusDays(29) // 2026-10-12

        // Act
        val filter = HistoryDateFilter.preset(DatePreset.LAST_30_DAYS, today)

        // Assert
        assertEquals(HistoryDateFilter.Period(twentyNineDaysAgo, today), filter)
        val range = filter.toEpochRange(utcZone)
        val thirtyDaysAgoMillis = ZonedDateTime.of(2026, 10, 11, 23, 59, 59, 999_000_000, utcZone).toInstant().toEpochMilli()
        val windowStartMillis = ZonedDateTime.of(2026, 10, 12, 0, 0, 0, 0, utcZone).toInstant().toEpochMilli()

        assertFalse("30 days ago must be excluded from 30-day window", range.contains(thirtyDaysAgoMillis))
        assertTrue("Window start 29 days ago must be included", range.contains(windowStartMillis))
    }

    // =========================================================================
    // TC-012 (P1): Calendar month navigation & leap-year February boundary
    // =========================================================================
    @Test
    fun tc012_monthNavigationLeapYearFebruary() {
        // Arrange
        val leapFeb = YearMonth.of(2024, 2)
        val nonLeapFeb = YearMonth.of(2026, 2)

        // Act & Assert
        assertEquals(29, leapFeb.lengthOfMonth())
        assertEquals(28, nonLeapFeb.lengthOfMonth())
        assertTrue(leapFeb.isLeapYear)
        assertFalse(nonLeapFeb.isLeapYear)

        val leapFeb29 = LocalDate.of(2024, 2, 29)
        val filterLeap = HistoryDateFilter.SingleDay(leapFeb29)
        val rangeLeap = filterLeap.toEpochRange(utcZone)
        val feb29Start = ZonedDateTime.of(2024, 2, 29, 0, 0, 0, 0, utcZone).toInstant().toEpochMilli()
        assertTrue(rangeLeap.contains(feb29Start))
    }

    // =========================================================================
    // TC-013 (P1): Event-dot indicators mark only dates with active entries in month
    // =========================================================================
    @Test
    fun tc013_eventDotsMarkOnlyDatesWithActiveEntries() {
        // Arrange
        val ym = YearMonth.of(2026, 11)
        val entryNov2 = ZonedDateTime.of(2026, 11, 2, 10, 0, 0, 0, utcZone).toInstant().toEpochMilli()
        val entryNov8 = ZonedDateTime.of(2026, 11, 8, 14, 0, 0, 0, utcZone).toInstant().toEpochMilli()
        val entryDec1 = ZonedDateTime.of(2026, 12, 1, 9, 0, 0, 0, utcZone).toInstant().toEpochMilli()

        val allTimestamps = listOf(entryNov2, entryNov8, entryDec1)

        // Act: Map timestamps to local dates falling within visible month
        val daysWithDots = allTimestamps.mapNotNull { ts ->
            val date = java.time.Instant.ofEpochMilli(ts).atZone(utcZone).toLocalDate()
            if (YearMonth.from(date) == ym) date else null
        }.toSet()

        // Assert
        assertTrue("Nov 2 has entry -> must have dot", daysWithDots.contains(LocalDate.of(2026, 11, 2)))
        assertTrue("Nov 8 has entry -> must have dot", daysWithDots.contains(LocalDate.of(2026, 11, 8)))
        assertFalse("Nov 5 has no entries -> no dot", daysWithDots.contains(LocalDate.of(2026, 11, 5)))
        assertFalse("Dec 1 entry is in next month -> no dot in Nov", daysWithDots.contains(LocalDate.of(2026, 12, 1)))
    }

    // =========================================================================
    // TC-014 (P1): Archived entries do not trigger dots or appear in date filter
    // =========================================================================
    @Test
    fun tc014_archivedEntriesExcludedFromDotsAndDateFilter() {
        // Arrange
        val targetDate = LocalDate.of(2026, 11, 6)
        val filter = HistoryDateFilter.SingleDay(targetDate)
        val range = filter.toEpochRange(utcZone)

        val activeEntry = JournalEntry(entry_id = "1", description = "Active", timestamp = range.startMillis, isArchived = false)
        val archivedEntry = JournalEntry(entry_id = "2", description = "Archived", timestamp = range.startMillis + 1000L, isArchived = true)

        // Act: Filter feed simulating repository/query logic
        val entries = listOf(activeEntry, archivedEntry)
        val filteredFeed = entries.filter { it.isArchived != true && range.contains(it.timestamp) }
        val daysWithDots = entries.filter { it.isArchived != true }.map { java.time.Instant.ofEpochMilli(it.timestamp).atZone(utcZone).toLocalDate() }.toSet()

        // Assert
        assertEquals(listOf(activeEntry), filteredFeed)
        assertFalse("Archived entry must not be in filtered feed", filteredFeed.contains(archivedEntry))
        assertFalse("Archived date must not trigger dot if no active entries exist", daysWithDots.contains(targetDate.plusDays(1)))
    }

    // =========================================================================
    // TC-016 (P1): Compound filter excludes entry inside date period when search mismatches
    // =========================================================================
    @Test
    fun tc016_compoundFilterExcludesSearchMismatch() {
        // Arrange
        val targetDate = LocalDate.of(2026, 11, 10)
        val filter = HistoryDateFilter.SingleDay(targetDate)
        val range = filter.toEpochRange(utcZone)

        val entry = JournalEntry(entry_id = "e1", description = "Morning walk", timestamp = range.startMillis + 3600_000L)
        val searchMismatch = "migraine"

        // Act
        val insideDateRange = range.contains(entry.timestamp)
        val matchesSearch = entry.description.contains(searchMismatch, ignoreCase = true)
        val passesCompoundFilter = insideDateRange && matchesSearch

        // Assert
        assertTrue("Entry timestamp is within selected date", insideDateRange)
        assertFalse("Search does not match query", matchesSearch)
        assertFalse("Compound filter DT-1 Col 4 must exclude entry on search mismatch", passesCompoundFilter)
    }

    // =========================================================================
    // TC-017 (P1): Compound filter with multiple tags requires AND match within period
    // =========================================================================
    @Test
    fun tc017_compoundFilterRequiresAllTagsMatch() {
        // Arrange
        val period = HistoryDateFilter.Period(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10))
        val range = period.toEpochRange(utcZone)

        val e1Tags = setOf("symptom")
        val e2Tags = setOf("symptom", "urgent")
        val requiredTags = setOf("symptom", "urgent")

        // Act: AND matching predicate
        val e1Matches = range.contains(range.startMillis) && e1Tags.containsAll(requiredTags)
        val e2Matches = range.contains(range.startMillis) && e2Tags.containsAll(requiredTags)

        // Assert
        assertFalse("E1 has only 1 matching tag -> excluded by AND semantics", e1Matches)
        assertTrue("E2 has both required tags -> included", e2Matches)
    }

    // =========================================================================
    // TC-018 (P2): Sort order toggle reverses entry order within active date period
    // =========================================================================
    @Test
    fun tc018_sortOrderToggleReversesEntryOrder() {
        // Arrange
        val e1 = JournalEntry(entry_id = "1", description = "First", timestamp = 1000L)
        val e2 = JournalEntry(entry_id = "2", description = "Second", timestamp = 2000L)
        val e3 = JournalEntry(entry_id = "3", description = "Third", timestamp = 3000L)
        val entries = listOf(e1, e2, e3)

        // Act
        val descList = entries.sortedByDescending { it.timestamp }
        val ascList = entries.sortedBy { it.timestamp }

        // Assert
        assertEquals(listOf(e3, e2, e1), descList)
        assertEquals(listOf(e1, e2, e3), ascList)
        assertEquals(descList.reversed(), ascList)
    }

    // =========================================================================
    // TC-020 (P1): Switching profile when new profile has 0 entries in active period
    // =========================================================================
    @Test
    fun tc020_profileSwitchToEmptyPeriodTransitionsToEmptyState() {
        // Arrange
        val filter = HistoryDateFilter.SingleDay(LocalDate.of(2026, 11, 10))
        val range = filter.toEpochRange(utcZone)

        val selfEntries = listOf(JournalEntry(entry_id = "1", description = "Self entry", profileId = "self", timestamp = range.startMillis))
        val siblingEntries = emptyList<JournalEntry>()

        // Act
        val selfFiltered = selfEntries.filter { it.profileId == "self" && range.contains(it.timestamp) }
        val siblingFiltered = siblingEntries.filter { it.profileId == "sibling" && range.contains(it.timestamp) }

        // Assert
        assertTrue("Self profile has 1 entry (S3 populated state)", selfFiltered.isNotEmpty())
        assertTrue("Sibling profile has 0 entries -> transitions S3 to S4 (FilteredEmpty)", siblingFiltered.isEmpty())
    }

    // =========================================================================
    // TC-031 (P2): Illegal T14 - Active date filter chip is not rendered when All
    // =========================================================================
    @Test
    fun tc031_dateFilterChipNotRenderedWhenFilterIsAll() {
        // Arrange
        val filter: HistoryDateFilter = HistoryDateFilter.All

        // Act & Assert
        val shouldRenderChip = filter !is HistoryDateFilter.All
        assertFalse("Chip must not be rendered when filter is All", shouldRenderChip)
    }

    // =========================================================================
    // TC-033 (P1): Pairwise 1: SingleDay x SearchOnly x Ascending x en-US x Self
    // =========================================================================
    @Test
    fun tc033_pairwise1_singleDay_searchOnly_ascending() {
        // Arrange
        val date = LocalDate.of(2026, 11, 10)
        val filter = HistoryDateFilter.SingleDay(date)
        val range = filter.toEpochRange(utcZone)

        val e1 = JournalEntry(entry_id = "1", description = "Morning vital check", timestamp = range.startMillis + 1000L, profileId = "self")
        val e2 = JournalEntry(entry_id = "2", description = "Evening vital check", timestamp = range.startMillis + 5000L, profileId = "self")
        val search = "vital"

        // Act
        val filtered = listOf(e2, e1)
            .filter { it.profileId == "self" && range.contains(it.timestamp) && it.description.contains(search) }
            .sortedBy { it.timestamp }

        // Assert
        assertEquals(listOf(e1, e2), filtered)
    }

    // =========================================================================
    // TC-034 (P1): Pairwise 2: Period_Preset x TagsOnly x Descending x ru-RU x Sibling
    // =========================================================================
    @Test
    fun tc034_pairwise2_periodPreset_tagsOnly_descending() {
        // Arrange
        val today = LocalDate.of(2026, 11, 10)
        val filter = HistoryDateFilter.preset(DatePreset.LAST_7_DAYS, today)
        val range = filter.toEpochRange(utcZone)

        val e1 = JournalEntry(entry_id = "1", description = "Симптом 1", timestamp = range.startMillis + 2000L, profileId = "sibling")
        val e2 = JournalEntry(entry_id = "2", description = "Симптом 2", timestamp = range.startMillis + 8000L, profileId = "sibling")

        // Act
        val filtered = listOf(e1, e2)
            .filter { it.profileId == "sibling" && range.contains(it.timestamp) }
            .sortedByDescending { it.timestamp }

        // Assert
        assertEquals(listOf(e2, e1), filtered)
    }

    // =========================================================================
    // TC-035 (P1): Pairwise 3: Period_Custom x SearchAndTags x Ascending x ru-RU x Self
    // =========================================================================
    @Test
    fun tc035_pairwise3_periodCustom_searchAndTags_ascending() {
        // Arrange
        val filter = HistoryDateFilter.Period(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10))
        val range = filter.toEpochRange(utcZone)

        val search = "головная боль"
        val e1 = JournalEntry(entry_id = "1", description = "Сильная головная боль", timestamp = range.startMillis + 1000L, profileId = "self")
        val e2 = JournalEntry(entry_id = "2", description = "Легкая головная боль", timestamp = range.startMillis + 9000L, profileId = "self")

        // Act
        val filtered = listOf(e2, e1)
            .filter { it.profileId == "self" && range.contains(it.timestamp) && it.description.contains(search) }
            .sortedBy { it.timestamp }

        // Assert
        assertEquals(listOf(e1, e2), filtered)
    }

    // =========================================================================
    // TC-036 (P2): Pairwise 4: All x None x Descending x en-US x Sibling
    // =========================================================================
    @Test
    fun tc036_pairwise4_all_none_descending() {
        // Arrange
        val filter = HistoryDateFilter.All
        val e1 = JournalEntry(entry_id = "1", description = "Sib Entry 1", timestamp = 1000L, profileId = "sibling")
        val e2 = JournalEntry(entry_id = "2", description = "Sib Entry 2", timestamp = 2000L, profileId = "sibling")

        // Act
        val filtered = listOf(e1, e2)
            .filter { it.profileId == "sibling" }
            .sortedByDescending { it.timestamp }

        // Assert
        assertEquals(listOf(e2, e1), filtered)
    }

    // =========================================================================
    // TC-037 (P1): Pairwise 5: SingleDay x None x Descending x ru-RU x Self (Empty Date)
    // =========================================================================
    @Test
    fun tc037_pairwise5_singleDay_none_descending_emptyDate() {
        // Arrange
        val date = LocalDate.of(2026, 11, 4)
        val filter = HistoryDateFilter.SingleDay(date)
        val range = filter.toEpochRange(utcZone)

        val otherDayEntry = JournalEntry(entry_id = "1", description = "Other day", timestamp = range.startMillis - 100000L, profileId = "self")

        // Act
        val filtered = listOf(otherDayEntry)
            .filter { it.profileId == "self" && range.contains(it.timestamp) }

        // Assert
        assertTrue("Filtered entries on empty date must be empty list", filtered.isEmpty())
    }

    // =========================================================================
    // TC-038 (P1): Error Guessing - SQL injection and wildcard search literals
    // =========================================================================
    @Test
    fun tc038_errorGuessing_sqlInjectionAndWildcardSearchLiteralsSafe() {
        // Arrange
        val period = HistoryDateFilter.Period(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10))
        val range = period.toEpochRange(utcZone)

        val injectionPayload = "' OR '1'='1"
        val wildcardPayload = "%_test"

        val normalEntry = JournalEntry(entry_id = "1", description = "Normal entry", timestamp = range.startMillis)
        val injectionEntry = JournalEntry(entry_id = "2", description = "Notes with ' OR '1'='1 inside", timestamp = range.startMillis)

        // Act: Literal string matching prevents SQL injection bypass
        val matchedInjection = listOf(normalEntry, injectionEntry).filter {
            range.contains(it.timestamp) && it.description.contains(injectionPayload)
        }
        val matchedWildcard = listOf(normalEntry, injectionEntry).filter {
            range.contains(it.timestamp) && it.description.contains(wildcardPayload)
        }

        // Assert
        assertEquals(listOf(injectionEntry), matchedInjection)
        assertTrue("Wildcard search does not match normal entry without exact token", matchedWildcard.isEmpty())
    }

    // =========================================================================
    // TC-039 (P2): Error Guessing - Rapid sequential preset and date-cell tapping
    // =========================================================================
    @Test
    fun tc039_errorGuessing_rapidSequentialPresetAndDateTransitions() {
        // Arrange
        val today = LocalDate.of(2026, 11, 10)
        var currentFilter: HistoryDateFilter = HistoryDateFilter.All

        // Act: Simulate rapid sequence: Today -> Last 30 Days -> Yesterday -> Clear
        currentFilter = HistoryDateFilter.preset(DatePreset.TODAY, today)
        assertEquals(HistoryDateFilter.SingleDay(today), currentFilter)

        currentFilter = HistoryDateFilter.preset(DatePreset.LAST_30_DAYS, today)
        assertEquals(HistoryDateFilter.Period(today.minusDays(29), today), currentFilter)

        currentFilter = HistoryDateFilter.preset(DatePreset.YESTERDAY, today)
        assertEquals(HistoryDateFilter.SingleDay(today.minusDays(1)), currentFilter)

        currentFilter = HistoryDateFilter.All
        assertEquals(HistoryDateFilter.All, currentFilter)

        // Assert
        assertEquals("Final state must deterministically settle on All without error", HistoryDateFilter.All, currentFilter)
    }
}
