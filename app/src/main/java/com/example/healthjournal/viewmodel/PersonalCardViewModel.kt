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
    val demographics: Demographics = Demographics(),
    val medicalProfile: MedicalProfile = MedicalProfile(),
    val medicalHistory: MedicalHistory = MedicalHistory(),
    val emergencyContacts: EmergencyContacts = EmergencyContacts(),
    val draftDemographics: Demographics = Demographics(),
    val draftMedicalProfile: MedicalProfile = MedicalProfile(),
    val draftMedicalHistory: MedicalHistory = MedicalHistory(),
    val draftEmergencyContacts: EmergencyContacts = EmergencyContacts()
)

class PersonalCardViewModel(
    private val repository: PersonalCardRepository,
    private val bodyMeasurementRepository: BodyMeasurementRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalCardUiState())
    val uiState: StateFlow<PersonalCardUiState> = _uiState.asStateFlow()

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
    }

    fun onSexChanged(value: String) {
        _uiState.update { it.copy(draftDemographics = it.draftDemographics.copy(sex = value)) }
    }

    fun onHeightChanged(value: String) {
        val height = parseMeasurement(value, maxHeight = 300.0)
        _uiState.update { it.copy(draftDemographics = it.draftDemographics.copy(heightCm = height)) }
    }

    fun onWeightChanged(value: String) {
        val weight = parseMeasurement(value, maxHeight = 500.0)
        _uiState.update { it.copy(draftDemographics = it.draftDemographics.copy(weightKg = weight)) }
    }

    private fun parseMeasurement(value: String, maxHeight: Double): Double? {
        if (value.isEmpty()) return null
        val limited = value.take(7)
        val parts = limited.split(".")
        return when {
            parts.size > 2 -> null
            parts.size == 2 && parts[1].length > 2 -> null
            else -> {
                val num = limited.toDoubleOrNull()
                if (num != null && num in 0.0..maxHeight) num else null
            }
        }
    }

    fun onRaceEthnicityChanged(value: String) {
        _uiState.update { it.copy(draftDemographics = it.draftDemographics.copy(raceEthnicity = value)) }
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