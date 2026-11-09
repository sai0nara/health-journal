package com.example.healthjournal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthjournal.R
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.domain.EntryVitals
import com.example.healthjournal.domain.UtcToLocalDate
import com.example.healthjournal.domain.ValidateVitals
import com.example.healthjournal.domain.VitalsInput
import com.example.healthjournal.viewmodel.VitalMetricTab
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalLogSheet(
    initialMetric: VitalMetricTab = VitalMetricTab.BLOOD_PRESSURE,
    unitSystem: UnitSystem = UnitSystem.METRIC,
    onDismiss: () -> Unit,
    onSave: (EntryVitals, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember(unitSystem) { mutableStateOf(VitalsInput()) }
    var timestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()) }
    val errors = remember(input, unitSystem) { ValidateVitals.validate(input, unitSystem) }

    val isFutureDate = timestamp > System.currentTimeMillis() + 60_000L
    val canSave = errors.isEmpty() && !input.isBlank() && !isFutureDate

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.vital_log_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("vital_log_sheet_title")
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.vitals_sheet_cd_close)
                    )
                }
            }

            // Timestamp picker row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(timestamp)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = stringResource(R.string.measurements_cd_pick_date)
                    )
                }
            }

            if (isFutureDate) {
                Text(
                    text = stringResource(R.string.error_date_in_future),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            VitalsFieldsEditor(
                input = input,
                onInputChange = { input = it },
                errors = errors,
                unitSystem = unitSystem
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("vital_log_cancel")
                ) {
                    Text(stringResource(R.string.vitals_cancel))
                }
                Button(
                    onClick = {
                        val entryVitals = ValidateVitals.toEntryVitals(input, unitSystem)
                        if (entryVitals != null) {
                            onSave(entryVitals, timestamp)
                            onDismiss()
                        }
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("vital_log_save")
                ) {
                    Text(stringResource(R.string.vitals_save))
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = timestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            timestamp = UtcToLocalDate.toLocalMillis(utcMillis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
