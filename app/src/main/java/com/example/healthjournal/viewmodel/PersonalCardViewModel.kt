package com.example.healthjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import com.example.healthjournal.domain.validation.DemographicsValidationResult
import com.example.healthjournal.domain.validation.DemographicsValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PersonalCardUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val demographics: Demographics = Demographics(),
    val medicalProfile: MedicalProfile = MedicalProfile(),
    val medicalHistory: MedicalHistory = MedicalHistory(),
    val emergencyContacts: EmergencyContacts = EmergencyContacts(),
    val draftDemographics: Demographics = Demographics(),
    val draftMedicalProfile: MedicalProfile = MedicalProfile(),
    val draftMedicalHistory: MedicalHistory = MedicalHistory(),
    val draftEmergencyContacts: EmergencyContacts = EmergencyContacts(),
    val validation: DemographicsValidationResult = DemographicsValidationResult()
)

class PersonalCardViewModel(
    private val repository: PersonalCardRepository,
    private val bodyMeasurementRepository: BodyMeasurementRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalCardUiState())
    val uiState: StateFlow<PersonalCardUiState> = _uiState.asStateFlow()

    private val demographicsValidator = DemographicsValidator()

    init {
        viewModelScope.launch(ioDispatcher) {
            repository.getPersonalCard().collect { card ->
                if (card != null) {
                    val latestWeight = bodyMeasurementRepository?.getLatestWeight()
                    val updatedDemographics = if (latestWeight != null) {
                        card.demographics.copy(weightKg = latestWeight)
                    } else {
                        card.demographics
                    }
                    _uiState.update {
                        it.copy(
                            demographics = updatedDemographics,
                            medicalProfile = card.medicalProfile,
                            medicalHistory = card.medicalHistory,
                            emergencyContacts = card.emergencyContacts,
                            draftDemographics = updatedDemographics,
                            draftMedicalProfile = card.medicalProfile,
                            draftMedicalHistory = card.medicalHistory,
                            draftEmergencyContacts = card.emergencyContacts,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun startEditing() {
        _uiState.update {
            it.copy(
                isEditing = true,
                draftDemographics = it.demographics,
                draftMedicalProfile = it.medicalProfile,
                draftMedicalHistory = it.medicalHistory,
                draftEmergencyContacts = it.emergencyContacts
            )
        }
    }

    fun cancelEditing() {
        _uiState.update {
            it.copy(
                isEditing = false,
                draftDemographics = it.demographics,
                draftMedicalProfile = it.medicalProfile,
                draftMedicalHistory = it.medicalHistory,
                draftEmergencyContacts = it.emergencyContacts
            )
        }
    }

    fun saveChanges() {
        val state = _uiState.value
        if (state.isSaving) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch(ioDispatcher) {
            val card = PersonalCard(
                id = PersonalCardRepository.PERSONAL_CARD_ID,
                lastModified = System.currentTimeMillis(),
                demographics = state.draftDemographics,
                medicalProfile = state.draftMedicalProfile,
                medicalHistory = state.draftMedicalHistory,
                emergencyContacts = state.draftEmergencyContacts
            )
            repository.insertOrUpdate(card)
            repository.markEntryDirty()
            _uiState.update {
                it.copy(
                    isEditing = false,
                    isSaving = false,
                    demographics = state.draftDemographics,
                    medicalProfile = state.draftMedicalProfile,
                    medicalHistory = state.draftMedicalHistory,
                    emergencyContacts = state.draftEmergencyContacts
                )
            }
        }
    }

    // Demographics updates
    fun onFullNameChanged(value: String) {
        _uiState.update { it.copy(draftDemographics = it.draftDemographics.copy(fullName = value)) }
    }

    fun onDateOfBirthChanged(value: String) {
        val digits = value.filter { it.isDigit() }.take(8)
        val formatted = when {
            digits.length <= 4 -> digits
            digits.length <= 6 -> "${digits.substring(0, 4)}-${digits.substring(4)}"
            else -> "${digits.substring(0, 4)}-${digits.substring(4, 6)}-${digits.substring(6)}"
        }
        _uiState.update { it.copy(draftDemographics = it.draftDemographics.copy(dateOfBirth = formatted)) }
        validateDraft()
    }

    fun onSexChanged(value: String) {
        _uiState.update { it.copy(draftDemographics = it.draftDemographics.copy(sex = value)) }
    }

    fun onHeightChanged(value: String) {
        val state = _uiState.value
        val heightCm = UnitConverter.parseInput(value, state.unitSystem, isHeight = true)
        _uiState.update { it.copy(draftDemographics = it.draftDemographics.copy(heightCm = heightCm)) }
        validateDraft()
    }

    fun onWeightChanged(value: String) {
        val state = _uiState.value
        val weightKg = UnitConverter.parseInput(value, state.unitSystem, isHeight = false)
        _uiState.update { it.copy(draftDemographics = it.draftDemographics.copy(weightKg = weightKg)) }
        validateDraft()
    }

    fun onRaceEthnicityChanged(value: String) {
        _uiState.update { it.copy(draftDemographics = it.draftDemographics.copy(raceEthnicity = value)) }
    }

    fun onUnitSystemChanged(unitSystem: UnitSystem) {
        _uiState.update { it.copy(unitSystem = unitSystem) }
        validateDraft()
    }

    private fun validateDraft() {
        val state = _uiState.value
        val validation = demographicsValidator(state.draftDemographics, state.unitSystem)
        _uiState.update { it.copy(validation = validation) }
    }

    companion object {
        fun formatDouble(value: Double?): String {
            if (value == null) return ""
            return if (value % 1.0 == 0.0) {
                value.toLong().toString()
            } else {
                value.toString()
            }
        }
    }

    // Medical Profile updates
    fun onBloodTypeChanged(value: BloodType?) {
        _uiState.update { it.copy(draftMedicalProfile = it.draftMedicalProfile.copy(bloodType = value)) }
    }

    fun addAllergy(allergy: String) {
        if (allergy.isBlank()) return
        _uiState.update {
            it.copy(draftMedicalProfile = it.draftMedicalProfile.copy(
                allergies = it.draftMedicalProfile.allergies + allergy
            ))
        }
    }

    fun removeAllergy(index: Int) {
        _uiState.update {
            it.copy(draftMedicalProfile = it.draftMedicalProfile.copy(
                allergies = it.draftMedicalProfile.allergies.toMutableList().apply { removeAt(index) }
            ))
        }
    }

    fun addMedication(medication: MedicationEntry) {
        if (medication.name.isBlank()) return
        _uiState.update {
            it.copy(draftMedicalProfile = it.draftMedicalProfile.copy(
                medications = it.draftMedicalProfile.medications + medication
            ))
        }
    }

    fun removeMedication(index: Int) {
        _uiState.update {
            it.copy(draftMedicalProfile = it.draftMedicalProfile.copy(
                medications = it.draftMedicalProfile.medications.toMutableList().apply { removeAt(index) }
            ))
        }
    }

    fun addAdverseReaction(reaction: String) {
        if (reaction.isBlank()) return
        _uiState.update {
            it.copy(draftMedicalProfile = it.draftMedicalProfile.copy(
                adverseReactions = it.draftMedicalProfile.adverseReactions + reaction
            ))
        }
    }

    fun removeAdverseReaction(index: Int) {
        _uiState.update {
            it.copy(draftMedicalProfile = it.draftMedicalProfile.copy(
                adverseReactions = it.draftMedicalProfile.adverseReactions.toMutableList().apply { removeAt(index) }
            ))
        }
    }

    // Medical History updates
    fun addHereditaryDisease(disease: String) {
        if (disease.isBlank()) return
        _uiState.update {
            it.copy(draftMedicalHistory = it.draftMedicalHistory.copy(
                hereditaryDiseases = it.draftMedicalHistory.hereditaryDiseases + disease
            ))
        }
    }

    fun removeHereditaryDisease(index: Int) {
        _uiState.update {
            it.copy(draftMedicalHistory = it.draftMedicalHistory.copy(
                hereditaryDiseases = it.draftMedicalHistory.hereditaryDiseases.toMutableList().apply { removeAt(index) }
            ))
        }
    }

    fun addChronicCondition(condition: String) {
        if (condition.isBlank()) return
        _uiState.update {
            it.copy(draftMedicalHistory = it.draftMedicalHistory.copy(
                chronicConditions = it.draftMedicalHistory.chronicConditions + condition
            ))
        }
    }

    fun removeChronicCondition(index: Int) {
        _uiState.update {
            it.copy(draftMedicalHistory = it.draftMedicalHistory.copy(
                chronicConditions = it.draftMedicalHistory.chronicConditions.toMutableList().apply { removeAt(index) }
            ))
        }
    }

    fun addSurgicalHistory(procedure: String) {
        if (procedure.isBlank()) return
        _uiState.update {
            it.copy(draftMedicalHistory = it.draftMedicalHistory.copy(
                surgicalHistory = it.draftMedicalHistory.surgicalHistory + procedure
            ))
        }
    }

    fun removeSurgicalHistory(index: Int) {
        _uiState.update {
            it.copy(draftMedicalHistory = it.draftMedicalHistory.copy(
                surgicalHistory = it.draftMedicalHistory.surgicalHistory.toMutableList().apply { removeAt(index) }
            ))
        }
    }

    // Emergency Contacts updates
    fun addEmergencyContact(contact: EmergencyContact) {
        if (contact.name.isBlank() || contact.phoneNumber.isBlank()) return
        _uiState.update {
            it.copy(draftEmergencyContacts = it.draftEmergencyContacts.copy(
                contacts = it.draftEmergencyContacts.contacts + contact
            ))
        }
    }

    fun removeEmergencyContact(index: Int) {
        _uiState.update {
            it.copy(draftEmergencyContacts = it.draftEmergencyContacts.copy(
                contacts = it.draftEmergencyContacts.contacts.toMutableList().apply { removeAt(index) }
            ))
        }
    }
}

class PersonalCardViewModelFactory(
    private val repository: PersonalCardRepository,
    private val bodyMeasurementRepository: BodyMeasurementRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PersonalCardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PersonalCardViewModel(repository, bodyMeasurementRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}