package com.example.healthjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthjournal.data.BodyMeasurementRepository
import com.example.healthjournal.data.GoalsRepository
import com.example.healthjournal.domain.MeasurementField
import com.example.healthjournal.domain.toParamTrend
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state for the Body Analytics section (tab selection + goal targets). */
data class BodyAnalyticsUiState(
    val selectedTab: MeasurementField = MeasurementField.WEIGHT,
    /** Goal target per parameter id (MeasurementField.name), metric units. */
    val goalTargets: Map<String, Double> = emptyMap()
)

/**
 * Drives the tabbed per-parameter trend charts on the Measurements screen:
 * derives each parameter's chronological series from the shared measurement
 * feed and exposes stored goal targets for goal-line rendering.
 */
class BodyAnalyticsViewModel(
    repository: BodyMeasurementRepository,
    goalsRepository: GoalsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(BodyAnalyticsUiState())
    val uiState: StateFlow<BodyAnalyticsUiState> = _uiState.asStateFlow()

    private val entries = repository.allEntries

    /** Series for the active tab: ascending timestamps ready for plotting. */
    val series: StateFlow<List<Pair<Long, Double>>> =
        combine(entries, _uiState) { list, state -> list.toParamTrend(state.selectedTab) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch(ioDispatcher) {
            goalsRepository.goals.collect { rows ->
                _uiState.update { current ->
                    current.copy(goalTargets = rows.associate { it.parameterId to it.target })
                }
            }
        }
    }

    fun onTabSelected(field: MeasurementField) {
        _uiState.update { it.copy(selectedTab = field) }
    }
}
