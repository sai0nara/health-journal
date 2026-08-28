package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.healthjournal.data.PersonalCardRepository
import com.example.healthjournal.data.local.BloodType
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
        val viewModel = PersonalCardViewModel(repository, ioDispatcher = testDispatcher)
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
            composeTestRule.onNodeWithText("180 cm").assertExists()
            composeTestRule.onNodeWithText("75 kg").assertExists()
        }
    }

    @Test
    fun cardWithMedicalProfile_displaysCorrectly() {
        step("Open Personal Card with medical profile") {
            setScreen(
                PersonalCard(
                    medicalProfile = MedicalProfile(
                        bloodType = BloodType.O_POSITIVE,
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

    @Test
    fun editMode_enterEditMode_showsInputFields() {
        step("Open Personal Card") {
            setScreen(PersonalCard())
        }

        step("Enter edit mode") {
            composeTestRule.onNodeWithContentDescription("Edit personal card").performClick()
        }

        step("Verify input fields are shown") {
            composeTestRule.onNodeWithText("Full Name").assertExists()
            composeTestRule.onNodeWithText("Date of Birth (YYYY-MM-DD)").assertExists()
            composeTestRule.onNodeWithText("Sex").assertExists()
            composeTestRule.onNodeWithText("Height (cm)").assertExists()
            composeTestRule.onNodeWithText("Weight (kg)").assertExists()
            composeTestRule.onNodeWithText("Blood Type").assertExists()
        }
    }

    @Test
    fun editMode_cancelEditing_returnsToViewMode() {
        step("Open Personal Card and enter edit mode") {
            setScreen(PersonalCard())
            composeTestRule.onNodeWithContentDescription("Edit personal card").performClick()
        }

        step("Click Cancel") {
            composeTestRule.onNodeWithText("Cancel").performClick()
        }

        step("Verify returned to view mode") {
            composeTestRule.onNodeWithContentDescription("Edit personal card").assertExists()
            composeTestRule.onNodeWithText("Save").assertDoesNotExist()
        }
    }

    @Test
    fun editMode_addAllergy_dialogAppears() {
        step("Open Personal Card and enter edit mode") {
            setScreen(PersonalCard())
            composeTestRule.onNodeWithContentDescription("Edit personal card").performClick()
        }

        step("Click add allergy button") {
            composeTestRule.onNodeWithContentDescription("Add allergy").performClick()
        }

        step("Verify allergy dialog appears") {
            composeTestRule.onNodeWithText("Add Allergy").assertExists()
            composeTestRule.onNodeWithText("Allergy").assertExists()
        }
    }

    @Test
    fun editMode_addMedication_dialogAppears() {
        step("Open Personal Card and enter edit mode") {
            setScreen(PersonalCard())
            composeTestRule.onNodeWithContentDescription("Edit personal card").performClick()
        }

        step("Click add medication button") {
            composeTestRule.onNodeWithContentDescription("Add medication").performClick()
        }

        step("Verify medication dialog appears") {
            composeTestRule.onNodeWithText("Add Medication").assertExists()
            composeTestRule.onNodeWithText("Drug Name").assertExists()
            composeTestRule.onNodeWithText("Dosage (e.g., 500mg)").assertExists()
        }
    }

    @Test
    fun editMode_addContact_dialogAppears() {
        step("Open Personal Card and enter edit mode") {
            setScreen(PersonalCard())
            composeTestRule.onNodeWithContentDescription("Edit personal card").performClick()
        }

        step("Click add emergency contact button") {
            composeTestRule.onNodeWithContentDescription("Add emergency contact").performClick()
        }

        step("Verify contact dialog appears") {
            composeTestRule.onNodeWithText("Add Emergency Contact").assertExists()
            composeTestRule.onNodeWithText("Name").assertExists()
            composeTestRule.onNodeWithText("Relationship").assertExists()
            composeTestRule.onNodeWithText("Phone Number").assertExists()
        }
    }

    @Test
    fun editMode_withExistingData_populatesFields() {
        step("Open Personal Card with existing data") {
            setScreen(
                PersonalCard(
                    demographics = Demographics(
                        fullName = "John Doe",
                        dateOfBirth = "1990-01-15",
                        sex = "Male"
                    ),
                    medicalProfile = MedicalProfile(
                        bloodType = BloodType.O_POSITIVE,
                        allergies = listOf("Peanuts")
                    )
                )
            )
        }

        step("Enter edit mode") {
            composeTestRule.onNodeWithContentDescription("Edit personal card").performClick()
        }

        step("Verify existing data is shown in fields") {
            composeTestRule.onNodeWithText("O+").assertExists()
            composeTestRule.onNodeWithText("Peanuts").assertExists()
        }
    }

    @Test
    fun editMode_showsUnitSystemToggle() {
        step("Open Personal Card screen with empty card") {
            setScreen(PersonalCard())
        }

        step("Enter edit mode") {
            composeTestRule.onNodeWithContentDescription("Edit personal card").performClick()
        }

        step("Verify Unit System toggle is shown") {
            composeTestRule.onNodeWithText("Unit System").assertExists()
        }
    }

    @Test
    fun editMode_showsDatePickerButton() {
        step("Open Personal Card screen with empty card") {
            setScreen(PersonalCard())
        }

        step("Enter edit mode") {
            composeTestRule.onNodeWithContentDescription("Edit personal card").performClick()
        }

        step("Verify Date Picker button is shown") {
            composeTestRule.onNodeWithContentDescription("Select date").assertExists()
        }
    }

    @Test
    fun editMode_disablesSaveWhenInvalid() {
        step("Open Personal Card with invalid date of birth") {
            setScreen(
                PersonalCard(
                    demographics = Demographics(dateOfBirth = "2030-01-01")
                )
            )
        }

        step("Enter edit mode") {
            composeTestRule.onNodeWithContentDescription("Edit personal card").performClick()
        }

        step("Verify Save button is disabled") {
            composeTestRule.onNodeWithText("Save").assertIsNotEnabled()
        }
    }

    @Test
    fun dialogs_useLocalizedCancelResource() {
        step("Open Personal Card and enter edit mode") {
            setScreen(PersonalCard())
            composeTestRule.onNodeWithContentDescription("Edit personal card").performClick()
        }

        step("Verify Add Allergy dialog has localized cancel button") {
            composeTestRule.onNodeWithContentDescription("Add allergy").performClick()
            composeTestRule.onNodeWithText("Add Allergy").assertExists()
            composeTestRule.onNodeWithText("Cancel").assertExists()
            composeTestRule.onNodeWithText("Cancel").performClick()
        }

        step("Verify Add Medication dialog has localized cancel button") {
            composeTestRule.onNodeWithContentDescription("Add medication").performClick()
            composeTestRule.onNodeWithText("Add Medication").assertExists()
            composeTestRule.onNodeWithText("Cancel").assertExists()
            composeTestRule.onNodeWithText("Cancel").performClick()
        }

        step("Verify Add Emergency Contact dialog has localized cancel button") {
            composeTestRule.onNodeWithContentDescription("Add emergency contact").performClick()
            composeTestRule.onNodeWithText("Add Emergency Contact").assertExists()
            composeTestRule.onNodeWithText("Cancel").assertExists()
        }
    }

    private fun step(description: String, block: () -> Unit) {
        io.qameta.allure.kotlin.Allure.step(description) { block() }
    }
}