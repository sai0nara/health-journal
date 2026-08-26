package com.example.healthjournal.viewmodel

import com.example.healthjournal.data.PersonalCardRepository
import com.example.healthjournal.data.local.Demographics
import com.example.healthjournal.data.local.EmergencyContact
import com.example.healthjournal.data.local.EmergencyContacts
import com.example.healthjournal.data.local.MedicalHistory
import com.example.healthjournal.data.local.MedicalProfile
import com.example.healthjournal.data.local.MedicationEntry
import com.example.healthjournal.data.local.PersonalCard
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
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.getPersonalCard() } returns flowOf(null)
        coEvery { repository.insertOrUpdate(any()) } returns Unit
        coEvery { repository.markEntryDirty() } returns Unit
        viewModel = PersonalCardViewModel(repository, testDispatcher)
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
                medicalProfile = MedicalProfile(bloodType = "O+")
            )
        )

        viewModel.startEditing()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = currentState()
        assertTrue(state.isEditing)
        assertEquals("John Doe", state.draftDemographics.fullName)
        assertEquals("O+", state.draftMedicalProfile.bloodType)
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
        viewModel.onBloodTypeChanged("A+")
        viewModel.saveChanges()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = currentState()
        assertFalse(state.isEditing)
        assertEquals("John Doe", state.demographics.fullName)
        assertEquals("A+", state.medicalProfile.bloodType)
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
        viewModel.onBloodTypeChanged("B-")

        assertEquals("B-", currentState().draftMedicalProfile.bloodType)
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

    private fun assertNull(value: Any?) {
        org.junit.Assert.assertNull(value)
    }

    private fun seedCard(card: PersonalCard) {
        coEvery { repository.getPersonalCard() } returns flowOf(card)
        viewModel = PersonalCardViewModel(repository, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()
    }
}