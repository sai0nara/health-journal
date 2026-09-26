package com.example.healthjournal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.healthjournal.R
import com.example.healthjournal.data.local.ExerciseCatalogItem
import com.example.healthjournal.data.local.UnitConverter
import com.example.healthjournal.data.local.UnitSettings
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.data.local.defaultPlanFor
import com.example.healthjournal.domain.PresetExercise
import com.example.healthjournal.domain.ScheduledDay
import com.example.healthjournal.viewmodel.PresetUiState
import com.example.healthjournal.viewmodel.PresetViewModel

/**
 * Workout-preset library screen driven by [PresetUiState]: preset list,
 * and a create/edit form with an exercise search dropdown. Uses
 * [PresetViewModel] for all state transitions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetLibraryScreen(
    viewModel: PresetViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    var deleteTarget by remember { mutableStateOf<WorkoutPreset?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back_label)
                        )
                    }
                },
                title = { Text(stringResource(R.string.preset_title)) }
            )
        },
        floatingActionButton = {
            if (uiState is PresetUiState.Library) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openCreate() },
                    text = { Text(stringResource(R.string.preset_create)) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) }
                )
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is PresetUiState.Idle -> {
                LaunchedEffect(Unit) { viewModel.showLibrary() }
            }
            is PresetUiState.Library -> LibraryContent(
                presets = presets,
                onSelectPreset = { viewModel.openEdit(it.id) },
                onAskDelete = { deleteTarget = it },
                modifier = Modifier.padding(padding)
            )
            is PresetUiState.Editing -> EditingContent(
                state = state,
                catalog = catalog,
                unitSystem = UnitSettings.read(LocalContext.current),
                onUpdateName = { viewModel.updateName(it) },
                onUpdateDay = { viewModel.updateScheduledDay(it) },
                onAddExercise = { viewModel.addExercise(it) },
                onRemoveExercise = { viewModel.removeExercise(it) },
                onUpdateExercise = { idx, ex -> viewModel.updateExercise(idx, ex) },
                onSave = { viewModel.savePreset() },
                onCancel = { viewModel.cancelEditing() },
                modifier = Modifier.padding(padding)
            )
        }
    }

    deleteTarget?.let { preset ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.preset_delete_title)) },
            text = { Text(stringResource(R.string.preset_delete_message, preset.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePreset(preset.id)
                    deleteTarget = null
                }) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun LibraryContent(
    presets: List<WorkoutPreset>,
    onSelectPreset: (WorkoutPreset) -> Unit,
    onAskDelete: (WorkoutPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    if (presets.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .testTag("preset_library_list"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(R.string.preset_empty), style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("preset_library_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(presets, key = { _, p -> p.id }) { _, preset ->
                PresetCard(
                    preset = preset,
                    onClick = { onSelectPreset(preset) },
                    onDelete = { onAskDelete(preset) }
                )
            }
        }
    }
}

@Composable
private fun PresetCard(
    preset: WorkoutPreset,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(preset.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(
                        R.string.preset_schedule_summary,
                        runCatching {
                            stringResource(
                                ScheduledDay.valueOf(preset.scheduledDay).labelRes
                            )
                        }.getOrDefault(preset.scheduledDay),
                        preset.exercises.size
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.testTag("preset_delete_${preset.id}")) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.preset_cd_delete))
            }
        }
    }
}

@Composable
private fun EditingContent(
    state: PresetUiState.Editing,
    catalog: List<ExerciseCatalogItem>,
    unitSystem: UnitSystem,
    onUpdateName: (String) -> Unit,
    onUpdateDay: (ScheduledDay) -> Unit,
    onAddExercise: (PresetExercise) -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onUpdateExercise: (Int, PresetExercise) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = onUpdateName,
            label = { Text(stringResource(R.string.preset_name_label)) },
            supportingText = state.nameError?.let { { Text(it) } },
            isError = state.nameError != null,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("preset_name_field")
        )
        DayDropdown(
            selectedDay = state.scheduledDay,
            onSelect = onUpdateDay
        )
        if (state.exerciseError != null) {
            Text(state.exerciseError!!, color = MaterialTheme.colorScheme.error)
        }
        Text(stringResource(R.string.preset_exercises_title), style = MaterialTheme.typography.titleSmall)
        ExerciseSearchDropdown(
            catalog = catalog,
            onAddExercise = onAddExercise
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .testTag("preset_draft_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(state.exercises, key = { idx, _ -> idx }) { idx, exercise ->
                DraftExerciseRow(
                    exercise = exercise,
                    catalog = catalog,
                    unitSystem = unitSystem,
                    onUpdate = { onUpdateExercise(idx, it) },
                    onRemove = { onRemoveExercise(idx) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }
            OutlinedButton(
                onClick = onSave,
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_preset_button")
            ) {
                Text(stringResource(R.string.preset_save))
            }
        }
    }
}

@Composable
private fun DayDropdown(
    selectedDay: ScheduledDay,
    onSelect: (ScheduledDay) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        TextButton(onClick = { expanded = true }) {
            Text(stringResource(selectedDay.labelRes))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ScheduledDay.entries.forEach { day ->
                DropdownMenuItem(
                    text = { Text(stringResource(day.labelRes)) },
                    onClick = {
                        onSelect(day)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ExerciseSearchDropdown(
    catalog: List<ExerciseCatalogItem>,
    onAddExercise: (PresetExercise) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val matches = if (query.isBlank()) emptyList()
        else catalog.filter { it.name.contains(query, ignoreCase = true) }

    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.preset_search_label)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("exercise_search_field")
        )
        if (matches.isNotEmpty()) {
            Column {
                matches.forEach { item ->
                    Text(
                        text = item.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val defaults = defaultPlanFor(item.id)
                                onAddExercise(
                                    PresetExercise(
                                        exerciseId = item.id,
                                        targetSets = defaults.sets,
                                        defaultReps = defaults.reps,
                                        defaultWeightKg = defaults.weightKg,
                                        restSeconds = defaults.restSeconds
                                    )
                                )
                                query = ""
                            }
                            .testTag("exercise_search_result")
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftExerciseRow(
    exercise: PresetExercise,
    catalog: List<ExerciseCatalogItem>,
    unitSystem: UnitSystem,
    onUpdate: (PresetExercise) -> Unit,
    onRemove: () -> Unit
) {
    val label = catalog.firstOrNull { it.id == exercise.exerciseId }?.name ?: exercise.exerciseId
    // The weight field shows display units and parses back to canonical kg.
    fun displayWeight(kg: Double): String =
        UnitConverter.formatMeasurement(kg, unitSystem, isWeight = true)
    var setsValue by remember(exercise.exerciseId) { mutableStateOf(exercise.targetSets.toString()) }
    var repsValue by remember(exercise.exerciseId) { mutableStateOf(exercise.defaultReps.toString()) }
    var weightValue by remember(exercise.exerciseId) { mutableStateOf(displayWeight(exercise.defaultWeightKg)) }
    var restValue by remember(exercise.exerciseId) { mutableStateOf(exercise.restSeconds.toString()) }
    LaunchedEffect(exercise.defaultWeightKg, unitSystem) {
        val formatted = displayWeight(exercise.defaultWeightKg)
        if (weightValue != formatted) weightValue = formatted
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.preset_cd_remove))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PresetField(stringResource(R.string.preset_sets), setsValue, Modifier.weight(1f).testTag("preset_field_sets")) {
                setsValue = it
                it.toIntOrNull()?.let { v -> onUpdate(exercise.copy(targetSets = v)) }
            }
            PresetField(stringResource(R.string.preset_reps), repsValue, Modifier.weight(1f).testTag("preset_field_reps")) {
                repsValue = it
                it.toIntOrNull()?.let { v -> onUpdate(exercise.copy(defaultReps = v)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PresetField(
                stringResource(
                    if (unitSystem == UnitSystem.IMPERIAL) R.string.preset_weight_lb
                    else R.string.preset_weight_kg
                ),
                weightValue,
                Modifier.weight(1f).testTag("preset_field_weight")
            ) {
                weightValue = it
                UnitConverter.parseMeasurement(it, unitSystem, isWeight = true)
                    ?.let { kg -> onUpdate(exercise.copy(defaultWeightKg = kg)) }
            }
            PresetField(stringResource(R.string.preset_rest), restValue, Modifier.weight(1f).testTag("preset_field_rest")) {
                restValue = it
                it.toIntOrNull()?.let { v -> onUpdate(exercise.copy(restSeconds = v)) }
            }
        }
    }
}

@Composable
private fun PresetField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}