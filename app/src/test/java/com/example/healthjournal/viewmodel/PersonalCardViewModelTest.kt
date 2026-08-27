package com.example.healthjournal.viewmodel

import com.example.healthjournal.data.BodyMeasurementRepository
import com.example.healthjournal.data.PersonalCardRepository
import com.example.healthjournal.data.local.BloodType
import com.example.healthjournal.data.local.Demographics
import com.example.healthjournal.data.local.EmergencyContact
import com.example.healthjournal.data.local.EmergencyContacts
import com.example.healthjournal.data.local.MedicalHistory
import com.example.healthjournal.data.local.MedicalProfile
import com.example.healthjournal.data.local.MedicationEntry
import com.example.healthjournal.data.local.PersonalCard
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.domain.validation.ValidationResult
import com.example.healthjournal.domain.validation.DemographicsValidationResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalCardViewModelTest {

    private lateinit var viewModel: PersonalCardViewModel
    private val repository: PersonalCardRepository = mockk()
    private val bodyMeasurementRepository: BodyMeasurementRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.getPersonalCard() } returns flowOf(null)
        coEvery { repository.insertOrUpdate(any()) } returns Unit
        coEvery { repository.markEntryDirty() } returns Unit
        coEvery { bodyMeasurementRepository.getLatestWeight() } returns null
        viewModel = PersonalCardViewModel(repository, bodyMeasurementRepository, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun currentState() = viewModel.uiState.value

    @Test
    fun initialState_hasDefaults() {
        val state = currentState()

        assertFalse(state.isEditing)
        assertTrue(state.isLoading)
        assertFalse(state.isSaving)
        assertEquals(Demographics(), state.demographics)
        assertEquals(MedicalProfile(), state.medicalProfile)
    }

    @Test
    fun startEditing_copiesCurrentToDraft() = runTest {
        seedCard(
            PersonalCard(
                demographics = Demographics(fullName = "John Doe"),
                medicalProfile = MedicalProfile(bloodType = BloodType.O_POSITIVE)
            )
        )

        viewModel.startEditing()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = currentState()
        assertTrue(state.isEditing)
        assertEquals("John Doe", state.draftDemographics.fullName)
        assertEquals(BloodType.O_POSITIVE, state.draftMedicalProfile.bloodType)
    }

    @Test
    fun cancelEditing_revertsDraftToOriginal() = runTest {
        seedCard(
            PersonalCard(
                demographics = Demographics(fullName = "John Doe")
            )
        )

        viewModel.startEditing()
        viewModel.onFullNameChanged("Jane Doe")
        viewModel.cancelEditing()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = currentState()
        assertFalse(state.isEditing)
        assertEquals("John Doe", state.demographics.fullName)
        assertEquals("John Doe", state.draftDemographics.fullName)
    }

    @Test
    fun saveChanges_persistsAndExitsEditMode() = runTest {
        seedCard(PersonalCard())

        viewModel.startEditing()
        viewModel.onFullNameChanged("John Doe")
        viewModel.onBloodTypeChanged(BloodType.A_POSITIVE)
        viewModel.saveChanges()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = currentState()
        assertFalse(state.isEditing)
        assertEquals("John Doe", state.demographics.fullName)
        assertEquals(BloodType.A_POSITIVE, state.medicalProfile.bloodType)
        coVerify { repository.insertOrUpdate(any()) }
        coVerify { repository.markEntryDirty() }
    }

    @Test
    fun onFullNameChanged_updatesDraftDemographics() {
        viewModel.startEditing()
        viewModel.onFullNameChanged("Test User")

        assertEquals("Test User", currentState().draftDemographics.fullName)
    }

    @Test
    fun onBloodTypeChanged_updatesDraftMedicalProfile() {
        viewModel.startEditing()
        viewModel.onBloodTypeChanged(BloodType.B_NEGATIVE)

        assertEquals(BloodType.B_NEGATIVE, currentState().draftMedicalProfile.bloodType)
    }

    @Test
    fun addAllergy_addsToList() {
        viewModel.startEditing()
        viewModel.addAllergy("Penicillin")
        viewModel.addAllergy("Peanuts")

        assertEquals(2, currentState().draftMedicalProfile.allergies.size)
        assertEquals("Penicillin", currentState().draftMedicalProfile.allergies[0])
    }

    @Test
    fun addAllergy_ignoresBlank() {
        viewModel.startEditing()
        viewModel.addAllergy("")

        assertTrue(currentState().draftMedicalProfile.allergies.isEmpty())
    }

    @Test
    fun removeAllergy_removesFromList() {
        viewModel.startEditing()
        viewModel.addAllergy("Penicillin")
        viewModel.addAllergy("Peanuts")
        viewModel.removeAllergy(0)

        assertEquals(1, currentState().draftMedicalProfile.allergies.size)
        assertEquals("Peanuts", currentState().draftMedicalProfile.allergies[0])
    }

    @Test
    fun addMedication_addsToList() {
        viewModel.startEditing()
        viewModel.addMedication(MedicationEntry(name = "Aspirin", dosage = "81mg", schedule = "Daily"))

        assertEquals(1, currentState().draftMedicalProfile.medications.size)
        assertEquals("Aspirin", currentState().draftMedicalProfile.medications[0].name)
    }

    @Test
    fun addMedication_ignoresBlankName() {
        viewModel.startEditing()
        viewModel.addMedication(MedicationEntry(name = "", dosage = "81mg", schedule = "Daily"))

        assertTrue(currentState().draftMedicalProfile.medications.isEmpty())
    }

    @Test
    fun removeMedication_removesFromList() {
        viewModel.startEditing()
        viewModel.addMedication(MedicationEntry(name = "Aspirin", dosage = "81mg", schedule = "Daily"))
        viewModel.removeMedication(0)

        assertTrue(currentState().draftMedicalProfile.medications.isEmpty())
    }

    @Test
    fun addChronicCondition_addsToList() {
        viewModel.startEditing()
        viewModel.addChronicCondition("Hypertension")

        assertEquals(1, currentState().draftMedicalHistory.chronicConditions.size)
    }

    @Test
    fun removeChronicCondition_removesFromList() {
        viewModel.startEditing()
        viewModel.addChronicCondition("Hypertension")
        viewModel.removeChronicCondition(0)

        assertTrue(currentState().draftMedicalHistory.chronicConditions.isEmpty())
    }

    @Test
    fun addEmergencyContact_addsToList() {
        viewModel.startEditing()
        viewModel.addEmergencyContact(
            EmergencyContact(name = "Jane", relationship = "Spouse", phoneNumber = "555-0123")
        )

        assertEquals(1, currentState().draftEmergencyContacts.contacts.size)
        assertEquals("Jane", currentState().draftEmergencyContacts.contacts[0].name)
    }

    @Test
    fun addEmergencyContact_ignoresBlankFields() {
        viewModel.startEditing()
        viewModel.addEmergencyContact(
            EmergencyContact(name = "", relationship = "Spouse", phoneNumber = "555-0123")
        )

        assertTrue(currentState().draftEmergencyContacts.contacts.isEmpty())
    }

    @Test
    fun removeEmergencyContact_removesFromList() {
        viewModel.startEditing()
        viewModel.addEmergencyContact(
            EmergencyContact(name = "Jane", relationship = "Spouse", phoneNumber = "555-0123")
        )
        viewModel.removeEmergencyContact(0)

        assertTrue(currentState().draftEmergencyContacts.contacts.isEmpty())
    }

    @Test
    fun onHeightChanged_parsesDoubleOrNull() {
        viewModel.startEditing()
        viewModel.onHeightChanged("180.5")

        assertEquals(180.5, currentState().draftDemographics.heightCm!!, 0.001)

        viewModel.onHeightChanged("abc")
        assertNull(currentState().draftDemographics.heightCm)
    }

    @Test
    fun onHeightChanged_updatesDraftDemographics() {
        viewModel.startEditing()
        viewModel.onHeightChanged("180.5")

        assertEquals(180.5, currentState().draftDemographics.heightCm!!, 0.001)
    }

    @Test
    fun onHeightChanged_validatesOutOfRange() {
        viewModel.startEditing()
        viewModel.onHeightChanged("350")

        assertTrue(currentState().validation.height is ValidationResult.Invalid)
    }

    @Test
    fun onHeightChanged_acceptsValidHeight() {
        viewModel.startEditing()
        viewModel.onHeightChanged("178.25")

        assertEquals(178.25, currentState().draftDemographics.heightCm!!, 0.001)
        assertTrue(currentState().validation.height is ValidationResult.Valid)
    }

    @Test
    fun onWeightChanged_validatesOutOfRange() {
        viewModel.startEditing()
        viewModel.onWeightChanged("700")

        assertTrue(currentState().validation.weight is ValidationResult.Invalid)
    }

    @Test
    fun onWeightChanged_acceptsValidWeight() {
        viewModel.startEditing()
        viewModel.onWeightChanged("75.55")

        assertEquals(75.55, currentState().draftDemographics.weightKg!!, 0.001)
        assertTrue(currentState().validation.weight is ValidationResult.Valid)
    }

    @Test
    fun onDateOfBirthChanged_formatsWithDashes() {
        viewModel.startEditing()
        viewModel.onDateOfBirthChanged("19900115")
        assertEquals("1990-01-15", currentState().draftDemographics.dateOfBirth)
    }

    @Test
    fun onDateOfBirthChanged_rejectsNonDigits() {
        viewModel.startEditing()
        viewModel.onDateOfBirthChanged("1990-01-15abc")
        assertEquals("1990-01-15", currentState().draftDemographics.dateOfBirth)
    }

    @Test
    fun formatDouble_removesTrailingZero() {
        assertEquals("178", PersonalCardViewModel.formatDouble(178.0))
        assertEquals("178.5", PersonalCardViewModel.formatDouble(178.5))
        assertEquals("", PersonalCardViewModel.formatDouble(null))
        assertEquals("0", PersonalCardViewModel.formatDouble(0.0))
    }

    @Test
    fun validation_failsWithInvalidDate() {
        viewModel.startEditing()
        viewModel.onDateOfBirthChanged("20300101")

        val state = currentState()
        assertFalse(state.validation.isValid)
        assertTrue(state.validation.dateOfBirth is ValidationResult.Invalid)
    }

    @Test
    fun validation_failsWithOutOfRangeHeight() {
        viewModel.startEditing()
        viewModel.onHeightChanged("300")

        val state = currentState()
        assertFalse(state.validation.isValid)
        assertTrue(state.validation.height is ValidationResult.Invalid)
    }

    @Test
    fun unitSystemChange_revalidatesHeightInNewUnits() {
        viewModel.startEditing()
        viewModel.onHeightChanged("20")
        assertTrue(currentState().validation.height is ValidationResult.Valid)

        viewModel.onUnitSystemChanged(UnitSystem.IMPERIAL)
        assertEquals(UnitSystem.IMPERIAL, currentState().unitSystem)
        assertTrue(currentState().validation.height is ValidationResult.Invalid)
    }

    @Test
    fun initialState_hasValidValidation() {
        val state = currentState()
        assertTrue(state.validation.isValid)
    }

    @Test
    fun onUnitSystemChanged_updatesUnitSystem() {
        viewModel.startEditing()
        viewModel.onUnitSystemChanged(UnitSystem.IMPERIAL)

        assertEquals(UnitSystem.IMPERIAL, currentState().unitSystem)
        assertTrue(currentState().validation.height is ValidationResult.Valid)
    }

    private fun assertNull(value: Any?) {
        org.junit.Assert.assertNull(value)
    }

    private fun seedCard(card: PersonalCard) {
        coEvery { repository.getPersonalCard() } returns flowOf(card)
        viewModel = PersonalCardViewModel(repository, bodyMeasurementRepository, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()
    }
}