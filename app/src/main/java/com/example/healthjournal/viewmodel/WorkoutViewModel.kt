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
import com.example.healthjournal.domain.ValidateWorkout
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
    STOP
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

    /** Workout history for discovery, newest first. */
    val recentSessions: StateFlow<List<WorkoutSession>> =
        repository.sessions.stateIn(
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
            _uiState.value = WorkoutUiState.Active(session)
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
     * Advances the running clock. Persistence is batched: the session row is
     * rewritten only on five-second boundaries (plus always on pause/finish)
     * instead of on every tick.
     */
    fun advanceTime(seconds: Long) {
        val current = _uiState.value as? WorkoutUiState.Active ?: return
        viewModelScope.launch(dispatcher) {
            val elapsed = current.session.elapsedSeconds + seconds
            val advanced = current.session.copy(elapsedSeconds = elapsed)
            _uiState.value = WorkoutUiState.Active(advanced)
            if (elapsed % PERSIST_EVERY_SECONDS == 0L) {
                repository.saveSession(advanced)
            }
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
                healthSynced = healthSynced
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
