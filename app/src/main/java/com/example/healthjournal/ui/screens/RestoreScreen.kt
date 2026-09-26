package com.example.healthjournal.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthjournal.R
import com.example.healthjournal.export.RestoreUiState
import com.example.healthjournal.export.RestoreViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Restore-from-backup UI body, hosted by ExportScreen's Scaffold within the Restore
 * tab. Drives [RestoreViewModel]'s MVI state machine: file selection, validation,
 * confirmation (with metadata), passphrase entry, progress, success, and error.
 */
@Composable
fun RestoreScreen(viewModel: RestoreViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    val openBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.selectBackup(it.toString()) } }

    when (val state = uiState) {
        is RestoreUiState.Idle -> RestoreIdleContent(
            onSelectBackup = { openBackupLauncher.launch(arrayOf("application/zip", "*/*")) }
        )

        is RestoreUiState.Validating,
        is RestoreUiState.Processing -> BusyContent()

        is RestoreUiState.ConfirmationRequired -> {
            RestoreConfirmationDialog(
                state = state,
                onConfirm = { viewModel.confirmRestore() },
                onDismiss = { viewModel.reset() }
            )
        }

        is RestoreUiState.PassphraseRequired -> {
            PassphraseDialog(
                title = stringResource(R.string.restore_encrypted_title),
                confirmLabel = stringResource(R.string.restore_continue),
                onSubmit = { viewModel.submitPassphrase(it) },
                onDismiss = { viewModel.reset() }
            )
        }

        is RestoreUiState.Success -> RestoreSuccessContent(
            result = state.result,
            onDone = { viewModel.reset() }
        )

        is RestoreUiState.Error -> {
            if (state.requestPassphrase) {
                PassphraseDialog(
                    title = stringResource(R.string.restore_wrong_passphrase_title),
                    confirmLabel = stringResource(R.string.restore_retry),
                    onSubmit = { viewModel.submitPassphrase(it) },
                    onDismiss = { viewModel.reset() }
                )
            } else {
                RestoreErrorContent(
                    error = state.error,
                    onRetry = { viewModel.reset() }
                )
            }
        }
    }
}

@Composable
private fun RestoreIdleContent(onSelectBackup: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.restore_intro),
            style = MaterialTheme.typography.bodyLarge
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.restore_what_happens), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.restore_what_happens_body),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onSelectBackup, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.restore_select_backup))
        }
    }
}

@Composable
private fun BusyContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        CircularProgressIndicator()
        Text(
            stringResource(R.string.restore_working),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun RestoreConfirmationDialog(
    state: RestoreUiState.ConfirmationRequired,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.restore_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.restore_confirm_body),
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider()
                MetadataRow(stringResource(R.string.restore_meta_created), formatter.format(Date(state.backupTimestamp)))
                MetadataRow(stringResource(R.string.restore_meta_schema), stringResource(R.string.restore_meta_schema_value, state.schemaVersion))
                MetadataRow(
                    stringResource(R.string.restore_meta_encrypted),
                    stringResource(if (state.isEncrypted) R.string.common_yes else R.string.common_no)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.restore_confirm_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun PassphraseDialog(
    title: String,
    confirmLabel: String,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.restore_passphrase_body),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text(stringResource(R.string.restore_passphrase_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(passphrase) },
                enabled = passphrase.isNotBlank()
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun RestoreSuccessContent(result: com.example.healthjournal.export.RestoreResult, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.restore_complete_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.restore_complete_body),
            style = MaterialTheme.typography.bodyLarge
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetadataRow(stringResource(R.string.restore_meta_journal), result.journalEntryCount.toString())
                MetadataRow(stringResource(R.string.measurements_title), result.bodyMeasurementCount.toString())
                MetadataRow(stringResource(R.string.restore_meta_goals), result.goalCount.toString())
                MetadataRow(stringResource(R.string.restore_meta_deleted), result.deletedEntryCount.toString())
                MetadataRow(stringResource(R.string.restore_meta_tags), result.tagCount.toString())
                MetadataRow(stringResource(R.string.restore_meta_workouts), result.workoutCount.toString())
                MetadataRow(stringResource(R.string.restore_meta_media), result.mediaFileCount.toString())
                MetadataRow(stringResource(R.string.restore_meta_total), result.totalRecords.toString())
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.common_done))
        }
    }
}

@Composable
private fun RestoreErrorContent(error: com.example.healthjournal.export.RestoreError, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.restore_failed_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
        Text(error.message ?: stringResource(R.string.restore_error_generic), style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.restore_choose_another))
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
