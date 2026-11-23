package com.example.healthjournal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthjournal.R
import com.example.healthjournal.data.local.WorkoutPreset

/**
 * Quick-entry bottom sheet opened from the Today Dashboard `+ Log` FAB.
 * Surfaces up to 3 recent workout presets (or a Manage Presets link when empty)
 * plus 4 single-tap logging shortcuts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLogBottomSheet(
    presets: List<WorkoutPreset>,
    onDismiss: () -> Unit,
    onPresetSelected: (WorkoutPreset) -> Unit,
    onManagePresets: () -> Unit,
    onLogVitals: () -> Unit,
    onStartWorkout: () -> Unit,
    onLogMeasurement: () -> Unit,
    onNewJournalEntry: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("quick_log_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.today_quick_log_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.today_quick_log_presets),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (presets.isEmpty()) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onManagePresets()
                        },
                        modifier = Modifier.testTag("quick_log_manage_presets")
                    ) {
                        Text(stringResource(R.string.today_quick_log_manage_presets))
                    }
                }
            }

            if (presets.isNotEmpty()) {
                presets.forEach { preset ->
                    ListItem(
                        headlineContent = { Text(preset.name) },
                        leadingContent = {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        },
                        modifier = Modifier
                            .testTag("quick_log_preset_${preset.id}")
                            .clickable {
                                onDismiss()
                                onPresetSelected(preset)
                            }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.today_quick_log_vitals)) },
                leadingContent = { Icon(Icons.Default.MonitorHeart, contentDescription = null) },
                modifier = Modifier
                    .testTag("quick_log_vitals")
                    .clickable {
                        onDismiss()
                        onLogVitals()
                    }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.today_quick_log_workout)) },
                leadingContent = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
                modifier = Modifier
                    .testTag("quick_log_workout")
                    .clickable {
                        onDismiss()
                        onStartWorkout()
                    }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.today_quick_log_measurement)) },
                leadingContent = { Icon(Icons.Default.Straighten, contentDescription = null) },
                modifier = Modifier
                    .testTag("quick_log_measurement")
                    .clickable {
                        onDismiss()
                        onLogMeasurement()
                    }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.today_quick_log_journal)) },
                leadingContent = { Icon(Icons.Default.EditNote, contentDescription = null) },
                modifier = Modifier
                    .testTag("quick_log_journal")
                    .clickable {
                        onDismiss()
                        onNewJournalEntry()
                    }
            )
        }
    }
}
