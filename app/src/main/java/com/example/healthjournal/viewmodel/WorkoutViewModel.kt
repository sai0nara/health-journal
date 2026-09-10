package com.example.healthjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.WorkoutRepository
import com.example.healthjournal.data.local.JournalEntry
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
import com.example.healthjournal.health.WorkoutHealthDataSource
import com.example.healthjournal.health.toHealthRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Haptic cues the UI plays for workout control events. */
enum class WorkoutHaptic {
    START,
    STOP,
    INTERVAL
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
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _uiState = MutableStateFlow<WorkoutUiState>(WorkoutUiState.Idle)
    val uiState: StateFlow<WorkoutUiState> = _uiState

    /** Workout history for discovery, newest first (completed only). */
    val recentSessions: StateFlow<List<WorkoutSession>> =
        repository.completedSessions.stateIn(
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
            // Run targets are entered in kilometres; everything else in minutes.
            val targetDistanceM = if (current.type == WorkoutType.RUN) targetValue * 1_000 else null
            val targetDurationMin = if (current.type == WorkoutType.RUN) null else targetValue
            val session = WorkoutSession(
                type = current.type.name,
                status = WorkoutStatus.ACTIVE.name,
                startTimestamp = clock(),
                targetDistanceM = targetDistanceM,
                targetDurationMin = targetDurationMin
            )
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
            repository.saveSession(active)
            _uiState.value = WorkoutUiState.Active(active)
            onHaptic(WorkoutHaptic.START)
        }
    }

    /**
     * Advances either the pre-session countdown or the running clock.
     * During the countdown the session row is left untouched; once it hits
     * zero the session goes Active at zero elapsed time. In Active, persistence
     * is batched: the session row is rewritten only on five-second boundaries
     * (plus always on pause/finish) instead of on every tick. Rest-timer
     * seconds also tick down here so one clock source drives both.
     */
    fun advanceTime(seconds: Long) {
        val current = _uiState.value
        when (current) {
            is WorkoutUiState.Countdown -> {
                val remaining = current.secondsRemaining - seconds.toInt()
                if (remaining <= 0) {
                    _uiState.value = WorkoutUiState.Active(current.session)
                } else {
                    _uiState.value = current.copy(secondsRemaining = remaining)
                }
            }
            is WorkoutUiState.Active -> viewModelScope.launch(dispatcher) {
                val elapsed = current.session.elapsedSeconds + seconds
                val advanced = current.session.copy(elapsedSeconds = elapsed)
                _uiState.value = WorkoutUiState.Active(
                    session = advanced,
                    restSeconds = (current.restSeconds - seconds).coerceAtLeast(0L).toInt(),
                    setMatrixError = current.setMatrixError
                )
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
        if (WorkoutType.valueOf(current.session.type) != WorkoutType.HIIT) return
        val tracker = current.session.intervalState ?: WorkoutIntervalSession()
        val advance = tracker.advance()
        viewModelScope.launch(dispatcher) {
            val updated = current.session.copy(intervalState = advance.session)
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
        if (WorkoutType.valueOf(current.session.type) != WorkoutType.FITNESS) return
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
        if (WorkoutType.valueOf(current.session.type) != WorkoutType.FITNESS) return
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

    fun finishSession() {
        val current = when (val state = _uiState.value) {
            is WorkoutUiState.Active -> state.session
            is WorkoutUiState.Paused -> state.session
            else -> return
        }
        viewModelScope.launch(dispatcher) {
            val end = clock()
            val calories = CalorieEstimator.estimate(
                type = WorkoutType.valueOf(current.type),
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
        timestamp: Long
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
                calories = kcal
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
        val type = WorkoutType.valueOf(session.type)
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
        val base = "Workout: ${type.label}, $minutesText, $caloriesText"
        return if (session.notes.isNotBlank()) "$base\n${session.notes}" else base
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
    private val onHaptic: (WorkoutHaptic) -> Unit = {}
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutViewModel(
                repository = repository,
                healthSource = healthSource,
                journalRepository = journalRepository,
                onHaptic = onHaptic
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
