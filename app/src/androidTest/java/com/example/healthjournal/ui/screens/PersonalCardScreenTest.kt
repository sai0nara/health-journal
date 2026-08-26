package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.healthjournal.data.PersonalCardRepository
import com.example.healthjournal.data.local.Demographics
import com.example.healthjournal.data.local.EmergencyContact
import com.example.healthjournal.data.local.EmergencyContacts
import com.example.healthjournal.data.local.MedicalHistory
import com.example.healthjournal.data.local.MedicalProfile
import com.example.healthjournal.data.local.MedicationEntry
import com.example.healthjournal.data.local.PersonalCard
import com.example.healthjournal.viewmodel.PersonalCardViewModel
import io.mockk.*
import io.qameta.allure.android.rules.ScreenshotRule
import io.qameta.allure.kotlin.Feature
import io.qameta.allure.kotlin.Step
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@Feature("Personal Card Screen")
class PersonalCardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = ScreenshotRule(mode = ScreenshotRule.Mode.FAILURE)

    private val repository: PersonalCardRepository = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    private fun setScreen(card: PersonalCard? = null) {
        every { repository.getPersonalCard() } returns MutableStateFlow(card)
        coEvery { repository.insertOrUpdate(any()) } returns Unit
        coEvery { repository.markEntryDirty() } returns Unit
        val viewModel = PersonalCardViewModel(repository, testDispatcher)
        composeTestRule.setContent {
            com.example.healthjournal.ui.theme.HealthJournalTheme {
                PersonalCardScreen(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun emptyCard_showsEmptyStates() {
        step("Open Personal Card screen with empty card") {
            setScreen(PersonalCard())
        }

        step("Verify empty state messages") {
            composeTestRule.onNodeWithText("No demographics information added yet.").assertExists()
            composeTestRule.onNodeWithText("No medical profile information added yet.").assertExists()
            composeTestRule.onNodeWithText("No medical history information added yet.").assertExists()
            composeTestRule.onNodeWithText("No emergency contacts added yet.").assertExists()
        }
    }

    @Test
    fun cardWithDemographics_displaysCorrectly() {
        step("Open Personal Card with demographics") {
            setScreen(
                PersonalCard(
                    demographics = Demographics(
                        fullName = "John Doe",
                        dateOfBirth = "1990-01-15",
                        sex = "Male",
                        heightCm = 180.0,
                        weightKg = 75.0
                    )
                )
            )
        }

        step("Verify demographics are displayed") {
            composeTestRule.onNodeWithText("John Doe").assertExists()
            composeTestRule.onNodeWithText("1990-01-15").assertExists()
            composeTestRule.onNodeWithText("Male").assertExists()
            composeTestRule.onNodeWithText("180.0 cm").assertExists()
            composeTestRule.onNodeWithText("75.0 kg").assertExists()
        }
    }

    @Test
    fun cardWithMedicalProfile_displaysCorrectly() {
        step("Open Personal Card with medical profile") {
            setScreen(
                PersonalCard(
                    medicalProfile = MedicalProfile(
                        bloodType = "O+",
                        allergies = listOf("Penicillin", "Peanuts"),
                        medications = listOf(
                            MedicationEntry(name = "Aspirin", dosage = "81mg", schedule = "Daily")
                        )
                    )
                )
            )
        }

        step("Verify medical profile is displayed") {
            composeTestRule.onNodeWithText("O+").assertExists()
            composeTestRule.onNodeWithText("Penicillin").assertExists()
            composeTestRule.onNodeWithText("Peanuts").assertExists()
            composeTestRule.onNodeWithText("Aspirin 81mg - Daily").assertExists()
        }
    }

    @Test
    fun cardWithEmergencyContacts_displaysCorrectly() {
        step("Open Personal Card with emergency contacts") {
            setScreen(
                PersonalCard(
                    emergencyContacts = EmergencyContacts(
                        contacts = listOf(
                            EmergencyContact(name = "Jane Doe", relationship = "Spouse", phoneNumber = "555-0123")
                        )
                    )
                )
            )
        }

        step("Verify emergency contact is displayed") {
            composeTestRule.onNodeWithText("Jane Doe").assertExists()
            composeTestRule.onNodeWithText("Spouse").assertExists()
            composeTestRule.onNodeWithText("555-0123").assertExists()
        }
    }

    @Test
    fun editButton_navigatesToEditMode() {
        step("Open Personal Card") {
            setScreen(PersonalCard())
        }

        step("Click edit button") {
            composeTestRule.onNodeWithContentDescription("Edit personal card").performClick()
        }

        step("Verify edit mode is activated") {
            composeTestRule.onNodeWithText("Save").assertExists()
            composeTestRule.onNodeWithText("Cancel").assertExists()
        }
    }

    private fun step(description: String, block: () -> Unit) {
        io.qameta.allure.kotlin.Allure.step(description) { block() }
    }
}