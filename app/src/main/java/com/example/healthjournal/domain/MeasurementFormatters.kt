package com.example.healthjournal.domain

import com.example.healthjournal.data.local.BodyMeasurementEntry
import java.math.BigDecimal

private data class LabeledValue(val label: String?, val unit: String, val value: Double?)

/**
 * Compact card summary of a measurement entry, e.g.
 * "78.5 kg · Waist 85 cm". Non-null fields only, in canonical order;
 * returns an empty string when no values are recorded.
 */
fun BodyMeasurementEntry.toSummary(): String {
    val parts = listOf(
        LabeledValue(null, "kg", weight_kg),
        LabeledValue("Chest", "cm", chest_cm),
        LabeledValue("Waist", "cm", waist_cm),
        LabeledValue("Glute", "cm", glute_cm),
        LabeledValue("Thighs", "cm", thigh_cm),
        LabeledValue("Calves", "cm", calf_cm),
        LabeledValue("Biceps", "cm", bicep_cm)
    ).filter { it.value != null }
        .joinToString(" · ") { part ->
            val value = requireNotNull(part.value)
            val label = part.label?.let { "$it " } ?: ""
            "$label${value.formatMeasurement()} ${part.unit}"
        }
    return parts
}

/**
 * Weight-over-time series for trend rendering: ascending by timestamp,
 * entries without a recorded weight are excluded.
 */
fun List<BodyMeasurementEntry>.toWeightTrend(): List<Pair<Long, Double>> =
    toParamTrend(MeasurementField.WEIGHT)

/**
 * Chronological series for any measurement parameter: ascending by timestamp,
 * entries without that parameter recorded are excluded. Backs the per-tab
 * Body Analytics charts.
 */
fun List<BodyMeasurementEntry>.toParamTrend(field: MeasurementField): List<Pair<Long, Double>> =
    asSequence()
        .filter { it.valueFor(field) != null }
        .sortedBy { it.timestamp }
        .map { it.timestamp to it.valueFor(field)!! }
        .toList()

/** Column accessor for a parameter, keeping chart code reflection-free. */
internal fun BodyMeasurementEntry.valueFor(field: MeasurementField): Double? = when (field) {
    MeasurementField.WEIGHT -> weight_kg
    MeasurementField.CHEST -> chest_cm
    MeasurementField.WAIST -> waist_cm
    MeasurementField.GLUTE -> glute_cm
    MeasurementField.THIGH -> thigh_cm
    MeasurementField.CALF -> calf_cm
    MeasurementField.BICEP -> bicep_cm
}

internal fun Double.formatMeasurement(): String =
    BigDecimal(toString()).stripTrailingZeros().toPlainString()
