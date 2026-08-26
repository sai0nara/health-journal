package com.example.healthjournal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.healthjournal.domain.GoalValidator
import com.example.healthjournal.domain.MeasurementField

/**
 * Bottom-sheet dialog for setting or clearing a parameter's goal target.
 * Pre-filled with the current goal; validation parity with measurement
 * capture via [GoalValidator]. Saving persists immediately through the
 * view-model; Clear deletes the stored goal (FR5/FR6).
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GoalSheet(
    field: MeasurementField,
    initialTarget: Double?,
    onSave: (Double) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val prefill = remember(field, initialTarget) {
        initialTarget?.let { target ->
            if (target % 1.0 == 0.0) target.toLong().toString() else target.toString()
        }.orEmpty()
    }
    var rawText by remember(field, initialTarget) { mutableStateOf(prefill) }
    var error by remember(field) { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                    text = "${field.label} goal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close goal sheet")
                }
            }

            OutlinedTextField(
                value = rawText,
                onValueChange = {
                    rawText = it
                    error = null
                },
                label = { Text("Target (${GoalValidator.unitLabel(field)})") },
                isError = error != null,
                supportingText = {
                    if (error != null) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("bm_goal_error")
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bm_goal_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClear()
                    },
                    modifier = Modifier.testTag("bm_goal_clear")
                ) {
                    Text("Clear")
                }

                Button(
                    onClick = {
                        val validationError = GoalValidator.validate(field, rawText)
                        if (validationError == null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSave(rawText.trim().toDouble())
                        } else {
                            error = validationError
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("bm_goal_save")
                ) {
                    Text("Save")
                }
            }
        }
    }
}
