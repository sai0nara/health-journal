package com.example.healthjournal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.healthjournal.R
import com.example.healthjournal.data.local.UnitConverter
import com.example.healthjournal.domain.ValidateWorkout
import com.example.healthjournal.domain.WorkoutType
import com.example.healthjournal.domain.validation.DateInputMask
import com.example.healthjournal.domain.validation.ValidateDateOfBirthUseCase
import com.example.healthjournal.domain.validation.ValidationResult
import com.example.healthjournal.viewmodel.WorkoutUiState
import com.example.healthjournal.viewmodel.WorkoutViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/**
 * Workout hub driven by [WorkoutUiState]: discovery, configuration, timed
 * sessions, manual logging, summaries, and crash-recovery prompts. The
 * one-second ticker lives here so [WorkoutViewModel] stays deterministic
 * and JVM-testable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentSessions by viewModel.recentSessions.collectAsState()
    var manualLogType by remember { mutableStateOf<WorkoutType?>(null) }

    val state = uiState
    if (state is WorkoutUiState.Active) {
        val sessionId = state.session.session_id
        LaunchedEffect(sessionId) {
            while (true) {
                kotlinx.coroutines.delay(1_000)
                viewModel.advanceTime(1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                title = { Text("Workouts") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (state) {
                is WorkoutUiState.Idle -> IdleContent(
                    recentLabels = recentSessions.map {
                        "${WorkoutType.valueOf(it.type).label} · ${formatDate(it.startTimestamp)}"
                    },
                    onSelectType = { viewModel.selectType(it) }
                )

                is WorkoutUiState.Configuring -> ConfiguringContent(
                    state = state,
                    onTargetChange = { viewModel.updateTarget(it) },
                    onStart = { viewModel.startSession() },
                    onBack = { viewModel.cancelConfiguring() },
                    onLogPast = { manualLogType = state.type }
                )

                is WorkoutUiState.Active -> SessionContent(
                    elapsedSeconds = state.session.elapsedSeconds,
                    paused = false,
                    onPauseResume = { viewModel.pauseSession() },
                    onFinish = { viewModel.finishSession() }
                )

                is WorkoutUiState.Paused -> SessionContent(
                    elapsedSeconds = state.session.elapsedSeconds,
                    paused = true,
                    onPauseResume = { viewModel.resumeSession() },
                    onFinish = { viewModel.finishSession() }
                )

                is WorkoutUiState.Summary -> SummaryContent(
                    caloriesKcal = state.caloriesKcal,
                    elapsedSeconds = state.session.elapsedSeconds,
                    healthSynced = state.healthSynced,
                    onDone = { viewModel.closeSummary() }
                )

                is WorkoutUiState.RecoveryRequired -> {
                    IdleContent(
                        recentLabels = emptyList(),
                        onSelectType = { viewModel.selectType(it) }
                    )
                    AlertDialog(
                        onDismissRequest = { viewModel.discardRecovery() },
                        title = { Text("Unfinished Workout") },
                        text = { Text("A previous session did not finish. Resume it or discard it.") },
                        confirmButton = {
                            TextButton(onClick = { viewModel.resumeRecovery() }) {
                                Text("Resume")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.discardRecovery() }) {
                                Text("Discard")
                            }
                        }
                    )
                }

                is WorkoutUiState.Error -> {
                    IdleContent(
                        recentLabels = emptyList(),
                        onSelectType = { viewModel.selectType(it) }
                    )
                    AlertDialog(
                        onDismissRequest = { viewModel.dismissError() },
                        title = { Text("Workout Error") },
                        text = { Text(state.message) },
                        confirmButton = {
                            TextButton(onClick = { viewModel.dismissError() }) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
        }
    }

    manualLogType?.let { type ->
        ManualLogDialog(
            initialType = type,
            onDismiss = { manualLogType = null },
            onSave = { selectedType, duration, calories, timestamp ->
                viewModel.saveManualLog(selectedType, duration, calories, timestamp)
                manualLogType = null
            }
        )
    }
}

@Composable
private fun IdleContent(
    recentLabels: List<String>,
    onSelectType: (WorkoutType) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("workout_catalog")
    ) {
        item {
            Text("Choose a workout", style = MaterialTheme.typography.titleMedium)
        }
        items(WorkoutType.entries) { type ->
            OutlinedButton(
                onClick = { onSelectType(type) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(type.label)
            }
        }
        if (recentLabels.isNotEmpty()) {
            item {
                Text(
                    "Recent",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(recentLabels) { label ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfiguringContent(
    state: WorkoutUiState.Configuring,
    onTargetChange: (String) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
    onLogPast: () -> Unit
) {
    Text(
        "Configure ${state.type.label}",
        style = MaterialTheme.typography.titleMedium
    )
    OutlinedTextField(
        value = state.target,
        onValueChange = onTargetChange,
        label = {
            Text(
                if (state.type == WorkoutType.RUN) "Target distance (km)"
                else "Target duration (min)"
            )
        },
        supportingText = state.targetError?.let { { Text(it) } },
        isError = state.targetError != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("workout_target_field")
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onBack) {
            Text("Back")
        }
        Button(onClick = onStart, modifier = Modifier.weight(1f)) {
            Text("Start")
        }
    }
    OutlinedButton(
        onClick = onLogPast,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Log past ${state.type.label} instead")
    }
}

@Composable
private fun SessionContent(
    elapsedSeconds: Long,
    paused: Boolean,
    onPauseResume: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = formatElapsed(elapsedSeconds),
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.testTag("workout_timer")
        )
        if (paused) {
            Text("Paused", style = MaterialTheme.typography.titleMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPauseResume, modifier = Modifier.weight(1f)) {
                Text(if (paused) "Resume" else "Pause")
            }
            Button(onClick = onFinish, modifier = Modifier.weight(1f)) {
                Text("Finish")
            }
        }
    }
}

@Composable
private fun SummaryContent(
    caloriesKcal: Double,
    elapsedSeconds: Long,
    healthSynced: Boolean,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Workout Complete", style = MaterialTheme.typography.headlineSmall)
        Text(
            "${formatCalories(caloriesKcal)} kcal",
            style = MaterialTheme.typography.displaySmall
        )
        Text(formatElapsed(elapsedSeconds), style = MaterialTheme.typography.titleMedium)
        Text(
            if (healthSynced) "Synced to Health Connect" else "Saved locally",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualLogDialog(
    initialType: WorkoutType,
    onDismiss: () -> Unit,
    onSave: (WorkoutType, String, String, Long) -> Unit
) {
    var duration by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(TextFieldValue("")) }
    var notes by remember { mutableStateOf("") }
    var durationError by remember { mutableStateOf<String?>(null) }
    var caloriesError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<ValidationResult?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    // Same validation and rules as Personal Card Date of Birth.
    val dateValidator = remember { ValidateDateOfBirthUseCase() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Past ${initialType.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = duration,
                    onValueChange = {
                        // Same sanitize rule as Personal Card Height/Weight:
                        // digits with a single decimal point.
                        duration = UnitConverter.sanitizeDecimalInput(it)
                        durationError = null
                    },
                    label = { Text("Duration (min)") },
                    supportingText = durationError?.let { { Text(it) } },
                    isError = durationError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_duration_field")
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = {
                        calories = UnitConverter.sanitizeDecimalInput(it)
                        caloriesError = null
                    },
                    label = { Text("Calories (optional)") },
                    supportingText = caloriesError?.let { { Text(it) } },
                    isError = caloriesError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_calories_field")
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = DateInputMask.mask(it); dateError = null },
                    label = { Text("Date (YYYY-MM-DD, blank for today)") },
                    supportingText = (dateError as? ValidationResult.Invalid)?.let {
                        { Text(stringResource(it.errorResId)) }
                    },
                    isError = dateError is ValidationResult.Invalid,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = stringResource(R.string.select_date)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_date_field")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_notes_field")
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val errors = ValidateWorkout.validateManualLog(
                    type = initialType,
                    durationMinutes = duration,
                    calories = calories,
                    timestamp = System.currentTimeMillis()
                )
                durationError = errors["duration"]
                caloriesError = errors["calories"]
                dateError = when {
                    date.text.isBlank() -> null
                    parseDateStrictOrNull(date.text) == null ->
                        ValidationResult.Invalid(R.string.error_invalid_date_format)
                    else -> dateValidator(date.text).takeIf { it is ValidationResult.Invalid }
                }
                if (durationError == null && caloriesError == null && dateError == null) {
                    onSave(
                        initialType,
                        duration,
                        calories,
                        parseDateStrictOrNull(date.text) ?: System.currentTimeMillis()
                    )
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Date picker, mirroring Personal Card Date of Birth.
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        date = DateInputMask.mask(
                            TextFieldValue(picked, TextRange(picked.length))
                        )
                        dateError = null
                    }
                    showDatePicker = false
                }) {
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

/**
 * Strict calendar parse for the manual-log date: unlike the lenient default,
 * impossible dates (month 13, February 30th, non-leap February 29th) are
 * rejected instead of silently rolled into a nearby valid date. Note the
 * proleptic-year pattern: STRICT rejects year-of-era without an era.
 */
private fun parseDateStrictOrNull(raw: String): Long? {
    if (raw.isBlank()) return null
    return try {
        LocalDate.parse(
            raw.trim(),
            DateTimeFormatter.ofPattern("uuuu-MM-dd")
                .withResolverStyle(ResolverStyle.STRICT)
        )
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    } catch (e: DateTimeParseException) {
        null
    }
}

private fun formatElapsed(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatCalories(caloriesKcal: Double): String =
    if (caloriesKcal == caloriesKcal.toLong().toDouble()) {
        "${caloriesKcal.toLong()}"
    } else {
        "%.1f".format(caloriesKcal)
    }

private fun formatDate(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ISO_LOCAL_DATE)
