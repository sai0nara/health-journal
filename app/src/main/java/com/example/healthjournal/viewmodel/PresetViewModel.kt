package com.example.healthjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthjournal.data.PresetRepository
import com.example.healthjournal.data.local.ExerciseCatalogDao
import com.example.healthjournal.data.local.ExerciseCatalogItem
import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.domain.PresetExercise
import com.example.healthjournal.domain.ScheduledDay
import com.example.healthjournal.domain.ValidatePreset
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * MVI ViewModel for preset management: navigating between library and the
 * create/edit draft, inline validation, and CRUD through [PresetRepository].
 * State transitions are JVM-testable via the injected [dispatcher].
 */
class PresetViewModel(
    private val repository: PresetRepository,
    private val catalogDao: ExerciseCatalogDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) : ViewModel() {

    private val _uiState = MutableStateFlow<PresetUiState>(PresetUiState.Idle)
    val uiState: StateFlow<PresetUiState> = _uiState

    /** Preset library for the list screen, newest-edited first. */
    val presets: StateFlow<List<WorkoutPreset>> =
        repository.presets.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    /** Full exercise catalog for the preset-building dropdown, grouped by muscle category. */
    val catalog: StateFlow<List<ExerciseCatalogItem>> =
        catalogDao.getAllExercises().stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun showLibrary() {
        if (_uiState.value !is PresetUiState.Library) {
            _uiState.value = PresetUiState.Library
        }
    }

    /** Opens a blank create draft. */
    fun openCreate() {
        _uiState.value = PresetUiState.Editing(presetId = null)
    }

    /** Loads [presetId] into an edit draft; unknown ids fall back to the library. */
    fun openEdit(presetId: String) {
        viewModelScope.launch(dispatcher) {
            val preset = repository.getPreset(presetId)
            _uiState.value = if (preset == null) {
                PresetUiState.Library
            } else {
                PresetUiState.Editing(
                    presetId = preset.id,
                    name = preset.name,
                    scheduledDay = runCatching { ScheduledDay.valueOf(preset.scheduledDay) }
                        .getOrDefault(ScheduledDay.ANY),
                    exercises = preset.exercises
                )
            }
        }
    }

    fun updateName(text: String) {
        val current = _uiState.value as? PresetUiState.Editing ?: return
        _uiState.value = current.copy(name = text, nameError = null)
    }

    fun updateScheduledDay(day: ScheduledDay) {
        val current = _uiState.value as? PresetUiState.Editing ?: return
        _uiState.value = current.copy(scheduledDay = day)
    }

    /**
     * Adds [exercise] to the draft. Invalid per-exercise defaults surface
     * as an inline error without mutating the draft.
     */
    fun addExercise(exercise: PresetExercise) {
        val current = _uiState.value as? PresetUiState.Editing ?: return
        val error = ValidatePreset.validateExercise(exercise)
        if (error != null) {
            _uiState.value = current.copy(exerciseError = error)
            return
        }
        _uiState.value = current.copy(
            exercises = current.exercises + exercise,
            exerciseError = null
        )
    }

    /** Removes the exercise at [index]; out-of-range indexes are ignored. */
    fun removeExercise(index: Int) {
        val current = _uiState.value as? PresetUiState.Editing ?: return
        if (index !in current.exercises.indices) return
        _uiState.value = current.copy(
            exercises = current.exercises.filterIndexed { i, _ -> i != index }
        )
    }

    /**
     * Replaces the exercise at [index] with [exercise]. Invalid per-exercise
     * defaults surface as an inline error without mutating the draft.
     */
    fun updateExercise(index: Int, exercise: PresetExercise) {
        val current = _uiState.value as? PresetUiState.Editing ?: return
        if (index !in current.exercises.indices) return
        val error = ValidatePreset.validateExercise(exercise)
        if (error != null) {
            _uiState.value = current.copy(exerciseError = error)
            return
        }
        _uiState.value = current.copy(
            exercises = current.exercises.mapIndexed { i, existing ->
                if (i == index) exercise else existing
            },
            exerciseError = null
        )
    }

    /**
     * Validates the draft and persists it, backing to the library on
     * success; inline errors keep the screen on the draft otherwise.
     */
    fun savePreset() {
        val current = _uiState.value as? PresetUiState.Editing ?: return
        val nameError = ValidatePreset.validateName(current.name)
        val exerciseError = ValidatePreset.validateExercises(current.exercises)
        if (nameError != null || exerciseError != null) {
            _uiState.value = current.copy(
                nameError = nameError,
                exerciseError = exerciseError
            )
            return
        }
        viewModelScope.launch(dispatcher) {
            repository.savePreset(
                WorkoutPreset(
                    id = current.presetId ?: UUID.randomUUID().toString(),
                    name = current.name.trim(),
                    scheduledDay = current.scheduledDay.name,
                    exercises = current.exercises
                )
            )
            _uiState.value = PresetUiState.Library
        }
    }

    fun deletePreset(presetId: String) {
        viewModelScope.launch(dispatcher) {
            repository.deletePreset(presetId)
            if ((_uiState.value as? PresetUiState.Editing)?.presetId == presetId) {
                _uiState.value = PresetUiState.Library
            }
        }
    }

    /** Discards the draft and returns to the library without persisting. */
    fun cancelEditing() {
        if (_uiState.value is PresetUiState.Editing) {
            _uiState.value = PresetUiState.Library
        }
    }
}

class PresetViewModelFactory(
    private val repository: PresetRepository,
    private val catalogDao: ExerciseCatalogDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PresetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PresetViewModel(
                repository = repository,
                catalogDao = catalogDao
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}