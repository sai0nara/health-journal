package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthjournal.data.PresetRepository
import com.example.healthjournal.data.local.ExerciseCatalogItem
import com.example.healthjournal.data.local.UnitConverter
import com.example.healthjournal.data.local.UnitSettings
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.domain.ScheduledDay
import com.example.healthjournal.ui.theme.HealthJournalTheme
import com.example.healthjournal.util.FakeExerciseCatalogDao
import com.example.healthjournal.util.FakeWorkoutPresetDao
import com.example.healthjournal.viewmodel.PresetUiState
import com.example.healthjournal.viewmodel.PresetViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PresetLibraryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var presetDao: FakeWorkoutPresetDao
    private lateinit var catalogDao: FakeExerciseCatalogDao
    private lateinit var repository: PresetRepository
    private lateinit var viewModel: PresetViewModel

    @Before
    fun setup() {
        presetDao = FakeWorkoutPresetDao()
        catalogDao = FakeExerciseCatalogDao()
        repository = PresetRepository(presetDao)
        viewModel = PresetViewModel(repository, catalogDao)
        runBlocking {
            catalogDao.insertAll(
                listOf(
                    ExerciseCatalogItem(
                        id = "bench-press",
                        name = "Bench Press",
                        muscleCategory = "Chest"
                    ),
                    ExerciseCatalogItem(
                        id = "barbell-squat",
                        name = "Barbell Squat",
                        muscleCategory = "Quads"
                    )
                )
            )
        }
    }

    private fun openScreen() {
        composeTestRule.setContent {
            HealthJournalTheme {
                PresetLibraryScreen(viewModel = viewModel, onBack = {})
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun emptyLibrary_showsEmptyStateAndCreateButton() {
        openScreen()

        composeTestRule.onNodeWithTag("preset_library_list").assertExists()
        composeTestRule.onNodeWithText("No presets yet").assertExists()
        composeTestRule.onNodeWithText("Create Preset", useUnmergedTree = true).assertExists()
    }

    @Test
    fun libraryWithPresets_listsThem() {
        runBlocking {
            repository.savePreset(WorkoutPreset(id = "p1", name = "Leg Day"))
        }
        openScreen()

        composeTestRule.onNodeWithText("Leg Day").assertExists()
    }

    @Test
    fun createButton_opensBlankCreateForm() {
        openScreen()

        composeTestRule.onNodeWithText("Create Preset", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("preset_name_field").assertExists()
        composeTestRule.onNodeWithText("Save Preset").assertExists()
    }

    @Test
    fun createForm_withBlankName_showsInlineError_andStaysOnForm() {
        openScreen()
        composeTestRule.onNodeWithText("Create Preset", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Save Preset").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Preset name is required").assertExists()
        composeTestRule.onNodeWithTag("preset_name_field").assertExists()
    }

    @Test
    fun createForm_withoutExercises_showsExerciseError() {
        openScreen()
        composeTestRule.onNodeWithText("Create Preset", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("preset_name_field").performTextInput("Leg Day")
        composeTestRule.onNodeWithText("Save Preset").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Add at least one exercise").assertExists()
    }

    @Test
    fun exerciseSearch_filtersCatalog_SelectingAddsToDraft() {
        openScreen()
        composeTestRule.onNodeWithText("Create Preset", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("exercise_search_field").performTextInput("Bench")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Bench Press").assertExists()

        composeTestRule.onNodeWithText("Bench Press").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("preset_draft_list").performScrollToNode(hasText("Bench Press"))
        composeTestRule.onNodeWithText("Bench Press").assertIsDisplayed()
    }

    @Test
    fun selectingExercise_prefillsSensibleDefaultsPerMovement() {
        openScreen()
        composeTestRule.onNodeWithText("Create Preset", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("exercise_search_field").performTextInput("Squat")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("exercise_search_result").performClick()
        composeTestRule.waitForIdle()

        assertEquals(3, selectedDraft().single().targetSets)
        assertEquals(5, selectedDraft().single().defaultReps)
        assertEquals(40.0, selectedDraft().single().defaultWeightKg, 0.0)
        assertEquals(120, selectedDraft().single().restSeconds)
    }

    @Test
    fun presetFields_canBeClearedAndRetyped() {
        openScreen()
        composeTestRule.onNodeWithText("Create Preset", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("exercise_search_field").performTextInput("Squat")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("exercise_search_result").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("preset_field_sets").performTextClearance()
        composeTestRule.onNodeWithTag("preset_field_sets").performTextInput("4")
        composeTestRule.onNodeWithTag("preset_field_weight").performTextClearance()
        composeTestRule.onNodeWithTag("preset_field_weight").performTextInput("60")
        composeTestRule.waitForIdle()

        assertEquals(4, selectedDraft().single().targetSets)
        assertEquals(60.0, selectedDraft().single().defaultWeightKg, 0.0)
    }

    /** The exercise draft currently being edited in the form. */
    private fun selectedDraft(): List<com.example.healthjournal.domain.PresetExercise> =
        (viewModel.uiState.value as PresetUiState.Editing).exercises

    @Test
    fun presetFields_nonNumericWeight_isIgnoredWithoutCrash() {
        openScreen()
        composeTestRule.onNodeWithText("Create Preset", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("exercise_search_field").performTextInput("Squat")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("exercise_search_result").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("preset_field_weight").performTextClearance()
        composeTestRule.onNodeWithTag("preset_field_weight").performTextInput("abc")
        composeTestRule.waitForIdle()

        // Non-numeric input is silently dropped at the field (toDoubleOrNull
        // fails), leaving the last valid weight in the draft — no crash.
        assertEquals(40.0, selectedDraft().single().defaultWeightKg, 0.0)
    }

    @Test
    fun create_validPreset_savesAndBacksToList() {
        openScreen()
        composeTestRule.onNodeWithText("Create Preset", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("preset_name_field").performTextInput("Leg Day")
        composeTestRule.onNodeWithTag("exercise_search_field").performTextInput("Squat")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("exercise_search_result").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Save Preset").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("preset_library_list").assertExists()
        composeTestRule.onNodeWithText("Leg Day").assertExists()
    }

    @Test
    fun tappingPreset_opensItInTheEditForm() {
        runBlocking {
            repository.savePreset(
                WorkoutPreset(
                    id = "p1",
                    name = "Leg Day",
                    scheduledDay = ScheduledDay.MONDAY.name
                )
            )
        }
        openScreen()

        composeTestRule.onNodeWithText("Leg Day").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("preset_name_field").assertExists()
        composeTestRule.onNodeWithText("Save Preset").assertExists()
    }

    @Test
    fun deletePromptsAndRemovesPresetFromList() {
        runBlocking {
            repository.savePreset(WorkoutPreset(id = "p1", name = "Leg Day"))
        }
        openScreen()

        composeTestRule.onNodeWithTag("preset_delete_p1").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Delete Preset?").assertExists()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("preset_library_list").assertExists()
        composeTestRule.onNodeWithText("Leg Day").assertDoesNotExist()
    }

    @Test
    fun exerciseSearch_caseInsensitiveQuery_findsMatch() {
        openScreen()
        composeTestRule.onNodeWithText("Create Preset", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("exercise_search_field").performTextInput("bEnCh")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Bench Press").assertExists()
    }

    @Test
    fun exerciseSearch_noMatch_showsEmptyResultsWithoutCrash() {
        openScreen()
        composeTestRule.onNodeWithText("Create Preset", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("exercise_search_field").performTextInput("zzz")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("exercise_search_result").assertDoesNotExist()
        composeTestRule.onNodeWithText("Bench Press").assertDoesNotExist()
        composeTestRule.onNodeWithTag("preset_name_field").assertExists()
    }

    @Test
    fun exerciseSearch_blankQuery_showsNoResults() {
        openScreen()
        composeTestRule.onNodeWithText("Create Preset", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("exercise_search_field").performTextInput("Be")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("exercise_search_field").performTextClearance()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("exercise_search_result").assertDoesNotExist()
    }

    @Test
    fun exerciseSearch_sqlInjectionQuery_neverLeaksOrCrashes() {
        openScreen()
        composeTestRule.onNodeWithText("Create Preset", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("exercise_search_field").performTextInput("' OR '1'='1")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("exercise_search_result").assertDoesNotExist()
        composeTestRule.onNodeWithText("Bench Press").assertDoesNotExist()
        composeTestRule.onNodeWithText("Barbell Squat").assertDoesNotExist()
        composeTestRule.onNodeWithTag("preset_name_field").assertExists()
    }

    @Test
    fun exerciseSearch_unicodeQuery_isSafe() {
        openScreen()
        composeTestRule.onNodeWithText("Create Preset", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("exercise_search_field").performTextInput("💪belle")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("exercise_search_result").assertDoesNotExist()
        composeTestRule.onNodeWithTag("preset_name_field").assertExists()
    }

    @Test
    fun presetEditor_imperialWeightShowsLbAndSavesKg() {
        UnitSettings.write(
            InstrumentationRegistry.getInstrumentation().targetContext,
            UnitSystem.IMPERIAL
        )
        try {
            openScreen()
            composeTestRule.onNodeWithText("Create Preset", useUnmergedTree = true).performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("exercise_search_field").performTextInput("Squat")
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("exercise_search_result").performClick()
            composeTestRule.waitForIdle()

            // The 40 kg squat default renders as 88.2 lb with an lb label.
            composeTestRule.onNodeWithTag("preset_field_weight").assertTextContains("88.2", substring = true)
            composeTestRule.onNodeWithText("Weight lb").assertExists()

            // Typing lb persists canonical kg.
            composeTestRule.onNodeWithTag("preset_name_field").performTextInput("Leg Day")
            composeTestRule.onNodeWithTag("preset_field_weight").performTextClearance()
            composeTestRule.onNodeWithTag("preset_field_weight").performTextInput("90")
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Save Preset").performClick()
            composeTestRule.waitForIdle()

            val saved = runBlocking { repository.presets.first() }.single()
            assertEquals(
                UnitConverter.lbsToKg(90.0),
                saved.exercises.single().defaultWeightKg,
                0.0
            )
        } finally {
            UnitSettings.write(
                InstrumentationRegistry.getInstrumentation().targetContext,
                UnitSystem.METRIC
            )
        }
    }
}