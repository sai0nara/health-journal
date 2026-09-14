package com.example.healthjournal.viewmodel

import com.example.healthjournal.data.PresetRepository
import com.example.healthjournal.data.local.ExerciseCatalogItem
import com.example.healthjournal.data.local.FakeExerciseCatalogDao
import com.example.healthjournal.data.local.FakeWorkoutPresetDao
import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.domain.PresetExercise
import com.example.healthjournal.domain.ScheduledDay
import com.example.healthjournal.domain.ValidatePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PresetViewModel preset CRUD + validation: MVI navigation
 * between library and editing, inline validation errors, per-exercise
 * defaults preserved through the save path, and edit/delete semantics.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PresetViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var presetDao: FakeWorkoutPresetDao
    private lateinit var catalogDao: FakeExerciseCatalogDao
    private lateinit var repository: PresetRepository

    private val benchPress = PresetExercise(
        exerciseId = "bench-press",
        targetSets = 3,
        defaultReps = 5,
        defaultWeightKg = 60.0,
        restSeconds = 90
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        presetDao = FakeWorkoutPresetDao()
        catalogDao = FakeExerciseCatalogDao()
        repository = PresetRepository(presetDao)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = PresetViewModel(
        repository = repository,
        catalogDao = catalogDao,
        dispatcher = dispatcher
    )

    @Test
    fun initialState_isIdle() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is PresetUiState.Idle)
    }

    @Test
    fun openCreate_movesToEditing_withBlankDraft() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.openCreate()

        val state = vm.uiState.value as PresetUiState.Editing
        assertNull(state.presetId)
        assertEquals("", state.name)
        assertEquals(ScheduledDay.ANY, state.scheduledDay)
        assertTrue(state.exercises.isEmpty())
    }

    @Test
    fun showLibrary_fromIdleOrEditing_landsOnLibrary() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.showLibrary()
        assertTrue(vm.uiState.value is PresetUiState.Library)

        vm.openCreate()
        vm.showLibrary()
        assertTrue(vm.uiState.value is PresetUiState.Library)
    }

    @Test
    fun create_withValidPreset_persistsAndBacksToLibrary() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.openCreate()
        vm.updateName("Leg Day")
        vm.updateScheduledDay(ScheduledDay.MONDAY)
        vm.addExercise(benchPress)
        vm.savePreset()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is PresetUiState.Library)
        val saved = presetDao.visiblePresets().let { presets ->
            assertEquals(1, presets.size)
            presets.single()
        }
        assertEquals("Leg Day", saved.name)
        assertEquals(ScheduledDay.MONDAY.name, saved.scheduledDay)
        assertEquals(listOf(benchPress), saved.exercises)
    }

    @Test
    fun savePreset_withBlankName_surfacesInlineError_andDoesNotPersist() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.openCreate()
        vm.updateName("   ")
        vm.addExercise(benchPress)

        vm.savePreset()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as PresetUiState.Editing
        assertEquals(ValidatePreset.ERROR_NAME_REQUIRED, state.nameError)
        assertTrue(presetDao.visiblePresets().isEmpty())
    }

    @Test
    fun savePreset_withoutExercises_surfacesExerciseError_andDoesNotPersist() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.openCreate()
        vm.updateName("Upper")

        vm.savePreset()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as PresetUiState.Editing
        assertEquals(ValidatePreset.ERROR_EXERCISES_REQUIRED, state.exerciseError)
        assertTrue(presetDao.visiblePresets().isEmpty())
    }

    @Test
    fun updateName_withExistingDraft_clearsPreviousNameError() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.openCreate()
        vm.addExercise(benchPress)
        vm.savePreset()
        dispatcher.scheduler.advanceUntilIdle()

        vm.updateName("Leg Day")
        val state = vm.uiState.value as PresetUiState.Editing
        assertNull(state.nameError)
    }

    @Test
    fun addExercise_withInvalidDefaults_showsInlineError_andDoesNotAppend() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.openCreate()

        vm.addExercise(benchPress.copy(defaultWeightKg = 0.0))

        val state = vm.uiState.value as PresetUiState.Editing
        assertEquals(ValidatePreset.ERROR_WEIGHT_POSITIVE, state.exerciseError)
        assertTrue(state.exercises.isEmpty())
    }

    @Test
    fun addExercise_withInvalidReps_showsInlineError_andDoesNotAppend() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.openCreate()

        vm.addExercise(benchPress.copy(defaultReps = 0))

        val state = vm.uiState.value as PresetUiState.Editing
        assertEquals(ValidatePreset.ERROR_REPS_POSITIVE, state.exerciseError)
        assertTrue(state.exercises.isEmpty())
    }

    @Test
    fun addExercise_withInvalidSets_showsInlineError_andDoesNotAppend() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.openCreate()

        vm.addExercise(benchPress.copy(targetSets = 0))

        val state = vm.uiState.value as PresetUiState.Editing
        assertEquals(ValidatePreset.ERROR_SETS_POSITIVE, state.exerciseError)
        assertTrue(state.exercises.isEmpty())
    }

    @Test
    fun addExercise_appendsAndClearsPriorExerciseError() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.openCreate()
        vm.addExercise(benchPress.copy(defaultWeightKg = 0.0))

        vm.addExercise(benchPress)

        val state = vm.uiState.value as PresetUiState.Editing
        assertNull(state.exerciseError)
        assertEquals(listOf(benchPress), state.exercises)
    }

    @Test
    fun removeExercise_removesOnlyThatExercise() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.openCreate()
        vm.addExercise(benchPress)
        vm.addExercise(benchPress.copy(exerciseId = "squat", defaultWeightKg = 100.0))

        vm.removeExercise(0)

        val state = vm.uiState.value as PresetUiState.Editing
        assertEquals(listOf(benchPress.copy(exerciseId = "squat", defaultWeightKg = 100.0)), state.exercises)
    }

    @Test
    fun updateExercise_replacesDefaultsAtThatIndex() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.openCreate()
        vm.addExercise(benchPress)
        vm.addExercise(benchPress.copy(exerciseId = "squat", defaultWeightKg = 100.0))

        vm.updateExercise(0, benchPress.copy(defaultWeightKg = 65.0, defaultReps = 8))

        val state = vm.uiState.value as PresetUiState.Editing
        assertEquals(
            listOf(benchPress.copy(defaultWeightKg = 65.0, defaultReps = 8), benchPress.copy(exerciseId = "squat", defaultWeightKg = 100.0)),
            state.exercises
        )
    }

    @Test
    fun updateExercise_withInvalidDefaults_showsInlineError_andLeavesDraft() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.openCreate()
        vm.addExercise(benchPress)

        vm.updateExercise(0, benchPress.copy(defaultReps = 0))

        val state = vm.uiState.value as PresetUiState.Editing
        assertEquals(ValidatePreset.ERROR_REPS_POSITIVE, state.exerciseError)
        assertEquals(listOf(benchPress), state.exercises)
    }

    @Test
    fun updateExercise_outOfRangeIndex_isIgnored() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.openCreate()
        vm.addExercise(benchPress)

        vm.updateExercise(5, benchPress.copy(defaultWeightKg = 80.0))

        val state = vm.uiState.value as PresetUiState.Editing
        assertEquals(listOf(benchPress), state.exercises)
    }

    @Test
    fun edit_loadsExistingPresetIntoDraft() = runTest {
        repository.savePreset(
            WorkoutPreset(
                id = "p1",
                name = "Old Name",
                scheduledDay = ScheduledDay.THURSDAY.name,
                exercises = listOf(benchPress)
            )
        )
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.openEdit("p1")
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as PresetUiState.Editing
        assertEquals("p1", state.presetId)
        assertEquals("Old Name", state.name)
        assertEquals(ScheduledDay.THURSDAY, state.scheduledDay)
        assertEquals(listOf(benchPress), state.exercises)
    }

    @Test
    fun saveEdit_updatesExistingRow_keepingId() = runTest {
        repository.savePreset(WorkoutPreset(id = "p1", name = "Old Name"))
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.openEdit("p1")
        dispatcher.scheduler.advanceUntilIdle()
        vm.updateName("New Name")
        vm.addExercise(benchPress)
        vm.savePreset()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is PresetUiState.Library)
        val updated = presetDao.visiblePresets().let { presets ->
            assertEquals(1, presets.size)
            presets.single()
        }
        assertEquals("p1", updated.id)
        assertEquals("New Name", updated.name)
        assertEquals(listOf(benchPress), updated.exercises)
    }

    @Test
    fun openEdit_unknownId_returnsToLibrary() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.openEdit("missing")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is PresetUiState.Library)
    }

    @Test
    fun deletePreset_removesFromStore() = runTest {
        repository.savePreset(WorkoutPreset(id = "p1", name = "Leg Day"))
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.showLibrary()

        vm.deletePreset("p1")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(presetDao.visiblePresets().isEmpty())
    }

    @Test
    fun cancelEditing_returnsToLibrary_withoutPersisting() = runTest {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.openCreate()
        vm.updateName("Leg Day")
        vm.addExercise(benchPress)

        vm.cancelEditing()

        assertTrue(vm.uiState.value is PresetUiState.Library)
        assertTrue(presetDao.visiblePresets().isEmpty())
    }

    @Test
    fun catalog_exposesSeededExercisesForDropdown() = runTest {
        catalogDao.insertAll(
            listOf(
                ExerciseCatalogItem(id = "bench-press", name = "Bench Press", muscleCategory = "Chest"),
                ExerciseCatalogItem(id = "squat", name = "Squat", muscleCategory = "Legs")
            )
        )
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, vm.catalog.value.size)
    }
}