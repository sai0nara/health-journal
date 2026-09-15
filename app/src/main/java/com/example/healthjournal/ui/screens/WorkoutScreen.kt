package com.example.healthjournal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.healthjournal.R
import com.example.healthjournal.data.local.ExerciseCatalogItem
import com.example.healthjournal.data.local.UnitConverter
import com.example.healthjournal.data.local.UnitSettings
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.data.local.WorkoutSession
import com.example.healthjournal.domain.StrengthExercise
import com.example.healthjournal.domain.StrengthSet
import com.example.healthjournal.domain.ValidateWorkout
import com.example.healthjournal.domain.WorkoutIntervalSession
import com.example.healthjournal.domain.WorkoutType
import com.example.healthjournal.domain.formatCompact
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
    onBack: () -> Unit,
    onPresetsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentSessions by viewModel.recentSessions.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val catalogExercises by viewModel.catalogExercises.collectAsState()
    var manualLogType by remember { mutableStateOf<WorkoutType?>(null) }
    val unitSystem = UnitSettings.read(LocalContext.current)

    val state = uiState
    val session = when (state) {
        is WorkoutUiState.Active -> state.session
        is WorkoutUiState.Countdown -> state.session
        else -> null
    }
    if (session != null) {
        val sessionId = session.session_id
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
                        val type = WorkoutType.fromName(it.type)
                        "${type?.label ?: it.type} · ${formatDate(it.startTimestamp)}"
                    },
                    presets = presets,
                    onSelectType = { viewModel.selectType(it) },
                    onStartRoutine = { presetId -> viewModel.startPreset(presetId) },
                    onPresetsClick = onPresetsClick
                )

                is WorkoutUiState.Configuring -> ConfiguringContent(
                    state = state,
                    onTargetChange = { viewModel.updateTarget(it) },
                    onStart = { viewModel.startSession() },
                    onBack = { viewModel.cancelConfiguring() },
                    onLogPast = { manualLogType = state.type }
                )

                is WorkoutUiState.Countdown -> CountdownContent(
                    secondsRemaining = state.secondsRemaining
                )

                is WorkoutUiState.Active -> SessionContent(
                    session = state.session,
                    paused = false,
                    keepScreenOn = state.keepScreenOn,
                    restSeconds = state.restSeconds,
                    setMatrixError = state.setMatrixError,
                    catalogExercises = catalogExercises,
                    unitSystem = unitSystem,
                    onPauseResume = { viewModel.pauseSession() },
                    onFinish = { viewModel.finishSession() },
                    onAdvanceInterval = { viewModel.advanceInterval() },
                    onAddExercise = { name -> viewModel.addExercise(name) },
                    onAddSet = { exerciseId, kg, reps -> viewModel.addSet(exerciseId, kg, reps) },
                    onToggleSetCompleted = { ex, set -> viewModel.toggleSetCompleted(ex, set) },
                    onUpdateRoutineSet = { ex, set, kg, reps, rpe ->
                        viewModel.updateRoutineSet(ex, set, kg, reps, rpe)
                    },
                    onSwapExercise = { ex, exerciseId, name ->
                        viewModel.swapRoutineExercise(ex, exerciseId, name)
                    }
                )

                is WorkoutUiState.Paused -> SessionContent(
                    session = state.session,
                    paused = true,
                    keepScreenOn = false,
                    restSeconds = 0,
                    setMatrixError = null,
                    catalogExercises = catalogExercises,
                    unitSystem = unitSystem,
                    onPauseResume = { viewModel.resumeSession() },
                    onFinish = { viewModel.finishSession() },
                    onAdvanceInterval = { viewModel.advanceInterval() },
                    onAddExercise = { name -> viewModel.addExercise(name) },
                    onAddSet = { exerciseId, kg, reps -> viewModel.addSet(exerciseId, kg, reps) },
                    onToggleSetCompleted = { ex, set -> viewModel.toggleSetCompleted(ex, set) },
                    onUpdateRoutineSet = { ex, set, kg, reps, rpe ->
                        viewModel.updateRoutineSet(ex, set, kg, reps, rpe)
                    },
                    onSwapExercise = { ex, exerciseId, name ->
                        viewModel.swapRoutineExercise(ex, exerciseId, name)
                    }
                )

                is WorkoutUiState.Summary -> SummaryContent(
                    caloriesKcal = state.caloriesKcal,
                    elapsedSeconds = state.session.elapsedSeconds,
                    healthSynced = state.healthSynced,
                    tonnageKg = state.tonnageKg,
                    intervalRounds = state.intervalRounds,
                    intervalIntervals = state.intervalIntervals,
                    onDone = { viewModel.closeSummary() }
                )

                is WorkoutUiState.RecoveryRequired -> {
                    IdleContent(
                        recentLabels = emptyList(),
                        presets = presets,
                        onSelectType = { viewModel.selectType(it) },
                        onStartRoutine = { presetId -> viewModel.startPreset(presetId) },
                        onPresetsClick = onPresetsClick
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
                        presets = presets,
                        onSelectType = { viewModel.selectType(it) },
                        onStartRoutine = { presetId -> viewModel.startPreset(presetId) },
                        onPresetsClick = onPresetsClick
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
            onSave = { selectedType, duration, calories, timestamp, note ->
                viewModel.saveManualLog(selectedType, duration, calories, timestamp, note)
                manualLogType = null
            }
        )
    }
}

@Composable
private fun IdleContent(
    recentLabels: List<String>,
    presets: List<WorkoutPreset>,
    onSelectType: (WorkoutType) -> Unit,
    onStartRoutine: (presetId: String) -> Unit,
    onPresetsClick: () -> Unit
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
        item {
            Button(
                onClick = onPresetsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("presets_entry")
            ) {
                Text("Presets")
            }
        }
        if (presets.isNotEmpty()) {
            item {
                Text(
                    "Routines",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(presets, key = { it.id }) { preset ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(preset.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${preset.exercises.size} exercises",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { onStartRoutine(preset.id) },
                            modifier = Modifier.testTag("routine_start_${preset.id}")
                        ) {
                            Text("Start routine")
                        }
                    }
                }
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
                if (state.type.targetKind.supportsDistance) "Target distance (km)"
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
private fun CountdownContent(secondsRemaining: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Get ready", style = MaterialTheme.typography.titleMedium)
        Text(
            text = secondsRemaining.toString(),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.testTag("workout_countdown")
        )
    }
}

@Composable
private fun SessionContent(
    session: WorkoutSession,
    paused: Boolean,
    keepScreenOn: Boolean,
    restSeconds: Int,
    setMatrixError: String?,
    catalogExercises: List<ExerciseCatalogItem>,
    unitSystem: UnitSystem = UnitSystem.METRIC,
    onPauseResume: () -> Unit,
    onFinish: () -> Unit,
    onAdvanceInterval: () -> Unit,
    onAddExercise: (String) -> Unit,
    onAddSet: (exerciseId: String, kg: Double, reps: Int) -> Unit,
    onToggleSetCompleted: (exerciseIndex: Int, setIndex: Int) -> Unit = { _, _ -> },
    onUpdateRoutineSet: (exerciseIndex: Int, setIndex: Int, kg: Double, reps: Int, rpe: Int?) -> Unit =
        { _, _, _, _, _ -> },
    onSwapExercise: (exerciseIndex: Int, exerciseId: String, name: String) -> Unit = { _, _, _ -> }
) {
    // Keep the display awake while a routine runs so rest windows or
    // between-lightning pulls don't dim the screen mid-workout.
    val view = LocalView.current
    DisposableEffect(keepScreenOn) {
        if (keepScreenOn) {
            view.keepScreenOn = true
            onDispose { view.keepScreenOn = false }
        } else {
            onDispose { }
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = formatElapsed(session.elapsedSeconds),
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.testTag("workout_timer")
        )
        if (paused) {
            Text("Paused", style = MaterialTheme.typography.titleMedium)
        }
        val type = WorkoutType.fromName(session.type)
        val isRoutine = session.setMatrix?.any { it.isPlanned } == true
        when {
            type == WorkoutType.HIIT -> IntervalControls(
                intervalState = session.intervalState,
                onAdvanceInterval = onAdvanceInterval
            )
            type == WorkoutType.FITNESS && isRoutine -> RoutineExecution(
                exercises = session.setMatrix.orEmpty(),
                restSeconds = restSeconds,
                setMatrixError = setMatrixError,
                catalogExercises = catalogExercises,
                unitSystem = unitSystem,
                onToggleSetCompleted = onToggleSetCompleted,
                onUpdateRoutineSet = onUpdateRoutineSet,
                onSwapExercise = onSwapExercise
            )
            type == WorkoutType.FITNESS -> SetMatrixEditor(
                exercises = session.setMatrix,
                restSeconds = restSeconds,
                setMatrixError = setMatrixError,
                onAddExercise = onAddExercise,
                onAddSet = onAddSet
            )
            else -> Unit
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
private fun IntervalControls(
    intervalState: WorkoutIntervalSession?,
    onAdvanceInterval: () -> Unit
) {
    val tracker = intervalState
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = tracker?.phase?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Work",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.testTag("hiit_phase")
        )
        Text(
            text = "Round ${tracker?.rounds ?: 0}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag("hiit_rounds")
        )
        Button(onClick = onAdvanceInterval, modifier = Modifier.testTag("next_interval_button")) {
            Text("Next interval")
        }
    }
}

@Composable
private fun SetMatrixEditor(
    exercises: List<StrengthExercise>?,
    restSeconds: Int,
    setMatrixError: String?,
    onAddExercise: (String) -> Unit,
    onAddSet: (exerciseId: String, kg: Double, reps: Int) -> Unit
) {
    var exerciseName by remember { mutableStateOf("") }
    var exerciseKg by remember { mutableStateOf("") }
    var exerciseReps by remember { mutableStateOf("") }
    var activeExerciseId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (restSeconds > 0) {
            Text(
                text = "Rest ${formatElapsed(restSeconds.toLong())}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("workout_rest_timer")
            )
        }
        setMatrixError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("set_matrix_error")
            )
        }

        val list = exercises ?: emptyList()
        if (list.isEmpty()) {
            Text("No exercises yet", style = MaterialTheme.typography.bodyMedium)
        }
        // Only the tapped card is "active"; the first present exercise is active
        // by default so a single-exercise workout shows its inputs immediately.
        // Keeping the other cards read-only means their per-card kg/reps rows
        // cannot share (and therefore mirror) the active card's text state.
        val activeId = activeExerciseId ?: list.firstOrNull()?.id
        list.forEachIndexed { index, exercise ->
            Card(modifier = Modifier.fillMaxWidth(), onClick = { activeExerciseId = exercise.id }) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(exercise.name, style = MaterialTheme.typography.titleSmall)
                    exercise.sets.forEach { set ->
                        Text(
                            "${formatCompact(set.kg)} kg × ${set.reps} reps",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    val isActive = exercise.id == activeId
                    if (isActive) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = exerciseKg,
                                onValueChange = { exerciseKg = it },
                                label = { Text("kg") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("set_kg_field_$index")
                            )
                            OutlinedTextField(
                                value = exerciseReps,
                                onValueChange = { exerciseReps = it },
                                label = { Text("reps") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("set_reps_field_$index")
                            )
                            Button(
                                onClick = {
                                    onAddSet(
                                        exercise.id,
                                        exerciseKg.replace(',', '.').toDoubleOrNull() ?: 0.0,
                                        exerciseReps.toIntOrNull() ?: 0
                                    )
                                },
                                modifier = Modifier.testTag("add_set_button_$index")
                            ) {
                                Text("Add set")
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = exerciseName,
                onValueChange = { exerciseName = it },
                label = { Text("Exercise name") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("exercise_name_field")
            )
            Button(
                onClick = {
                    onAddExercise(exerciseName)
                    exerciseName = ""
                },
                modifier = Modifier.testTag("add_exercise_button")
            ) {
                Text("Add exercise")
            }
        }
    }
}

@Composable
private fun RoutineExecution(
    exercises: List<StrengthExercise>,
    restSeconds: Int,
    setMatrixError: String?,
    catalogExercises: List<ExerciseCatalogItem>,
    unitSystem: UnitSystem = UnitSystem.METRIC,
    onToggleSetCompleted: (exerciseIndex: Int, setIndex: Int) -> Unit,
    onUpdateRoutineSet: (exerciseIndex: Int, setIndex: Int, kg: Double, reps: Int, rpe: Int?) -> Unit,
    onSwapExercise: (exerciseIndex: Int, exerciseId: String, name: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (restSeconds > 0) {
            item {
                Text(
                    text = "Rest ${formatElapsed(restSeconds.toLong())}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("workout_rest_timer")
                )
            }
        }
        setMatrixError?.let {
            item {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("set_matrix_error")
                )
            }
        }
        if (exercises.isEmpty()) {
            item { Text("No exercises yet", style = MaterialTheme.typography.bodyMedium) }
        }
        exercises.forEachIndexed { exerciseIndex, exercise ->
            item(key = exercise.id) {
                RoutineExerciseCard(
                    exercise = exercise,
                    exerciseIndex = exerciseIndex,
                    catalogExercises = catalogExercises,
                    unitSystem = unitSystem,
                    onToggleSetCompleted = onToggleSetCompleted,
                    onUpdateRoutineSet = onUpdateRoutineSet,
                    onSwapExercise = onSwapExercise
                )
            }
        }
    }
}

@Composable
private fun RoutineExerciseCard(
    exercise: StrengthExercise,
    exerciseIndex: Int,
    catalogExercises: List<ExerciseCatalogItem>,
    unitSystem: UnitSystem = UnitSystem.METRIC,
    onToggleSetCompleted: (exerciseIndex: Int, setIndex: Int) -> Unit,
    onUpdateRoutineSet: (exerciseIndex: Int, setIndex: Int, kg: Double, reps: Int, rpe: Int?) -> Unit,
    onSwapExercise: (exerciseIndex: Int, exerciseId: String, name: String) -> Unit
) {
    var swapExpanded by remember(exercise.exerciseId) { mutableStateOf(false) }
    // The quick pad acts on the last set the user touched (focused a field),
    // not blindly the first uncompleted one — otherwise typing RPE on Set 3
    // and tapping +2.5 kg would silently bump Set 1.
    var activeSet by remember(exercise.exerciseId) {
        mutableStateOf(exercise.sets.indexOfFirst { !it.completed }.coerceAtLeast(0))
    }
    val padSet = exercise.sets
        .getOrNull(activeSet)
        ?.takeIf { !it.completed }
        ?: exercise.sets.firstOrNull { !it.completed }
    val padEnabled = padSet != null
    fun adjustWeight(deltaKg: Double) {
        val target = padSet ?: return
        onUpdateRoutineSet(
            exerciseIndex,
            exercise.sets.indexOf(target),
            (target.kg + deltaKg).coerceAtLeast(0.0),
            target.reps,
            target.rpe
        )
    }
    fun adjustReps(delta: Int) {
        val target = padSet ?: return
        onUpdateRoutineSet(
            exerciseIndex,
            exercise.sets.indexOf(target),
            target.kg,
            (target.reps + delta).coerceAtLeast(1),
            target.rpe
        )
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("routine_exercise_$exerciseIndex")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    OutlinedButton(
                        onClick = { swapExpanded = true },
                        modifier = Modifier.testTag("routine_swap_$exerciseIndex")
                    ) {
                        Text("Swap")
                    }
                    DropdownMenu(
                        expanded = swapExpanded,
                        onDismissRequest = { swapExpanded = false },
                        modifier = Modifier.testTag("routine_swap_menu_$exerciseIndex")
                    ) {
                        catalogExercises.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.name) },
                                onClick = {
                                    if (item.id != exercise.exerciseId) {
                                        onSwapExercise(exerciseIndex, item.id, item.name)
                                    }
                                    swapExpanded = false
                                },
                                leadingIcon = {
                                    if (item.id == exercise.exerciseId) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            Text(
                text = "${exercise.targetSets} x ${exercise.targetReps} @ " +
                    "${UnitConverter.formatDouble(exercise.targetWeightKg ?: 0.0)} kg · " +
                    "Rest ${exercise.restSeconds}s",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            exercise.sets.forEachIndexed { setIndex, set ->
                RoutineSetRow(
                    exerciseIndex = exerciseIndex,
                    setIndex = setIndex,
                    set = set,
                    onSetActivated = { activeSet = setIndex },
                    onToggleCompleted = onToggleSetCompleted,
                    onUpdateRoutineSet = onUpdateRoutineSet
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = padEnabled,
                    onClick = { adjustReps(-1) },
                    modifier = Modifier.weight(1f).testTag("routine_reps_minus")
                ) {
                    Text("-1 rep", maxLines = 1)
                }
                OutlinedButton(
                    enabled = padEnabled,
                    onClick = { adjustWeight(-padStepWeightKg(unitSystem)) },
                    modifier = Modifier.weight(1f).testTag("routine_weight_minus")
                ) {
                    Text("-${padStepDisplay(unitSystem)}", maxLines = 1)
                }
                OutlinedButton(
                    enabled = padEnabled,
                    onClick = { adjustWeight(padStepWeightKg(unitSystem)) },
                    modifier = Modifier.weight(1f).testTag("routine_weight_plus")
                ) {
                    Text("+${padStepDisplay(unitSystem)}", maxLines = 1)
                }
                OutlinedButton(
                    enabled = padEnabled,
                    onClick = { adjustReps(1) },
                    modifier = Modifier.weight(1f).testTag("routine_reps_plus")
                ) {
                    Text("+1 rep", maxLines = 1)
                }
            }
        }
    }
}

/** Kilogram step for the quick pad: 2.5 kg or a 5 lb plate. */
private fun padStepWeightKg(unitSystem: UnitSystem): Double =
    if (unitSystem == UnitSystem.IMPERIAL) UnitConverter.lbsToKg(5.0) else 2.5

/** Display label for the current pad step, in the selected unit system. */
private fun padStepDisplay(unitSystem: UnitSystem): String =
    if (unitSystem == UnitSystem.IMPERIAL) "5 lb" else "2.5 kg"

@Composable
private fun RoutineSetRow(
    exerciseIndex: Int,
    setIndex: Int,
    set: StrengthSet,
    onSetActivated: (setIndex: Int) -> Unit,
    onToggleCompleted: (exerciseIndex: Int, setIndex: Int) -> Unit,
    onUpdateRoutineSet: (exerciseIndex: Int, setIndex: Int, kg: Double, reps: Int, rpe: Int?) -> Unit
) {
    // Each field keeps its own text state so typing stays smooth; the row
    // re-seeds it from the persisted set only while unfocused (e.g. after a
    // quick-pad tap), and pushes every valid edit straight to the ViewModel.
    var kgFocused by remember { mutableStateOf(false) }
    var repsFocused by remember { mutableStateOf(false) }
    var rpeFocused by remember { mutableStateOf(false) }
    var kgValue by remember { mutableStateOf(TextFieldValue(UnitConverter.formatDouble(set.kg))) }
    var repsValue by remember { mutableStateOf(TextFieldValue(set.reps.toString())) }
    var rpeValue by remember { mutableStateOf(TextFieldValue(set.rpe?.toString() ?: "")) }

    fun commit(kg: Double, reps: Int, rpe: Int?) {
        onUpdateRoutineSet(exerciseIndex, setIndex, kg, reps, rpe)
    }
    fun currentKg(): Double? = kgValue.text.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
    fun currentReps(): Int? = repsValue.text.toIntOrNull()?.takeIf { it >= 1 }
    fun currentRpe(): Int? = rpeValue.text.toIntOrNull()?.takeIf { it in 1..10 }

    LaunchedEffect(set.kg) {
        if (!kgFocused) kgValue = TextFieldValue(UnitConverter.formatDouble(set.kg))
    }
    LaunchedEffect(set.reps) {
        if (!repsFocused) repsValue = TextFieldValue(set.reps.toString())
    }
    LaunchedEffect(set.rpe) {
        if (!rpeFocused) rpeValue = TextFieldValue(set.rpe?.toString() ?: "")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Set ${setIndex + 1}",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.testTag("routine_set_label_${exerciseIndex}_${setIndex}")
        )
        OutlinedTextField(
            value = kgValue,
            onValueChange = {
                kgValue = it
                currentKg()?.let { kg -> commit(kg, repsValue.text.toIntOrNull() ?: set.reps, currentRpe()) }
            },
            label = { Text("kg") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged {
                    kgFocused = it.isFocused
                    if (it.isFocused) onSetActivated(setIndex)
                }
                .testTag("routine_set_kg_${exerciseIndex}_${setIndex}")
        )
        OutlinedTextField(
            value = repsValue,
            onValueChange = {
                repsValue = it
                currentReps()?.let { reps -> commit(currentKg() ?: set.kg, reps, currentRpe()) }
            },
            label = { Text("reps") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged {
                    repsFocused = it.isFocused
                    if (it.isFocused) onSetActivated(setIndex)
                }
                .testTag("routine_set_reps_${exerciseIndex}_${setIndex}")
        )
        OutlinedTextField(
            value = rpeValue,
            onValueChange = {
                rpeValue = it
                currentRpe()?.let { rpe -> commit(currentKg() ?: set.kg, repsValue.text.toIntOrNull() ?: set.reps, rpe) }
            },
            label = { Text("RPE") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged {
                    rpeFocused = it.isFocused
                    if (it.isFocused) onSetActivated(setIndex)
                }
                .testTag("routine_set_rpe_${exerciseIndex}_${setIndex}")
        )
        Checkbox(
            checked = set.completed,
            onCheckedChange = {
                onSetActivated(setIndex)
                onToggleCompleted(exerciseIndex, setIndex)
            },
            modifier = Modifier.testTag("routine_set_done_${exerciseIndex}_${setIndex}")
        )
    }
}

@Composable
private fun SummaryContent(
    caloriesKcal: Double,
    elapsedSeconds: Long,
    healthSynced: Boolean,
    tonnageKg: Double?,
    intervalRounds: Int,
    intervalIntervals: Int,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Workout Complete", style = MaterialTheme.typography.headlineSmall)
        Text(
            "${formatCompact(caloriesKcal)} kcal",
            style = MaterialTheme.typography.displaySmall
        )
        Text(formatElapsed(elapsedSeconds), style = MaterialTheme.typography.titleMedium)
        tonnageKg?.let {
            Text(
                "Tonnage: ${formatCompact(it)} kg",
                style = MaterialTheme.typography.titleMedium
            )
        }
        if (intervalRounds > 0 || intervalIntervals > 0) {
            Text(
                "Rounds: $intervalRounds · Intervals: $intervalIntervals",
                style = MaterialTheme.typography.titleMedium
            )
        }
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
    onSave: (WorkoutType, String, String, Long, String) -> Unit
) {
    var duration by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(TextFieldValue("")) }
    var notes by remember { mutableStateOf("") }
    var laps by remember { mutableStateOf("") }
    var movements by remember { mutableStateOf("") }
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
                when {
                    initialType == WorkoutType.SWIMMING -> OutlinedTextField(
                        value = laps,
                        onValueChange = { laps = UnitConverter.sanitizeDecimalInput(it) },
                        label = { Text("Laps (optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_laps_field")
                    )
                    initialType == WorkoutType.CALISTHENICS -> OutlinedTextField(
                        value = movements,
                        onValueChange = { movements = UnitConverter.sanitizeDecimalInput(it) },
                        label = { Text("Movements (optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_movements_field")
                    )
                    else -> Unit
                }
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
                    val extra = when {
                        initialType == WorkoutType.SWIMMING && laps.isNotBlank() -> "Laps: $laps"
                        initialType == WorkoutType.CALISTHENICS && movements.isNotBlank() ->
                            "Movements: $movements"
                        else -> ""
                    }
                    val note = listOf(extra, notes.trim())
                        .filter { it.isNotBlank() }
                        .joinToString("\n")
                    onSave(
                        initialType,
                        duration,
                        calories,
                        parseDateStrictOrNull(date.text) ?: System.currentTimeMillis(),
                        note
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

private fun formatDate(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ISO_LOCAL_DATE)
