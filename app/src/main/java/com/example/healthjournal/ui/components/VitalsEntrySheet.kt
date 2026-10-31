package com.example.healthjournal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.domain.EntryVitals
import com.example.healthjournal.domain.ValidateVitals

/**
 * Quick-add/edit sheet for the manual vitals on one journal entry. Draft text
 * stays local until Save; every field is optional, and clearing all fields on
 * an entry that already has vitals removes them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalsEntrySheet(
    entry: JournalEntry,
    unitSystem: UnitSystem,
    onDismiss: () -> Unit,
    onSave: (EntryVitals) -> Unit
) {
    var input by remember(entry.entry_id, unitSystem) {
        mutableStateOf(ValidateVitals.formatVitals(entry.toVitals(), unitSystem))
    }
    val errors = ValidateVitals.validate(input, unitSystem)
    val canSave = errors.isEmpty() && (entry.hasEditableVitals() || !input.isBlank())
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
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
                    text = stringResource(R.string.vitals_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("vitals_sheet_title")
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.vitals_sheet_cd_close)
                    )
                }
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
                        .testTag("vitals_cancel")
                ) {
                    Text(stringResource(R.string.vitals_cancel))
                }
                Button(
                    onClick = {
                        ValidateVitals.toEntryVitals(input, unitSystem)?.let(onSave)
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("vitals_save")
                ) {
                    Text(stringResource(R.string.vitals_save))
                }
            }
        }
    }
}
