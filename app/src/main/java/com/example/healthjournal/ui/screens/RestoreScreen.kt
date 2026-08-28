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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                title = "Encrypted Backup",
                confirmLabel = "Continue",
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
                    title = "Wrong Passphrase",
                    confirmLabel = "Retry",
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
            text = "Restore a full backup to replace all current data.",
            style = MaterialTheme.typography.bodyLarge
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("What happens", style = MaterialTheme.typography.titleSmall)
                Text(
                    "All existing journal entries, measurements, goals, cards, and media " +
                        "will be replaced by the contents of the backup. You can also restore a " +
                        "password-protected (encrypted) backup.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onSelectBackup, modifier = Modifier.fillMaxWidth()) {
            Text("Select Backup File")
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
            "Working on your backup...",
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
        title = { Text("Confirm Restore") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "This will replace ALL current data with the contents of this backup. " +
                        "This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider()
                MetadataRow("Backup created", formatter.format(Date(state.backupTimestamp)))
                MetadataRow("Backup schema", "v${state.schemaVersion}")
                MetadataRow("Encrypted", if (state.isEncrypted) "Yes" else "No")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Restore") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
                    "This backup is encrypted. Enter the passphrase used to create it.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Passphrase") },
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
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
        Text("Restore Complete", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Your backup has been restored successfully. Restored data is being " +
                "re-synced to the cloud.",
            style = MaterialTheme.typography.bodyLarge
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetadataRow("Journal entries", result.journalEntryCount.toString())
                MetadataRow("Body measurements", result.bodyMeasurementCount.toString())
                MetadataRow("Goals", result.goalCount.toString())
                MetadataRow("Deleted entries", result.deletedEntryCount.toString())
                MetadataRow("Tags", result.tagCount.toString())
                MetadataRow("Media files", result.mediaFileCount.toString())
                MetadataRow("Total records", result.totalRecords.toString())
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
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
        Text("Restore Failed", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
        Text(error.message ?: "An error occurred during the restore.", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("Choose Another Backup")
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
