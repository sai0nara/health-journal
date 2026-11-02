package com.example.healthjournal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.healthjournal.R
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.UnitConverter
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.domain.EntryVitals
import com.example.healthjournal.domain.ValidateVitals
import com.example.healthjournal.domain.VitalField
import com.example.healthjournal.domain.VitalsError
import com.example.healthjournal.domain.VitalsInput
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Shared editor for the five manual vitals. Field text stays in display units;
 * callers validate the draft with [ValidateVitals.validate] and convert it with
 * [ValidateVitals.toEntryVitals] on save.
 */
@Composable
fun VitalsFieldsEditor(
    input: VitalsInput,
    onInputChange: (VitalsInput) -> Unit,
    errors: Map<VitalField, VitalsError>,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        VitalsField(
            field = VitalField.SYSTOLIC,
            label = stringResource(R.string.vitals_field_systolic),
            tag = "vitals_field_systolic",
            value = input.systolic,
            error = errors[VitalField.SYSTOLIC],
            unitSystem = unitSystem,
            keyboardType = KeyboardType.Decimal,
            onValueChange = { onInputChange(input.copy(systolic = it)) }
        )
        VitalsField(
            field = VitalField.DIASTOLIC,
            label = stringResource(R.string.vitals_field_diastolic),
            tag = "vitals_field_diastolic",
            value = input.diastolic,
            error = errors[VitalField.DIASTOLIC],
            unitSystem = unitSystem,
            keyboardType = KeyboardType.Decimal,
            onValueChange = { onInputChange(input.copy(diastolic = it)) }
        )
        VitalsField(
            field = VitalField.HEART_RATE,
            label = stringResource(R.string.vitals_field_hr),
            tag = "vitals_field_hr",
            value = input.heartRate,
            error = errors[VitalField.HEART_RATE],
            unitSystem = unitSystem,
            keyboardType = KeyboardType.Number,
            onValueChange = { onInputChange(input.copy(heartRate = it)) }
        )
        VitalsField(
            field = VitalField.GLUCOSE,
            label = stringResource(R.string.vitals_field_glucose),
            tag = "vitals_field_glucose",
            value = input.glucose,
            error = errors[VitalField.GLUCOSE],
            unitSystem = unitSystem,
            keyboardType = KeyboardType.Decimal,
            onValueChange = { onInputChange(input.copy(glucose = it)) }
        )
        VitalsField(
            field = VitalField.TEMPERATURE,
            label = stringResource(R.string.vitals_field_temperature),
            tag = "vitals_field_temperature",
            value = input.temperature,
            error = errors[VitalField.TEMPERATURE],
            unitSystem = unitSystem,
            keyboardType = KeyboardType.Decimal,
            onValueChange = { onInputChange(input.copy(temperature = it)) }
        )
        VitalsField(
            field = VitalField.SPO2,
            label = stringResource(R.string.vitals_field_spo2),
            tag = "vitals_field_spo2",
            value = input.spo2,
            error = errors[VitalField.SPO2],
            unitSystem = unitSystem,
            keyboardType = KeyboardType.Number,
            onValueChange = { onInputChange(input.copy(spo2 = it)) }
        )
    }
}

@Composable
private fun VitalsField(
    field: VitalField,
    label: String,
    tag: String,
    value: String,
    error: VitalsError?,
    unitSystem: UnitSystem,
    keyboardType: KeyboardType,
    onValueChange: (String) -> Unit
) {
    val errorMessage = error?.let { errorMessage(it, field, unitSystem) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                stringResource(
                    R.string.vitals_field_label_format,
                    label,
                    unitFor(field, unitSystem)
                )
            )
        },
        isError = error != null,
        supportingText = if (errorMessage != null) {
            { Text(errorMessage) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    )
}

@Composable
private fun errorMessage(
    error: VitalsError,
    field: VitalField,
    unitSystem: UnitSystem
): String = when (error) {
    VitalsError.InvalidFormat -> stringResource(R.string.vitals_error_invalid_format)
    VitalsError.IntegerRequired -> stringResource(R.string.vitals_error_integer_required)
    VitalsError.BloodPressurePairRequired -> stringResource(R.string.vitals_error_bp_pair)
    VitalsError.SystolicNotGreater -> stringResource(R.string.vitals_error_systolic_not_greater)
    is VitalsError.OutOfRange -> stringResource(
        R.string.vitals_error_out_of_range,
        formatBound(error.min, field, unitSystem),
        formatBound(error.max, field, unitSystem),
        unitFor(field, unitSystem)
    )
}

@Composable
internal fun unitFor(field: VitalField, unitSystem: UnitSystem): String = when (field) {
    VitalField.SYSTOLIC, VitalField.DIASTOLIC -> stringResource(R.string.entry_vitals_unit_bp)
    VitalField.HEART_RATE -> stringResource(R.string.entry_vitals_unit_bpm)
    VitalField.GLUCOSE -> if (unitSystem == UnitSystem.IMPERIAL) {
        stringResource(R.string.entry_vitals_unit_mgdl)
    } else {
        stringResource(R.string.entry_vitals_unit_mmol_l)
    }
    VitalField.TEMPERATURE -> if (unitSystem == UnitSystem.IMPERIAL) {
        stringResource(R.string.entry_vitals_unit_fahrenheit)
    } else {
        stringResource(R.string.entry_vitals_unit_celsius)
    }
    VitalField.SPO2 -> stringResource(R.string.entry_vitals_unit_percent)
}

private fun formatBound(value: Double, field: VitalField, unitSystem: UnitSystem): String {
    val displayValue = when (field) {
        VitalField.GLUCOSE -> if (unitSystem == UnitSystem.IMPERIAL) {
            UnitConverter.mmolToMgdl(value)
        } else {
            value
        }
        VitalField.TEMPERATURE -> if (unitSystem == UnitSystem.IMPERIAL) {
            UnitConverter.celsiusToFahrenheit(value)
        } else {
            value
        }
        else -> value
    }
    return if (displayValue == displayValue.toLong().toDouble()) {
        displayValue.toLong().toString()
    } else {
        BigDecimal(displayValue)
            .setScale(1, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }
}

internal fun JournalEntry.toVitals(): EntryVitals = EntryVitals(
    bpSystolic = bp_systolic,
    bpDiastolic = bp_diastolic,
    heartRate = heart_rate_avg,
    bloodGlucoseMmol = blood_glucose_mmol,
    bodyTemperatureC = body_temperature_c,
    spo2Percent = spo2_percent
)

internal fun JournalEntry.hasEditableVitals(): Boolean =
    bp_systolic != null ||
        bp_diastolic != null ||
        heart_rate_avg != null ||
        blood_glucose_mmol != null ||
        body_temperature_c != null ||
        spo2_percent != null

internal fun JournalEntry.hasAnyVitals(): Boolean =
    hasEditableVitals() || sleep_hours != null

internal fun VitalsInput.isBlank(): Boolean =
    systolic.isBlank() &&
        diastolic.isBlank() &&
        heartRate.isBlank() &&
        glucose.isBlank() &&
        temperature.isBlank() &&
        spo2.isBlank()

/** One-decimal sleep display shared by the entry card and the entry screen. */
internal fun formatSleepHours(hours: Float): String =
    BigDecimal(hours.toDouble())
        .setScale(1, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
