package com.example.healthjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthjournal.data.BodyMeasurementRepository
import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.UnitConverter
import com.example.healthjournal.data.local.UnitSystem
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
    /** Inline alert shown under the date row when the form is future-dated. */
    val timestampError: String? = null,
    val canSave: Boolean = false,
    val isSaving: Boolean = false,
    /** Display unit system for entry labels; storage stays canonical metric. */
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    /** One-shot flag consumed by the UI (haptic + dismiss), reset via [BodyMeasurementViewModel.onSavedHandled]. */
    val justSaved: Boolean = false
)

class BodyMeasurementViewModel(
    private val repository: BodyMeasurementRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    unitSystem: UnitSystem = UnitSystem.METRIC
) : ViewModel() {

    private val _uiState = MutableStateFlow(BodyMeasurementUiState(unitSystem = unitSystem))
    val uiState: StateFlow<BodyMeasurementUiState> = _uiState.asStateFlow()

    companion object {
        /** Inline alert text for future-dated forms; Save stays disabled while set. */
        const val ERROR_FUTURE_DATE = "Future dates cannot be saved"
    }

    /** Chronological feed (newest first) backing the Measurements screen. */
    private val _entries = MutableStateFlow(emptyList<BodyMeasurementEntry>())
    val entries: StateFlow<List<BodyMeasurementEntry>> = _entries.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            repository.allEntries.collect { _entries.value = it }
        }
    }

    /** Snapshots records before removal so Undo can restore them verbatim. */
    private val pendingUndoSnapshots = mutableMapOf<String, BodyMeasurementEntry>()

    fun deleteEntry(entryId: String) {
        // Snapshot from the already-observed list: no extra DB read and no
        // race if the row vanishes between UI tap and deletion.
        _entries.value.firstOrNull { it.entry_id == entryId }?.let { snapshot ->
            // Remove-then-put keeps re-deleted entries at the end so the
            // default LIFO undo always targets the most recent deletion.
            pendingUndoSnapshots.remove(entryId)
            pendingUndoSnapshots[entryId] = snapshot
        }
        viewModelScope.launch(ioDispatcher) {
            repository.deleteEntry(entryId)
        }
    }

    /**
     * Re-inserts a deleted record (Undo snackbar action). Without an explicit
     * [entryId] the most recently deleted record is restored (LIFO), so rapid
     * successive deletions each remain individually restorable.
     */
    fun undoDelete(entryId: String? = null) {
        val targetId = entryId ?: pendingUndoSnapshots.keys.lastOrNull() ?: return
        val snapshot = pendingUndoSnapshots.remove(targetId) ?: return
        viewModelScope.launch(ioDispatcher) {
            repository.insert(snapshot)
        }
    }

    fun onFieldChanged(field: MeasurementField, text: String) {
        _uiState.update { current ->
            val rawValues = current.rawValues + (field to text)
            val fieldErrors = ValidateMeasurements.validate(rawValues, current.unitSystem)
            current.copy(
                rawValues = rawValues,
                fieldErrors = fieldErrors,
                timestampError = futureDateError(current.timestamp),
                canSave = deriveCanSave(fieldErrors, rawValues, current.timestamp)
            )
        }
    }

    /**
     * Switches the entry display units, converting any in-progress draft text
     * through the shared converter (parse in the old system, format in the
     * new one) so toggling never loses typed values. Blanks stay blank.
     */
    fun onUnitSystemChanged(unitSystem: UnitSystem) {
        _uiState.update { current ->
            if (current.unitSystem == unitSystem) return@update current
            val converted = current.rawValues.mapValues { (field, raw) ->
                val metric = ValidateMeasurements.parseMetric(raw, field, current.unitSystem)
                if (metric == null) raw
                else UnitConverter.formatMeasurement(
                    metric,
                    unitSystem,
                    isWeight = field == MeasurementField.WEIGHT
                )
            }
            val fieldErrors = ValidateMeasurements.validate(converted, unitSystem)
            current.copy(
                rawValues = converted,
                fieldErrors = fieldErrors,
                timestampError = futureDateError(current.timestamp),
                canSave = deriveCanSave(fieldErrors, converted, current.timestamp),
                unitSystem = unitSystem
            )
        }
    }

    fun onTimestampChanged(timestampMillis: Long) {
        _uiState.update {
            it.copy(
                timestamp = timestampMillis,
                timestampError = futureDateError(timestampMillis),
                canSave = deriveCanSave(it.fieldErrors, it.rawValues, timestampMillis)
            )
        }
    }

    private fun futureDateError(timestampMillis: Long): String? =
        if (timestampMillis > System.currentTimeMillis()) ERROR_FUTURE_DATE else null

    private fun deriveCanSave(
        fieldErrors: Map<MeasurementField, String>,
        rawValues: Map<MeasurementField, String>,
        timestampMillis: Long
    ): Boolean =
        fieldErrors.isEmpty() &&
            ValidateMeasurements.hasAtLeastOneMeasurement(rawValues) &&
            futureDateError(timestampMillis) == null

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
            ?.let { ValidateMeasurements.parseMetric(it, field, unitSystem) }
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
