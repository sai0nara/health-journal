package com.example.healthjournal.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Closed epoch-millisecond interval [startMillis, endMillis] inclusive.
 */
data class EpochDateRange(
    val startMillis: Long,
    val endMillis: Long
) {
    init {
        require(startMillis <= endMillis) {
            "startMillis ($startMillis) must be <= endMillis ($endMillis)"
        }
    }

    fun contains(timestampMillis: Long): Boolean =
        timestampMillis in startMillis..endMillis
}

/**
 * Preset time windows for quick history date filtering.
 */
enum class DatePreset {
    TODAY,
    YESTERDAY,
    LAST_7_DAYS,
    LAST_30_DAYS
}

/**
 * Filter mode applied to Journal History feed.
 *
 * - [All]: Unfiltered view containing all non-archived entries.
 * - [SingleDay]: Entries matching a single local calendar date [date] in [ZoneId.systemDefault()].
 * - [Period]: Entries within inclusive interval [[startDate], [endDate]] in [ZoneId.systemDefault()].
 */
sealed class HistoryDateFilter {

    object All : HistoryDateFilter()

    data class SingleDay(val date: LocalDate) : HistoryDateFilter()

    data class Period(
        val startDate: LocalDate,
        val endDate: LocalDate
    ) : HistoryDateFilter() {
        init {
            require(startDate <= endDate) {
                "startDate ($startDate) must be <= endDate ($endDate)"
            }
        }
    }

    /**
     * Resolves the filter to a concrete [EpochDateRange] in the given [zone].
     * For [All], spans from 0 to [Long.MAX_VALUE].
     */
    fun toEpochRange(zone: ZoneId = ZoneId.systemDefault()): EpochDateRange = when (this) {
        is All -> EpochDateRange(0L, Long.MAX_VALUE)
        is SingleDay -> {
            val startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L
            EpochDateRange(startMillis, endMillis)
        }
        is Period -> {
            val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L
            EpochDateRange(startMillis, endMillis)
        }
    }

    companion object {
        /**
         * Safely constructs a [Period] filter, automatically swapping [startDate] and [endDate]
         * if an inverted range is passed.
         */
        fun periodOf(startDate: LocalDate, endDate: LocalDate): Period {
            return if (startDate <= endDate) {
                Period(startDate, endDate)
            } else {
                Period(startDate = endDate, endDate = startDate)
            }
        }

        /**
         * Resolves a [DatePreset] relative to [anchorDate] (defaulting to [LocalDate.now(zone)]).
         */
        fun preset(preset: DatePreset, anchorDate: LocalDate): HistoryDateFilter = when (preset) {
            DatePreset.TODAY -> SingleDay(anchorDate)
            DatePreset.YESTERDAY -> SingleDay(anchorDate.minusDays(1))
            DatePreset.LAST_7_DAYS -> Period(startDate = anchorDate.minusDays(6), endDate = anchorDate)
            DatePreset.LAST_30_DAYS -> Period(startDate = anchorDate.minusDays(29), endDate = anchorDate)
        }
    }
}
