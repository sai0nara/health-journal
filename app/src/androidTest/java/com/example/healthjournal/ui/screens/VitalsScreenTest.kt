package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.ui.theme.HealthJournalTheme
import com.example.healthjournal.util.FakeActiveProfileProvider
import com.example.healthjournal.viewmodel.VitalMetricTab
import com.example.healthjournal.viewmodel.VitalsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class VitalsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val journalRepository: JournalRepository = mockk(relaxed = true)
    private val activeProfileProvider = FakeActiveProfileProvider()
    private val vitalsFlow = MutableStateFlow<List<JournalEntry>>(emptyList())
    private val unitSystemFlow = MutableStateFlow(UnitSystem.METRIC)
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: VitalsViewModel

    @Before
    fun setup() {
        every { journalRepository.vitalsEntries } returns vitalsFlow
        every { journalRepository.currentProfileId() } returns SELF_PROFILE_ID
        coEvery { journalRepository.insert(any<String>(), any()) } returns Unit
        coEvery { journalRepository.delete(any<String>(), any<String>()) } returns Unit

        viewModel = VitalsViewModel(
            journalRepository = journalRepository,
            activeProfileProvider = activeProfileProvider,
            unitSystemFlow = unitSystemFlow,
            dispatcher = testDispatcher
        )
    }

    private fun setScreen() {
        composeTestRule.setContent {
            HealthJournalTheme {
                VitalsScreen(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
        }
    }

    @Test
    fun vitalsScreen_emptyState_showsTitleAndCta() {
        setScreen()

        composeTestRule.onNodeWithTag("vitals_empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithTag("vitals_empty_cta").assertIsDisplayed()
    }

    @Test
    fun vitalsScreen_tabSwitching_updatesSelectedMetric() {
        setScreen()

        composeTestRule.onNodeWithTag("vitals_tab_BLOOD_GLUCOSE").performClick()
        assertEquals(VitalMetricTab.BLOOD_GLUCOSE, viewModel.uiState.value.selectedMetric)
    }

    @Test
    fun vitalsScreen_fabOpensLogSheet_andSavesReading() {
        setScreen()

        composeTestRule.onNodeWithTag("vitals_fab_log").performClick()
        composeTestRule.onNodeWithTag("vital_log_sheet_title").assertIsDisplayed()

        // Input systolic & diastolic
        composeTestRule.onNodeWithTag("vitals_field_systolic").performTextInput("120")
        composeTestRule.onNodeWithTag("vitals_field_diastolic").performTextInput("80")

        composeTestRule.onNodeWithTag("vital_log_save").performClick()

        coVerify {
            journalRepository.insert(
                SELF_PROFILE_ID,
                match { it.bp_systolic == 120.0 && it.bp_diastolic == 80.0 }
            )
        }
    }

    @Test
    fun vitalsScreen_withEntries_rendersHistoryCardAndDeleteTriggersUndo() {
        val entry = JournalEntry(
            entry_id = "test-e1",
            description = "",
            timestamp = System.currentTimeMillis() - 3600000L,
            bp_systolic = 125.0,
            bp_diastolic = 85.0
        )
        vitalsFlow.value = listOf(entry)
        setScreen()

        composeTestRule.onNodeWithTag("vitals_delete_test-e1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("vitals_delete_test-e1").performClick()

        // Check Undo snackbar is visible
        composeTestRule.onNodeWithText("Undo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Undo").performClick()

        coVerify {
            journalRepository.insert(SELF_PROFILE_ID, entry)
        }
    }
}
