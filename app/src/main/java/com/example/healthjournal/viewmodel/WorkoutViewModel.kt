package com.example.healthjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.PresetRepository
import com.example.healthjournal.data.WorkoutRepository
import com.example.healthjournal.data.local.ExerciseCatalogDao
import com.example.healthjournal.data.local.ExerciseCatalogItem
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.data.local.WorkoutSession
import com.example.healthjournal.data.local.WorkoutStatus
import com.example.healthjournal.domain.CalorieEstimator
import com.example.healthjournal.domain.StrengthExercise
import com.example.healthjournal.domain.StrengthSet
import com.example.healthjournal.domain.TonnageCalculator
import com.example.healthjournal.domain.ValidateStrengthExercise
import com.example.healthjournal.domain.ValidateWorkout
import com.example.healthjournal.domain.WorkoutIntervalSession
import com.example.healthjournal.domain.WorkoutType
import com.example.healthjournal.domain.formatCompact
import com.example.healthjournal.health.WorkoutHealthDataSource
import com.example.healthjournal.health.toHealthRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Haptic cues the UI plays for workout control events. */
enum class WorkoutHaptic {
    START,
    STOP,
    INTERVAL,
    /** Heavy tap when a routine set is checked off. */
    SET_COMPLETE,
    /** Success pattern when every set of a planned exercise is done. */
    EXERCISE_COMPLETE,
    /** Light tap when a routine rest period elapses. */
    REST_ENDED
}

/**
 * MVI ViewModel for workout tracking. Pure state transitions are
 * JVM-testable via injected [repository], [healthSource] and
 * [journalRepository]; the only Android-specific step (haptics) is a
 * plain callback. Time advances through [advanceTime] so tests control
 * the clock deterministically; production drives it from a ticker.
 */
class WorkoutViewModel(
    private val repository: WorkoutRepository,
    private val healthSource: WorkoutHealthDataSource,
    private val journalRepository: JournalRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val onHaptic: (WorkoutHaptic) -> Unit = {},
    private val clock: () -> Long = System::currentTimeMillis,
    private val presetRepository: PresetRepository? = null,
    private val catalogDao: ExerciseCatalogDao? = null
) : ViewModel() {

    /**
     * Wall-clock reading of the last tick, used to reconcile the running
     * timer against [clock] so elapsed/rest time survives Doze: the UI ticker
     * only fires `advanceTime(1)` per delayed tick, which under-reports when
     * the device sleeps between ticks.
     */
    private var lastTickMillis: Long = clock()

    private val _uiState = MutableStateFlow<WorkoutUiState>(WorkoutUiState.Idle)
    val uiState: StateFlow<WorkoutUiState> = _uiState

    /** Workout history for discovery, newest first (completed only). */
    val recentSessions: StateFlow<List<WorkoutSession>> =
        repository.completedSessions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    /** Saved preset routines for the hub's routine picker, newest-edited first. */
    val presets: StateFlow<List<WorkoutPreset>> =
        (presetRepository?.presets ?: emptyFlow()).stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    /** Full exercise catalog for the routine swap dropdown, grouped by category. */
    val catalogExercises: StateFlow<List<ExerciseCatalogItem>> =
        (catalogDao?.getAllExercises() ?: emptyFlow()).stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    init {
        checkRecovery()
    }

    /** Looks for an unfinished session from a previous run (crash recovery). */
    fun checkRecovery() {
        viewModelScope.launch(dispatcher) {
            val unfinished = repository.getUnfinishedSession()
            if (unfinished != null) {
                _uiState.value = WorkoutUiState.RecoveryRequired(unfinished)
            }
        }
    }

    fun selectType(type: WorkoutType) {
        _uiState.value = WorkoutUiState.Configuring(type = type)
    }

    fun updateTarget(text: String) {
        val current = _uiState.value as? WorkoutUiState.Configuring ?: return
        _uiState.value = current.copy(target = text, targetError = null)
    }

    fun startSession() {
        val current = _uiState.value as? WorkoutUiState.Configuring ?: return
        viewModelScope.launch(dispatcher) {
            val unfinished = repository.getUnfinishedSession()
            if (unfinished != null) {
                // Never start a second session while one is unfinished.
                _uiState.value = WorkoutUiState.RecoveryRequired(unfinished)
                return@launch
            }
            val errors = ValidateWorkout.validateTarget(current.type, current.target)
            if (errors.isNotEmpty()) {
                _uiState.value = current.copy(
                    targetError = errors["target"] ?: errors["type"]
                )
                return@launch
            }
            val targetValue = current.target.trim().replace(',', '.').toDouble()
            // Distance-capable types (Run, Walking/Hiking, Cycling) take their
            // target in kilometres and store metres; the rest take minutes.
            val targetDistanceM =
                if (current.type.targetKind.supportsDistance) targetValue * 1_000 else null
            val targetDurationMin =
                if (current.type.targetKind.supportsDistance) null else targetValue
            val session = WorkoutSession(
                type = current.type.name,
                status = WorkoutStatus.ACTIVE.name,
                startTimestamp = clock(),
                targetDistanceM = targetDistanceM,
                targetDurationMin = targetDurationMin
            )
            lastTickMillis = clock()
            repository.saveSession(session)
            _uiState.value = WorkoutUiState.Countdown(
                session = session,
                secondsRemaining = COUNTDOWN_SECONDS
            )
            onHaptic(WorkoutHaptic.START)
        }
    }

    fun pauseSession() {
        val current = _uiState.value as? WorkoutUiState.Active ?: return
        viewModelScope.launch(dispatcher) {
            val paused = current.session.copy(status = WorkoutStatus.PAUSED.name)
            repository.saveSession(paused)
            _uiState.value = WorkoutUiState.Paused(paused)
            onHaptic(WorkoutHaptic.STOP)
        }
    }

    fun resumeSession() {
        val current = _uiState.value as? WorkoutUiState.Paused ?: return
        viewModelScope.launch(dispatcher) {
            val active = current.session.copy(status = WorkoutStatus.ACTIVE.name)
            lastTickMillis = clock()
            repository.saveSession(active)
            _uiState.value = WorkoutUiState.Active(active)
            onHaptic(WorkoutHaptic.START)
        }
    }

    /**
     * Advances either the pre-session countdown or the running clock. The
     * delta reconciles the explicitly supplied [seconds] against the wall
     * clock ([clock]) since the last tick, so time spent in Doze — when the
     * UI ticker fires late rather than once per real second — is still
     * counted. During the countdown the session row is left untouched; once
     * it hits zero the session goes Active at zero elapsed time. In Active,
     * persistence is batched: the session row is rewritten only on
     * five-second boundaries (plus always on pause/finish) instead of on
     * every tick. Rest-timer seconds also tick down here so one clock source
     * drives both.
     */
    fun advanceTime(seconds: Long) {
        val now = clock()
        val wallDelta = ((now - lastTickMillis) / 1000L).coerceAtLeast(0L)
        val delta = maxOf(seconds, wallDelta)
        lastTickMillis = now
        val current = _uiState.value
        when (current) {
            is WorkoutUiState.Countdown -> {
                val remaining = current.secondsRemaining - delta.toInt()
                if (remaining <= 0) {
                    _uiState.value = WorkoutUiState.Active(current.session)
                } else {
                    _uiState.value = current.copy(secondsRemaining = remaining)
                }
            }
            is WorkoutUiState.Active -> viewModelScope.launch(dispatcher) {
                val elapsed = current.session.elapsedSeconds + delta
                val advanced = current.session.copy(elapsedSeconds = elapsed)
                val restSeconds = (current.restSeconds - delta).coerceAtLeast(0L).toInt()
                _uiState.value = WorkoutUiState.Active(
                    session = advanced,
                    restSeconds = restSeconds,
                    setMatrixError = current.setMatrixError
                )
                if (current.restSeconds > 0 && restSeconds == 0) {
                    onHaptic(WorkoutHaptic.REST_ENDED)
                }
                if (elapsed % PERSIST_EVERY_SECONDS == 0L) {
                    repository.saveSession(advanced)
                }
            }
            else -> Unit
        }
    }

    /**
     * Advances the manual HIIT interval tracker one boundary (WORK <-> REST),
     * persists immediately for crash recovery, and emits a haptic cue. Only
     * meaningful while Active on an HIIT session.
     */
    fun advanceInterval() {
        val current = _uiState.value as? WorkoutUiState.Active ?: return
        if (WorkoutType.fromName(current.session.type) != WorkoutType.HIIT) return
        val tracker = current.session.intervalState ?: WorkoutIntervalSession()
        val updatedTracker = tracker.advance()
        viewModelScope.launch(dispatcher) {
            val updated = current.session.copy(intervalState = updatedTracker)
            repository.saveSession(updated)
            _uiState.value = current.copy(session = updated)
            onHaptic(WorkoutHaptic.INTERVAL)
        }
    }

    /**
     * Adds a named strength exercise to the set matrix while Active on a
     * Fitness session; invalid names surface as an inline error.
     */
    fun addExercise(name: String) {
        val current = _uiState.value as? WorkoutUiState.Active ?: return
        if (WorkoutType.fromName(current.session.type) != WorkoutType.FITNESS) return
        val nameError = ValidateStrengthExercise.validateName(name)
        if (nameError != null) {
            _uiState.value = current.copy(setMatrixError = nameError)
            return
        }
        val exercise = StrengthExercise(name = name.trim())
        viewModelScope.launch(dispatcher) {
            val matrix = (current.session.setMatrix ?: emptyList()) + exercise
            val updated = current.session.copy(setMatrix = matrix)
            repository.saveSession(updated)
            _uiState.value = current.copy(session = updated, setMatrixError = null)
        }
    }

    /**
     * Adds a validated set (kg, reps) to an exercise and starts the rest
     * timer. Invalid values surface as an inline error without mutating.
     */
    fun addSet(exerciseId: String, kg: Double, reps: Int) {
        val current = _uiState.value as? WorkoutUiState.Active ?: return
        if (WorkoutType.fromName(current.session.type) != WorkoutType.FITNESS) return
        val errors = ValidateStrengthExercise.validateSet(kg = kg, reps = reps)
        if (errors.isNotEmpty()) {
            _uiState.value = current.copy(
                setMatrixError = errors["kg"] ?: errors["reps"]
            )
            return
        }
        viewModelScope.launch(dispatcher) {
            val matrix = (current.session.setMatrix ?: emptyList()).map { exercise ->
                if (exercise.id == exerciseId) {
                    exercise.copy(sets = exercise.sets + StrengthSet(kg = kg, reps = reps))
                } else {
                    exercise
                }
            }
            val updated = current.session.copy(setMatrix = matrix)
            repository.saveSession(updated)
            _uiState.value = current.copy(
                session = updated,
                setMatrixError = null,
                restSeconds = DEFAULT_REST_SECONDS
            )
        }
    }

    /**
     * Starts a routine session from a saved preset: prebuilds the planned set
     * matrix (one scaled set-row per target set, prefilling weight/reps, all
     * uncompleted), resolves catalog names, and enters the same 3-2-1
     * countdown as a manual session. Requires [presetRepository] and
     * [catalogDao] wired in; with neither present the call is a no-op.
     */
    fun startPreset(presetId: String) {
        val presetRepository = presetRepository ?: return
        val catalogDao = catalogDao ?: return
        viewModelScope.launch(dispatcher) {
            val unfinished = repository.getUnfinishedSession()
            if (unfinished != null) {
                // Never start a second session while one is unfinished.
                _uiState.value = WorkoutUiState.RecoveryRequired(unfinished)
                return@launch
            }
            val preset = presetRepository.getPreset(presetId)
            if (preset == null) {
                _uiState.value = WorkoutUiState.Error("Preset not found")
                return@launch
            }
            val matrix = buildList {
                for (planned in preset.exercises) {
                    val name = catalogDao.getExerciseById(planned.exerciseId)?.name
                    if (name == null || planned.targetSets <= 0) continue
                    add(
                        StrengthExercise(
                            name = name,
                            sets = List(planned.targetSets) {
                                StrengthSet(
                                    kg = planned.defaultWeightKg,
                                    reps = planned.defaultReps
                                )
                            },
                            targetSets = planned.targetSets,
                            targetReps = planned.defaultReps,
                            targetWeightKg = planned.defaultWeightKg,
                            restSeconds = planned.restSeconds,
                            exerciseId = planned.exerciseId
                        )
                    )
                }
            }
            val session = WorkoutSession(
                type = WorkoutType.FITNESS.name,
                status = WorkoutStatus.ACTIVE.name,
                startTimestamp = clock(),
                setMatrix = matrix
            )
            lastTickMillis = clock()
            repository.saveSession(session)
            _uiState.value = WorkoutUiState.Countdown(
                session = session,
                secondsRemaining = COUNTDOWN_SECONDS
            )
            onHaptic(WorkoutHaptic.START)
        }
    }

    /**
     * Toggles a routine set's completion flag. Checking off a set persists
     * immediately (crash recovery) and starts the exercise's rest timer with a
     * heavy haptic; when it completes the exercise's final set a success
     * haptic fires instead. Unchecking simply persists the set as pending.
     */
    fun toggleSetCompleted(exerciseIndex: Int, setIndex: Int) {
        val current = _uiState.value as? WorkoutUiState.Active ?: return
        if (WorkoutType.fromName(current.session.type) != WorkoutType.FITNESS) return
        val matrix = current.session.setMatrix ?: return
        val exercise = matrix.getOrNull(exerciseIndex) ?: return
        if (!exercise.isPlanned) return
        val set = exercise.sets.getOrNull(setIndex) ?: return
        viewModelScope.launch(dispatcher) {
            val newCompleted = !set.completed
            val updatedSets = exercise.sets.mapIndexed { i, s ->
                if (i == setIndex) s.copy(completed = newCompleted) else s
            }
            val copyForward = newCompleted &&
                (exercise.targetSets ?: 0) > setIndex + 1
            val withProgression = if (copyForward) {
                val next = updatedSets.getOrNull(setIndex + 1)
                val pristine = next != null &&
                    !next.completed &&
                    next.kg == (exercise.targetWeightKg ?: next.kg) &&
                    next.reps == (exercise.targetReps ?: next.reps)
                if (pristine) {
                    updatedSets.mapIndexed { i, s ->
                        if (i == setIndex + 1) s.copy(kg = set.kg, reps = set.reps) else s
                    }
                } else {
                    updatedSets
                }
            } else {
                updatedSets
            }
            val updatedExercise = exercise.copy(sets = withProgression)
            val matrix = matrix.mapIndexed { i, ex ->
                if (i == exerciseIndex) updatedExercise else ex
            }
            val updated = current.session.copy(setMatrix = matrix)
            repository.saveSession(updated)
            _uiState.value = current.copy(
                session = updated,
                setMatrixError = null,
                restSeconds = if (newCompleted) {
                    exercise.restSeconds ?: DEFAULT_REST_SECONDS
                } else {
                    // Unchecking stops the rest period; a later re-check restarts it.
                    0
                }
            )
            if (newCompleted) {
                onHaptic(WorkoutHaptic.SET_COMPLETE)
                if (updatedExercise.sets.all { it.completed }) {
                    onHaptic(WorkoutHaptic.EXERCISE_COMPLETE)
                }
            }
        }
    }

    /**
     * Edits a routine set's weight/reps in place, mirroring the manual
     * set-validation rules (positive kg, reps >= 1). Invalid values surface
     * as an inline error without mutating.
     */
    fun updateRoutineSet(
        exerciseIndex: Int,
        setIndex: Int,
        kg: Double,
        reps: Int
    ) {
        val current = _uiState.value as? WorkoutUiState.Active ?: return
        if (WorkoutType.fromName(current.session.type) != WorkoutType.FITNESS) return
        val matrix = current.session.setMatrix ?: return
        val exercise = matrix.getOrNull(exerciseIndex) ?: return
        if (!exercise.isPlanned) return
        if (setIndex !in exercise.sets.indices) return
        val errors = ValidateStrengthExercise.validateSet(kg = kg, reps = reps)
        if (errors.isNotEmpty()) {
            _uiState.value = current.copy(
                setMatrixError = errors["kg"] ?: errors["reps"]
            )
            return
        }
        viewModelScope.launch(dispatcher) {
            val updatedSets = exercise.sets.mapIndexed { i, s ->
                if (i == setIndex) s.copy(kg = kg, reps = reps) else s
            }
            val updatedExercise = exercise.copy(sets = updatedSets)
            val updatedMatrix = matrix.mapIndexed { i, ex ->
                if (i == exerciseIndex) updatedExercise else ex
            }
            val updated = current.session.copy(setMatrix = updatedMatrix)
            repository.saveSession(updated)
            _uiState.value = current.copy(session = updated, setMatrixError = null)
        }
    }

    /**
     * Swaps a planned exercise mid-routine to another catalog movement,
     * keeping the planned target structure (sets, rest) but resetting every
     * set row to the planned defaults — performed weights, reps, RPE and
     * completion flags are cleared so the swapped-in movement starts fresh.
     */
    fun swapRoutineExercise(exerciseIndex: Int, exerciseId: String, name: String) {
        val current = _uiState.value as? WorkoutUiState.Active ?: return
        if (WorkoutType.fromName(current.session.type) != WorkoutType.FITNESS) return
        val matrix = current.session.setMatrix ?: return
        val exercise = matrix.getOrNull(exerciseIndex) ?: return
        if (!exercise.isPlanned) return
        viewModelScope.launch(dispatcher) {
            val targetSets = exercise.targetSets ?: exercise.sets.size
            val resets = (0 until targetSets).map {
                StrengthSet(
                    kg = exercise.targetWeightKg ?: 20.0,
                    reps = exercise.targetReps ?: 10,
                    rpe = null,
                    completed = false
                )
            }
            val swapped = exercise.copy(
                name = name,
                exerciseId = exerciseId,
                sets = resets
            )
            val updatedMatrix = matrix.mapIndexed { i, ex ->
                if (i == exerciseIndex) swapped else ex
            }
            val updated = current.session.copy(setMatrix = updatedMatrix)
            repository.saveSession(updated)
            _uiState.value = current.copy(
                session = updated,
                setMatrixError = null,
                // Replacing the exercise's target voids the in-flight rest
                // period; there is nothing meaningful to rest toward.
                restSeconds = 0
            )
        }
    }

    /**
     * Appends an extra planned set to a routine exercise mid-session, seeded
     * from its planned weight/reps targets, and grows [targetSets] so the
     * success/tonnage bookkeeping tracks the enlarged plan. Only meaningful
     * for planned (preset-built) exercises.
     */
    fun addRoutineSet(exerciseIndex: Int) {
        val current = _uiState.value as? WorkoutUiState.Active ?: return
        if (WorkoutType.fromName(current.session.type) != WorkoutType.FITNESS) return
        val matrix = current.session.setMatrix ?: return
        val exercise = matrix.getOrNull(exerciseIndex) ?: return
        if (!exercise.isPlanned) return
        viewModelScope.launch(dispatcher) {
            val added = StrengthSet(
                kg = exercise.targetWeightKg ?: 20.0,
                reps = exercise.targetReps ?: 10
            )
            val targetSets = 1 + (exercise.targetSets ?: exercise.sets.size)
            val grown = exercise.copy(
                sets = exercise.sets + added,
                targetSets = targetSets
            )
            val updatedMatrix = matrix.mapIndexed { i, ex ->
                if (i == exerciseIndex) grown else ex
            }
            val updated = current.session.copy(setMatrix = updatedMatrix)
            repository.saveSession(updated)
            _uiState.value = current.copy(session = updated, setMatrixError = null)
        }
    }

    fun finishSession() {
        val current = when (val state = _uiState.value) {
            is WorkoutUiState.Active -> state.session
            is WorkoutUiState.Paused -> state.session
            else -> return
        }
        viewModelScope.launch(dispatcher) {
            val end = clock()
            val calories = CalorieEstimator.estimate(
                type = WorkoutType.fromName(current.type) ?: WorkoutType.FITNESS,
                durationMinutes = current.elapsedSeconds / 60.0
            )
            val completed = current.copy(
                status = WorkoutStatus.COMPLETED.name,
                endTimestamp = end,
                elapsedSeconds = current.elapsedSeconds,
                calories = calories
            )
            repository.saveSession(completed)
            journalRepository.insert(
                JournalEntry(
                    timestamp = current.startTimestamp,
                    description = describe(completed, calories)
                )
            )
            val healthSynced = try {
                healthSource.writeRecord(completed.toHealthRecord(now = end))
            } catch (e: Exception) {
                false
            }
            _uiState.value = WorkoutUiState.Summary(
                session = completed,
                caloriesKcal = calories,
                healthSynced = healthSynced,
                tonnageKg = completed.setMatrix
                    ?.let(TonnageCalculator::tonnageKg),
                intervalRounds = completed.intervalState?.rounds ?: 0,
                intervalIntervals = completed.intervalState?.intervals ?: 0
            )
            onHaptic(WorkoutHaptic.STOP)
        }
    }

    /**
     * Saves a manually logged past workout: validates, persists a completed
     * session plus journal entry, syncs to Health Connect, and returns idle.
     */
    fun saveManualLog(
        type: WorkoutType?,
        durationMinutes: String,
        calories: String,
        timestamp: Long,
        note: String = ""
    ) {
        viewModelScope.launch(dispatcher) {
            val errors = ValidateWorkout.validateManualLog(
                type = type,
                durationMinutes = durationMinutes,
                calories = calories,
                timestamp = timestamp,
                now = clock()
            )
            if (errors.isNotEmpty() || type == null) {
                _uiState.value = WorkoutUiState.Error(
                    errors.values.firstOrNull() ?: "Invalid workout"
                )
                return@launch
            }
            val minutes = durationMinutes.trim().replace(',', '.').toDouble()
            val kcal = calories.trim().takeIf { it.isNotEmpty() }?.replace(',', '.')?.toDouble()
                ?: CalorieEstimator.estimate(type, minutes)
            val completed = WorkoutSession(
                type = type.name,
                status = WorkoutStatus.COMPLETED.name,
                startTimestamp = timestamp,
                endTimestamp = timestamp + (minutes * 60_000).toLong(),
                elapsedSeconds = (minutes * 60).toLong(),
                calories = kcal,
                notes = note
            )
            repository.saveSession(completed)
            journalRepository.insert(
                JournalEntry(
                    timestamp = timestamp,
                    description = describe(completed, kcal)
                )
            )
            try {
                healthSource.writeRecord(completed.toHealthRecord(now = clock()))
            } catch (e: Exception) {
                // Manual logs must not fail when Health Connect is unavailable.
            }
            _uiState.value = WorkoutUiState.Idle
        }
    }

    fun resumeRecovery() {
        val current = _uiState.value as? WorkoutUiState.RecoveryRequired ?: return
        viewModelScope.launch(dispatcher) {
            val active = current.session.copy(status = WorkoutStatus.ACTIVE.name)
            lastTickMillis = clock()
            repository.saveSession(active)
            _uiState.value = WorkoutUiState.Active(active)
            onHaptic(WorkoutHaptic.START)
        }
    }

    fun discardRecovery() {
        val current = _uiState.value as? WorkoutUiState.RecoveryRequired ?: return
        viewModelScope.launch(dispatcher) {
            repository.saveSession(
                current.session.copy(status = WorkoutStatus.DISCARDED.name)
            )
            _uiState.value = WorkoutUiState.Idle
        }
    }

    fun dismissError() {
        if (_uiState.value is WorkoutUiState.Error) {
            _uiState.value = WorkoutUiState.Idle
        }
    }

    /** Returns from configuration or summary to the hub. */
    fun cancelConfiguring() {
        if (_uiState.value is WorkoutUiState.Configuring) {
            _uiState.value = WorkoutUiState.Idle
        }
    }

    /** Dismisses the finished-workout summary back to the hub. */
    fun closeSummary() {
        if (_uiState.value is WorkoutUiState.Summary) {
            _uiState.value = WorkoutUiState.Idle
        }
    }

    private fun describe(session: WorkoutSession, caloriesKcal: Double): String {
        val typeLabel = WorkoutType.fromName(session.type)?.label ?: session.type
        val minutes = session.elapsedSeconds / 60.0
        val minutesText = if (minutes == minutes.toLong().toDouble()) {
            "${minutes.toLong()} min"
        } else {
            "%.1f min".format(minutes)
        }
        val caloriesText = if (caloriesKcal == caloriesKcal.toLong().toDouble()) {
            "${caloriesKcal.toLong()} kcal"
        } else {
            "%.1f kcal".format(caloriesKcal)
        }
        val base = "Workout: $typeLabel, $minutesText, $caloriesText"
        val details = buildList {
            session.setMatrix
                ?.takeIf { it.isNotEmpty() }
                ?.let { add("Tonnage: ${formatCompact(TonnageCalculator.tonnageKg(it))} kg") }
            session.intervalState?.let {
                if (it.intervals > 0) add("Rounds: ${it.rounds} · Intervals: ${it.intervals}")
            }
        }
        val withDetails = if (details.isEmpty()) base else (listOf(base) + details).joinToString("\n")
        return if (session.notes.isNotBlank()) "$withDetails\n${session.notes}" else withDetails
    }

    companion object {
        /** Session rows are rewritten on these elapsed-second boundaries. */
        const val PERSIST_EVERY_SECONDS = 5L

        /** Pre-session countdown length in seconds (3-2-1). */
        const val COUNTDOWN_SECONDS = 3

        /** Between-sets rest timer length in seconds for the set matrix. */
        const val DEFAULT_REST_SECONDS = 60
    }
}

class WorkoutViewModelFactory(
    private val repository: WorkoutRepository,
    private val healthSource: WorkoutHealthDataSource,
    private val journalRepository: JournalRepository,
    private val onHaptic: (WorkoutHaptic) -> Unit = {},
    private val presetRepository: PresetRepository? = null,
    private val catalogDao: ExerciseCatalogDao? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutViewModel(
                repository = repository,
                healthSource = healthSource,
                journalRepository = journalRepository,
                onHaptic = onHaptic,
                presetRepository = presetRepository,
                catalogDao = catalogDao
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
