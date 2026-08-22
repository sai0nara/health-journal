package com.example.healthjournal.domain

import com.example.healthjournal.data.local.BodyMeasurementEntry
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for measurement display formatting: card summaries
 * ("78.5 kg · Waist 85 cm", non-null fields only) and the weight trend
 * series mapping (ascending timestamps, null weights filtered).
 */
class MeasurementFormattersTest {

    @Test
    fun fullEntry_summaryListsAllFieldsInOrder() {
        val summary = BodyMeasurementEntry(
            weight_kg = 78.5,
            chest_cm = 100.0,
            waist_cm = 85.0,
            glute_cm = 95.0,
            thigh_cm = 55.5,
            calf_cm = 36.0,
            bicep_cm = 32.0
        ).toSummary()

        assertEquals(
            "78.5 kg · Chest 100 cm · Waist 85 cm · Glute 95 cm · Thighs 55.5 cm · Calves 36 cm · Biceps 32 cm",
            summary
        )
    }

    @Test
    fun partialEntry_summaryOnlyIncludesNonNullFields() {
        val summary = BodyMeasurementEntry(
            weight_kg = 80.25,
            waist_cm = 86.0
        ).toSummary()

        assertEquals("80.25 kg · Waist 86 cm", summary)
    }

    @Test
    fun singleGirthEntry_summaryShowsLabelAndUnit() {
        assertEquals("Waist 85 cm", BodyMeasurementEntry(waist_cm = 85.0).toSummary())
    }

    @Test
    fun wholeNumbers_trimTrailingZeros() {
        assertEquals("80 kg · Chest 98 cm", BodyMeasurementEntry(
            weight_kg = 80.0,
            chest_cm = 98.0
        ).toSummary())
    }

    @Test
    fun emptyEntry_summaryIsEmptyString() {
        assertEquals("", BodyMeasurementEntry().toSummary())
    }

    @Test
    fun weightTrend_sortedAscendingWithNullsFiltered() {
        val trend = listOf(
            BodyMeasurementEntry(timestamp = 3_000L, weight_kg = 79.0),
            BodyMeasurementEntry(timestamp = 1_000L, weight_kg = 80.5),
            BodyMeasurementEntry(timestamp = 2_000L, waist_cm = 85.0),
            BodyMeasurementEntry(timestamp = 500L, weight_kg = 81.0)
        ).toWeightTrend()

        assertEquals(
            listOf(500L to 81.0, 1_000L to 80.5, 3_000L to 79.0),
            trend
        )
    }

    @Test
    fun weightTrend_emptyWhenNoWeightsRecorded() {
        val trend = listOf(
            BodyMeasurementEntry(waist_cm = 85.0),
            BodyMeasurementEntry(bicep_cm = 32.0)
        ).toWeightTrend()

        assertEquals(emptyList<Pair<Long, Double>>(), trend)
    }
}
