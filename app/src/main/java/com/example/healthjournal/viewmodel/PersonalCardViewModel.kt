package com.example.healthjournal.viewmodel

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthjournal.data.PersonalCardRepository
import com.example.healthjournal.data.local.BloodType
import com.example.healthjournal.data.local.Demographics
import com.example.healthjournal.data.local.EmergencyContact
import com.example.healthjournal.data.local.EmergencyContacts
import com.example.healthjournal.data.local.MedicalHistory
import com.example.healthjournal.data.local.MedicalProfile
import com.example.healthjournal.data.local.MedicationEntry
import com.example.healthjournal.data.local.PersonalCard
import com.example.healthjournal.data.local.UnitConverter
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
    val draftDateOfBirthValue: TextFieldValue = TextFieldValue(""),
    val draftHeightText: String = "",
    val draftWeightText: String = "",
    val validation: DemographicsValidationResult = DemographicsValidationResult()
)

class PersonalCardViewModel(
    private val repository: PersonalCardRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalCardUiState())
    val uiState: StateFlow<PersonalCardUiState> = _uiState.asStateFlow()

    private val demographicsValidator = DemographicsValidator()

    init {
        viewModelScope.launch(ioDispatcher) {
            repository.getPersonalCard().collect { card ->
                if (card != null) {
                    _uiState.update {
                        it.copy(
                            demographics = card.demographics,
                            medicalProfile = card.medicalProfile,
                            medicalHistory = card.medicalHistory,
                            emergencyContacts = card.emergencyContacts,
                            draftDemographics = card.demographics,
                            draftMedicalProfile = card.medicalProfile,
                            draftMedicalHistory = card.medicalHistory,
                            draftEmergencyContacts = card.emergencyContacts,
                            draftDateOfBirthValue = TextFieldValue(
                                card.demographics.dateOfBirth,
                                TextRange(card.demographics.dateOfBirth.length)
                            ),
                            draftHeightText = UnitConverter.formatForDisplay(
                                card.demographics.heightCm, UnitSystem.METRIC, isHeight = true
                            ),
                            draftWeightText = UnitConverter.formatForDisplay(
                                card.demographics.weightKg, UnitSystem.METRIC, isHeight = false
                            ),
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
        _uiState.update { state ->
            state.copy(
                isEditing = true,
                draftDemographics = state.demographics,
                draftMedicalProfile = state.medicalProfile,
                draftMedicalHistory = state.medicalHistory,
                draftEmergencyContacts = state.emergencyContacts,
                draftDateOfBirthValue = TextFieldValue(
                    state.demographics.dateOfBirth,
                    TextRange(state.demographics.dateOfBirth.length)
                ),
                draftHeightText = UnitConverter.formatForDisplay(
                    state.demographics.heightCm, state.unitSystem, isHeight = true
                ),
                draftWeightText = UnitConverter.formatForDisplay(
                    state.demographics.weightKg, state.unitSystem, isHeight = false
                )
            )
        }
        validateDraft()
    }

    fun cancelEditing() {
        _uiState.update { state ->
            state.copy(
                isEditing = false,
                draftDemographics = state.demographics,
                draftMedicalProfile = state.medicalProfile,
                draftMedicalHistory = state.medicalHistory,
                draftEmergencyContacts = state.emergencyContacts,
                draftDateOfBirthValue = TextFieldValue(
                    state.demographics.dateOfBirth,
                    TextRange(state.demographics.dateOfBirth.length)
                ),
                draftHeightText = UnitConverter.formatForDisplay(
                    state.demographics.heightCm, state.unitSystem, isHeight = true
                ),
                draftWeightText = UnitConverter.formatForDisplay(
                    state.demographics.weightKg, state.unitSystem, isHeight = false
                )
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

    fun onDateOfBirthChanged(value: TextFieldValue) {
        val digits = value.text.filter { it.isDigit() }.take(8)
        val typedDigits = value.text
            .take(value.selection.end.coerceIn(0, value.text.length))
            .count { it.isDigit() }
            .coerceIn(0, digits.length)
        val formatted = when {
            digits.length <= 4 -> digits
            digits.length <= 6 -> "${digits.substring(0, 4)}-${digits.substring(4)}"
            else -> "${digits.substring(0, 4)}-${digits.substring(4, 6)}-${digits.substring(6)}"
        }
        val dashesBeforeCursor =
            (if (digits.length >= 5 && typedDigits > 4) 1 else 0) +
                (if (digits.length >= 7 && typedDigits > 6) 1 else 0)
        val cursorPosition = (typedDigits + dashesBeforeCursor).coerceIn(0, formatted.length)
        _uiState.update {
            it.copy(
                draftDateOfBirthValue = TextFieldValue(formatted, TextRange(cursorPosition)),
                draftDemographics = it.draftDemographics.copy(dateOfBirth = formatted)
            )
        }
        validateDraft()
    }

    fun onDateOfBirthSelected(dateString: String) {
        _uiState.update {
            it.copy(
                draftDateOfBirthValue = TextFieldValue(dateString, TextRange(dateString.length)),
                draftDemographics = it.draftDemographics.copy(dateOfBirth = dateString)
            )
        }
        validateDraft()
    }

    fun onSexChanged(value: String) {
        _uiState.update { it.copy(draftDemographics = it.draftDemographics.copy(sex = value)) }
    }

    fun onHeightChanged(value: String) {
        val state = _uiState.value
        val sanitized = UnitConverter.sanitizeDecimalInput(value)
        val heightCm = UnitConverter.parseInput(sanitized, state.unitSystem, isHeight = true)
        _uiState.update {
            it.copy(
                draftHeightText = sanitized,
                draftDemographics = it.draftDemographics.copy(heightCm = heightCm)
            )
        }
        validateDraft()
    }

    fun onWeightChanged(value: String) {
        val state = _uiState.value
        val sanitized = UnitConverter.sanitizeDecimalInput(value)
        val weightKg = UnitConverter.parseInput(sanitized, state.unitSystem, isHeight = false)
        _uiState.update {
            it.copy(
                draftWeightText = sanitized,
                draftDemographics = it.draftDemographics.copy(weightKg = weightKg)
            )
        }
        validateDraft()
    }

    fun onRaceEthnicityChanged(value: String) {
        _uiState.update { it.copy(draftDemographics = it.draftDemographics.copy(raceEthnicity = value)) }
    }

    fun onUnitSystemChanged(unitSystem: UnitSystem) {
        _uiState.update {
            it.copy(
                unitSystem = unitSystem,
                draftHeightText = UnitConverter.formatForDisplay(
                    it.draftDemographics.heightCm, unitSystem, isHeight = true
                ),
                draftWeightText = UnitConverter.formatForDisplay(
                    it.draftDemographics.weightKg, unitSystem, isHeight = false
                )
            )
        }
        validateDraft()
    }

    private fun validateDraft() {
        val state = _uiState.value
        val validation = demographicsValidator(state.draftDemographics, state.unitSystem)
        _uiState.update { it.copy(validation = validation) }
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
    private val repository: PersonalCardRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PersonalCardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PersonalCardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
