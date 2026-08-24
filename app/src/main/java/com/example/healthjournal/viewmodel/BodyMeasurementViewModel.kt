package com.example.healthjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthjournal.data.BodyMeasurementRepository
import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.domain.MeasurementField
import com.example.healthjournal.domain.ValidateMeasurements
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable UI state for the measurement capture sheet.
 */
data class BodyMeasurementUiState(
    val timestamp: Long = System.currentTimeMillis(),
    val rawValues: Map<MeasurementField, String> =
        MeasurementField.entries.associateWith { "" },
    val fieldErrors: Map<MeasurementField, String> = emptyMap(),
    val canSave: Boolean = false,
    val isSaving: Boolean = false,
    /** One-shot flag consumed by the UI (haptic + dismiss), reset via [BodyMeasurementViewModel.onSavedHandled]. */
    val justSaved: Boolean = false
)

class BodyMeasurementViewModel(
    private val repository: BodyMeasurementRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(BodyMeasurementUiState())
    val uiState: StateFlow<BodyMeasurementUiState> = _uiState.asStateFlow()

    /** Chronological feed (newest first) backing the Measurements screen. */
    private val _entries = MutableStateFlow(emptyList<BodyMeasurementEntry>())
    val entries: StateFlow<List<BodyMeasurementEntry>> = _entries.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            repository.allEntries.collect { _entries.value = it }
        }
    }

    /** Snapshots the record before removal so Undo can restore it verbatim. */
    private var pendingUndoSnapshot: BodyMeasurementEntry? = null

    fun deleteEntry(entryId: String) {
        // Snapshot from the already-observed list: no extra DB read and no
        // race if the row vanishes between UI tap and deletion.
        pendingUndoSnapshot = _entries.value.firstOrNull { it.entry_id == entryId }
        viewModelScope.launch(ioDispatcher) {
            repository.deleteEntry(entryId)
        }
    }

    /** Re-inserts the most recently deleted record (Undo snackbar action). */
    fun undoDelete() {
        val snapshot = pendingUndoSnapshot ?: return
        viewModelScope.launch(ioDispatcher) {
            repository.insert(snapshot)
            pendingUndoSnapshot = null
        }
    }

    fun onFieldChanged(field: MeasurementField, text: String) {
        _uiState.update { current ->
            val rawValues = current.rawValues + (field to text)
            val fieldErrors = ValidateMeasurements.validate(rawValues)
            current.copy(
                rawValues = rawValues,
                fieldErrors = fieldErrors,
                canSave = fieldErrors.isEmpty() &&
                    ValidateMeasurements.hasAtLeastOneMeasurement(rawValues)
            )
        }
    }

    fun onTimestampChanged(timestampMillis: Long) {
        _uiState.update { it.copy(timestamp = timestampMillis) }
    }

    fun onSaveClicked() {
        val state = _uiState.value
        if (!state.canSave || state.isSaving) return
        // Parity with JournalViewModel.addEntry: future-dated records are
        // rejected at save time instead of polluting the history feed.
        if (state.timestamp > System.currentTimeMillis()) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch(ioDispatcher) {
            repository.insert(state.toEntry())
            _uiState.update {
                BodyMeasurementUiState().copy(justSaved = true)
            }
        }
    }

    /** Called by the UI after consuming [BodyMeasurementUiState.justSaved]. */
    fun onSavedHandled() {
        _uiState.update { it.copy(justSaved = false) }
    }

    private fun BodyMeasurementUiState.toEntry(): BodyMeasurementEntry =
        BodyMeasurementEntry(
            timestamp = timestamp,
            lastModified = timestamp,
            weight_kg = parsedValue(MeasurementField.WEIGHT),
            chest_cm = parsedValue(MeasurementField.CHEST),
            waist_cm = parsedValue(MeasurementField.WAIST),
            glute_cm = parsedValue(MeasurementField.GLUTE),
            thigh_cm = parsedValue(MeasurementField.THIGH),
            calf_cm = parsedValue(MeasurementField.CALF),
            bicep_cm = parsedValue(MeasurementField.BICEP),
            isSynced = false,
            syncStatus = "PENDING_SYNC"
        )

    private fun BodyMeasurementUiState.parsedValue(field: MeasurementField): Double? =
        rawValues[field]
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { ValidateMeasurements.parseDecimal(it) }
}

class BodyMeasurementViewModelFactory(
    private val repository: BodyMeasurementRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BodyMeasurementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BodyMeasurementViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
