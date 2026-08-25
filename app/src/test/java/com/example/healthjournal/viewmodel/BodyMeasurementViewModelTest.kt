package com.example.healthjournal.viewmodel

import com.example.healthjournal.data.BodyMeasurementRepository
import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.domain.MeasurementField
import com.example.healthjournal.domain.ValidateMeasurements
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BodyMeasurementViewModelTest {

    private lateinit var viewModel: BodyMeasurementViewModel
    private val repository: BodyMeasurementRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.insert(any()) } returns Unit
        coEvery { repository.allEntries } returns mockk(relaxed = true)
        viewModel = BodyMeasurementViewModel(repository, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun currentState() = viewModel.uiState.value

    private fun set(field: MeasurementField, value: String) =
        viewModel.onFieldChanged(field, value)

    @Test
    fun initialState_hasDefaults() {
        val state = currentState()

        assertTrue(state.timestamp > 0)
        assertEquals(MeasurementField.entries.size, state.rawValues.size)
        assertTrue(state.rawValues.values.all { it.isEmpty() })
        assertTrue(state.fieldErrors.isEmpty())
        assertFalse(state.canSave)
        assertFalse(state.isSaving)
        assertFalse(state.justSaved)
    }

    @Test
    fun onFieldChanged_updatesValueWithoutTouchingOthers() {
        set(MeasurementField.WEIGHT, "78.5")
        set(MeasurementField.WAIST, "85")

        val raw = currentState().rawValues
        assertEquals("78.5", raw[MeasurementField.WEIGHT])
        assertEquals("85", raw[MeasurementField.WAIST])
        assertEquals("", raw[MeasurementField.BICEP])
    }

    @Test
    fun onFieldChanged_malformedInput_setsInlineErrorAndRetainsTypedValue() {
        set(MeasurementField.WEIGHT, "abc")

        val state = currentState()

        assertEquals(
            ValidateMeasurements.ERROR_INVALID_FORMAT,
            state.fieldErrors[MeasurementField.WEIGHT]
        )
        assertEquals("abc", state.rawValues[MeasurementField.WEIGHT])
    }

    @Test
    fun onFieldChanged_validInput_clearsPreviousError() {
        set(MeasurementField.WEIGHT, "abc")
        set(MeasurementField.WEIGHT, "78.5")

        assertFalse(currentState().fieldErrors.containsKey(MeasurementField.WEIGHT))
    }

    @Test
    fun canSave_falseWhenAllFieldsBlank() {
        set(MeasurementField.WEIGHT, "123")
        set(MeasurementField.WEIGHT, "")

        assertFalse(currentState().canSave)
    }

    @Test
    fun canSave_trueWithSingleValidPartialValue() {
        set(MeasurementField.WAIST, "85")

        assertTrue(currentState().canSave)
    }

    @Test
    fun canSave_falseWhileAnyFieldHasError() {
        set(MeasurementField.WAIST, "85")
        set(MeasurementField.WEIGHT, "501")

        assertFalse(currentState().canSave)
    }

    @Test
    fun onSaveClicked_persistsPartialEntryWithPendingSyncStatus() = runTest {
        set(MeasurementField.WAIST, "85")

        viewModel.onSaveClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.insert(
                match {
                    it.weight_kg == null &&
                        it.waist_cm == 85.0 &&
                        it.syncStatus == "PENDING_SYNC"
                }
            )
        }
    }

    @Test
    fun onSaveClicked_emitsSuccessAndResetsForm() = runTest {
        set(MeasurementField.WEIGHT, "78.5")

        viewModel.onSaveClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = currentState()
        assertTrue(state.justSaved)

        viewModel.onSavedHandled()
        assertFalse(currentState().justSaved)

        set(MeasurementField.WEIGHT, "")
        assertFalse(currentState().canSave)
    }

    @Test
    fun onSaveClicked_blockedWhenNothingEntered() = runTest {
        viewModel.onSaveClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun onSaveClicked_blockedWhenErrorsPresent() = runTest {
        set(MeasurementField.WEIGHT, "-5")

        viewModel.onSaveClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.insert(any()) }
        assertFalse(currentState().justSaved)
    }

    @Test
    fun onSaveClicked_ignoresBlankFieldsInPersistedEntry() = runTest {
        set(MeasurementField.CHEST, "")
        set(MeasurementField.WEIGHT, "80.25")
        set(MeasurementField.BICEP, "")

        viewModel.onSaveClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.insert(match { it.weight_kg == 80.25 && it.bicep_cm == null })
        }
    }

    @Test
    fun onTimestampChanged_updatesState() {
        val custom = 1_700_000_000_000L

        viewModel.onTimestampChanged(custom)

        assertEquals(custom, currentState().timestamp)
    }

    @Test
    fun onSaveClicked_blockedForFutureTimestamp() = runTest {
        set(MeasurementField.WAIST, "85")
        viewModel.onTimestampChanged(System.currentTimeMillis() + 60_000)

        viewModel.onSaveClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.insert(any()) }
        assertFalse(currentState().justSaved)
    }

    @Test
    fun futureTimestamp_setsAlertAndBlocksSave() {
        set(MeasurementField.WAIST, "85")
        viewModel.onTimestampChanged(System.currentTimeMillis() + 60_000)

        val state = currentState()
        assertEquals(
            BodyMeasurementViewModel.ERROR_FUTURE_DATE,
            state.timestampError
        )
        assertFalse(state.canSave)
    }

    @Test
    fun currentTimestamp_clearsFutureAlert() {
        viewModel.onTimestampChanged(System.currentTimeMillis() + 60_000)
        viewModel.onTimestampChanged(System.currentTimeMillis())

        val state = currentState()
        assertNull(state.timestampError)
    }

    @Test
    fun onSaveClicked_allowsCurrentTimestamp() = runTest {
        set(MeasurementField.WAIST, "85")
        viewModel.onTimestampChanged(System.currentTimeMillis())

        viewModel.onSaveClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.insert(any()) }
    }

    private fun mkEntry(id: String, waist: Double = 80.0) = BodyMeasurementEntry(
        entry_id = id,
        timestamp = System.currentTimeMillis(),
        lastModified = System.currentTimeMillis(),
        waist_cm = waist,
        isSynced = false,
        syncStatus = "PENDING_SYNC"
    )

    private fun seedEntries(vararg entries: BodyMeasurementEntry) {
        coEvery { repository.allEntries } returns kotlinx.coroutines.flow.flowOf(entries.toList())
        coEvery { repository.deleteEntry(any()) } returns Unit
        viewModel = BodyMeasurementViewModel(repository, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun undoDelete_defaultRestoresMostRecentlyDeletedLifo() = runTest {
        seedEntries(mkEntry("a", 70.0), mkEntry("b", 90.0))

        viewModel.deleteEntry("a")
        viewModel.deleteEntry("b")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.undoDelete()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repository.insert(match { it.entry_id == "b" }) }

        viewModel.undoDelete()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repository.insert(match { it.entry_id == "a" }) }
    }

    @Test
    fun undoDelete_byIdRestoresThatSpecificEntry() = runTest {
        seedEntries(mkEntry("a", 70.0), mkEntry("b", 90.0))

        viewModel.deleteEntry("a")
        viewModel.deleteEntry("b")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.undoDelete("a")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repository.insert(match { it.entry_id == "a" }) }
        coVerify(exactly = 0) { repository.insert(match { it.entry_id == "b" }) }
    }

    @Test
    fun deleteEntry_rapidSuccessiveDeletions_bothRemainRestorable() = runTest {
        seedEntries(mkEntry("a", 70.0), mkEntry("b", 90.0))

        viewModel.deleteEntry("a")
        viewModel.deleteEntry("b")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.undoDelete("b")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.undoDelete("a")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.insert(match { it.entry_id == "a" }) }
        coVerify(exactly = 1) { repository.insert(match { it.entry_id == "b" }) }
    }

    @Test
    fun undoDelete_unknownId_isSafeNoOp() = runTest {
        seedEntries(mkEntry("a"))

        viewModel.deleteEntry("a")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.undoDelete("does-not-exist")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.insert(any()) }
    }
}
