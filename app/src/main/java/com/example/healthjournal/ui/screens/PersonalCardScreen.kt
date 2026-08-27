package com.example.healthjournal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.example.healthjournal.R
import com.example.healthjournal.data.local.BloodType
import com.example.healthjournal.data.local.Demographics
import com.example.healthjournal.data.local.EmergencyContact
import com.example.healthjournal.data.local.EmergencyContacts
import com.example.healthjournal.data.local.MedicalHistory
import com.example.healthjournal.data.local.MedicalProfile
import com.example.healthjournal.data.local.MedicationEntry
import com.example.healthjournal.data.local.UnitConverter
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.domain.validation.DemographicsValidationResult
import com.example.healthjournal.domain.validation.ValidationResult
import com.example.healthjournal.viewmodel.PersonalCardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalCardScreen(
    viewModel: PersonalCardViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.personal_card_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isEditing) {
                            viewModel.cancelEditing()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (uiState.isEditing) {
                        TextButton(onClick = { viewModel.cancelEditing() }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        TextButton(
                            onClick = { viewModel.saveChanges() },
                            enabled = uiState.validation.isValid
                        ) {
                            Text(stringResource(R.string.action_save))
                        }
                    } else {
                        IconButton(onClick = { viewModel.startEditing() }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.loading),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .testTag("personal_card_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    if (uiState.isEditing) {
                        DemographicsEditCard(
                            demographics = uiState.draftDemographics,
                            dateOfBirthValue = uiState.draftDateOfBirthValue,
                            heightText = uiState.draftHeightText,
                            weightText = uiState.draftWeightText,
                            validation = uiState.validation,
                            unitSystem = uiState.unitSystem,
                            onFullNameChanged = viewModel::onFullNameChanged,
                            onDateOfBirthChanged = viewModel::onDateOfBirthChanged,
                            onDateOfBirthSelected = viewModel::onDateOfBirthSelected,
                            onSexChanged = viewModel::onSexChanged,
                            onHeightChanged = viewModel::onHeightChanged,
                            onWeightChanged = viewModel::onWeightChanged,
                            onRaceEthnicityChanged = viewModel::onRaceEthnicityChanged,
                            onUnitSystemChanged = viewModel::onUnitSystemChanged
                        )
                    } else {
                        DemographicsCard(demographics = uiState.demographics)
                    }
                }
                item {
                    if (uiState.isEditing) {
                        MedicalProfileEditCard(
                            medicalProfile = uiState.draftMedicalProfile,
                            onBloodTypeChanged = viewModel::onBloodTypeChanged,
                            onAddAllergy = viewModel::addAllergy,
                            onRemoveAllergy = viewModel::removeAllergy,
                            onAddMedication = viewModel::addMedication,
                            onRemoveMedication = viewModel::removeMedication,
                            onAddAdverseReaction = viewModel::addAdverseReaction,
                            onRemoveAdverseReaction = viewModel::removeAdverseReaction
                        )
                    } else {
                        MedicalProfileCard(medicalProfile = uiState.medicalProfile)
                    }
                }
                item {
                    if (uiState.isEditing) {
                        MedicalHistoryEditCard(
                            medicalHistory = uiState.draftMedicalHistory,
                            onAddHereditaryDisease = viewModel::addHereditaryDisease,
                            onRemoveHereditaryDisease = viewModel::removeHereditaryDisease,
                            onAddChronicCondition = viewModel::addChronicCondition,
                            onRemoveChronicCondition = viewModel::removeChronicCondition,
                            onAddSurgicalHistory = viewModel::addSurgicalHistory,
                            onRemoveSurgicalHistory = viewModel::removeSurgicalHistory
                        )
                    } else {
                        MedicalHistoryCard(medicalHistory = uiState.medicalHistory)
                    }
                }
                item {
                    if (uiState.isEditing) {
                        EmergencyContactsEditCard(
                            emergencyContacts = uiState.draftEmergencyContacts,
                            onAddContact = viewModel::addEmergencyContact,
                            onRemoveContact = viewModel::removeEmergencyContact
                        )
                    } else {
                        EmergencyContactsCard(emergencyContacts = uiState.emergencyContacts)
                    }
                }
            }
        }
    }
}

// ==================== VIEW MODE CARDS ====================

@Composable
private fun DemographicsCard(demographics: Demographics) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.demographics_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("demographics_title")
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (demographics.fullName.isEmpty() && demographics.dateOfBirth.isEmpty() && demographics.sex.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_demographics),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                InfoRow(label = stringResource(R.string.label_name), value = demographics.fullName)
                InfoRow(label = stringResource(R.string.label_date_of_birth), value = demographics.dateOfBirth)
                InfoRow(label = stringResource(R.string.label_sex), value = demographics.sex)
                InfoRow(label = stringResource(R.string.label_height), value = demographics.heightCm?.let { stringResource(R.string.format_cm_value, UnitConverter.formatDouble(it)) } ?: "")
                InfoRow(label = stringResource(R.string.label_weight), value = demographics.weightKg?.let { stringResource(R.string.format_kg_value, UnitConverter.formatDouble(it)) } ?: "")
                InfoRow(label = stringResource(R.string.label_race_ethnicity), value = demographics.raceEthnicity)
            }
        }
    }
}

@Composable
private fun MedicalProfileCard(medicalProfile: MedicalProfile) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.medical_profile_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("medical_profile_title")
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (medicalProfile.bloodType == null && medicalProfile.allergies.isEmpty() && medicalProfile.medications.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_medical_profile),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                InfoRow(label = stringResource(R.string.label_blood_type), value = medicalProfile.bloodType?.displayName ?: "")

                if (medicalProfile.allergies.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.label_allergies),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    medicalProfile.allergies.forEach { allergy ->
                        Text(
                            text = "• $allergy",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                if (medicalProfile.medications.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.label_medications),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    medicalProfile.medications.forEach { medication ->
                        Text(
                            text = stringResource(R.string.medication_item, medication.name, medication.dosage, medication.schedule),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                if (medicalProfile.adverseReactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.label_adverse_reactions),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    medicalProfile.adverseReactions.forEach { reaction ->
                        Text(
                            text = "• $reaction",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicalHistoryCard(medicalHistory: MedicalHistory) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.medical_history_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("medical_history_title")
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (medicalHistory.hereditaryDiseases.isEmpty() && medicalHistory.chronicConditions.isEmpty() && medicalHistory.surgicalHistory.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_medical_history),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (medicalHistory.hereditaryDiseases.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.label_hereditary_diseases),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    medicalHistory.hereditaryDiseases.forEach { disease ->
                        Text(
                            text = "• $disease",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (medicalHistory.chronicConditions.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.label_chronic_conditions),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    medicalHistory.chronicConditions.forEach { condition ->
                        Text(
                            text = "• $condition",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (medicalHistory.surgicalHistory.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.label_surgical_history),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    medicalHistory.surgicalHistory.forEach { procedure ->
                        Text(
                            text = "• $procedure",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyContactsCard(emergencyContacts: EmergencyContacts) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.emergency_contacts_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("emergency_contacts_title")
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (emergencyContacts.contacts.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_emergency_contacts),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                emergencyContacts.contacts.forEach { contact ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = contact.relationship,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = contact.phoneNumber,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    if (value.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ==================== EDIT MODE CARDS ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemographicsEditCard(
    demographics: Demographics,
    dateOfBirthValue: TextFieldValue,
    heightText: String,
    weightText: String,
    validation: DemographicsValidationResult,
    unitSystem: UnitSystem,
    onFullNameChanged: (String) -> Unit,
    onDateOfBirthChanged: (TextFieldValue) -> Unit,
    onDateOfBirthSelected: (String) -> Unit,
    onSexChanged: (String) -> Unit,
    onHeightChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onRaceEthnicityChanged: (String) -> Unit,
    onUnitSystemChanged: (UnitSystem) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var unitSystemExpanded by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.demographics_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Unit System Toggle
            ExposedDropdownMenuBox(
                expanded = unitSystemExpanded,
                onExpandedChange = { unitSystemExpanded = it }
            ) {
                OutlinedTextField(
                    value = stringResource(if (unitSystem == UnitSystem.METRIC) R.string.unit_system_metric else R.string.unit_system_imperial),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_unit_system)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitSystemExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = unitSystemExpanded,
                    onDismissRequest = { unitSystemExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.unit_system_metric)) },
                        onClick = {
                            onUnitSystemChanged(UnitSystem.METRIC)
                            unitSystemExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.unit_system_imperial)) },
                        onClick = {
                            onUnitSystemChanged(UnitSystem.IMPERIAL)
                            unitSystemExpanded = false
                        }
                    )
                }
            }

            val fullNameContentDescription = stringResource(R.string.cd_full_name, demographics.fullName)
            OutlinedTextField(
                value = demographics.fullName,
                onValueChange = onFullNameChanged,
                label = { Text(stringResource(R.string.label_full_name)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = fullNameContentDescription }
            )

            // Date of Birth with Date Picker
            OutlinedTextField(
                value = dateOfBirthValue,
                onValueChange = onDateOfBirthChanged,
                label = { Text(stringResource(R.string.label_dob_edit)) },
                isError = validation.dateOfBirth is ValidationResult.Invalid,
                supportingText = if (validation.dateOfBirth is ValidationResult.Invalid) {
                    { Text(stringResource(validation.dateOfBirth.errorResId)) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.select_date))
                    }
                }
            )

            val sexContentDescription = stringResource(R.string.cd_sex, demographics.sex)
            OutlinedTextField(
                value = demographics.sex,
                onValueChange = onSexChanged,
                label = { Text(stringResource(R.string.label_sex)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = sexContentDescription }
            )

            // Height with unit conversion
            val heightUnit = stringResource(if (unitSystem == UnitSystem.METRIC) R.string.height_unit_cm else R.string.height_unit_in)
            val heightContentDescription = stringResource(R.string.cd_height, heightUnit, heightText)
            OutlinedTextField(
                value = heightText,
                onValueChange = onHeightChanged,
                label = { Text(stringResource(R.string.label_height_with_unit, heightUnit)) },
                isError = validation.height is ValidationResult.Invalid,
                supportingText = if (validation.height is ValidationResult.Invalid) {
                    { Text(stringResource(validation.height.errorResId, *validation.height.formatArgs.toTypedArray())) }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = heightContentDescription },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                )
            )

            // Weight with unit conversion
            val weightUnit = stringResource(if (unitSystem == UnitSystem.METRIC) R.string.weight_unit_kg else R.string.weight_unit_lbs)
            val weightContentDescription = stringResource(R.string.cd_weight, weightUnit, weightText)
            OutlinedTextField(
                value = weightText,
                onValueChange = onWeightChanged,
                label = { Text(stringResource(R.string.label_weight_with_unit, weightUnit)) },
                isError = validation.weight is ValidationResult.Invalid,
                supportingText = if (validation.weight is ValidationResult.Invalid) {
                    { Text(stringResource(validation.weight.errorResId, *validation.weight.formatArgs.toTypedArray())) }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = weightContentDescription },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                )
            )

            val raceEthnicityContentDescription = stringResource(R.string.cd_race_ethnicity, demographics.raceEthnicity)
            OutlinedTextField(
                value = demographics.raceEthnicity,
                onValueChange = onRaceEthnicityChanged,
                label = { Text(stringResource(R.string.label_race_ethnicity)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = raceEthnicityContentDescription }
            )
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateOfBirthSelected(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicalProfileEditCard(
    medicalProfile: MedicalProfile,
    onBloodTypeChanged: (BloodType?) -> Unit,
    onAddAllergy: (String) -> Unit,
    onRemoveAllergy: (Int) -> Unit,
    onAddMedication: (MedicationEntry) -> Unit,
    onRemoveMedication: (Int) -> Unit,
    onAddAdverseReaction: (String) -> Unit,
    onRemoveAdverseReaction: (Int) -> Unit
) {
    var showAddAllergyDialog by remember { mutableStateOf(false) }
    var showAddMedicationDialog by remember { mutableStateOf(false) }
    var showAddReactionDialog by remember { mutableStateOf(false) }
    var bloodTypeExpanded by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.medical_profile_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = bloodTypeExpanded,
                onExpandedChange = { bloodTypeExpanded = it }
            ) {
                OutlinedTextField(
                    value = medicalProfile.bloodType?.displayName ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_blood_type)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodTypeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = bloodTypeExpanded,
                    onDismissRequest = { bloodTypeExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.blood_type_none)) },
                        onClick = {
                            onBloodTypeChanged(null)
                            bloodTypeExpanded = false
                        }
                    )
                    BloodType.entries.forEach { bloodType ->
                        DropdownMenuItem(
                            text = { Text(bloodType.displayName) },
                            onClick = {
                                onBloodTypeChanged(bloodType)
                                bloodTypeExpanded = false
                            }
                        )
                    }
                }
            }

            // Allergies
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_allergies),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { showAddAllergyDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_allergy))
                }
            }
            medicalProfile.allergies.forEachIndexed { index, allergy ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• $allergy",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onRemoveAllergy(index) }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_allergy))
                    }
                }
            }

            // Medications
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_medications),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { showAddMedicationDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_medication))
                }
            }
            medicalProfile.medications.forEachIndexed { index, medication ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.medication_item, medication.name, medication.dosage, medication.schedule),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onRemoveMedication(index) }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_medication))
                    }
                }
            }

            // Adverse Reactions
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_adverse_reactions),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { showAddReactionDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_adverse_reaction))
                }
            }
            medicalProfile.adverseReactions.forEachIndexed { index, reaction ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• $reaction",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onRemoveAdverseReaction(index) }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_adverse_reaction))
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddAllergyDialog) {
        AddStringDialog(
            title = stringResource(R.string.dialog_add_allergy),
            label = stringResource(R.string.label_allergy),
            onConfirm = { onAddAllergy(it); showAddAllergyDialog = false },
            onDismiss = { showAddAllergyDialog = false }
        )
    }

    if (showAddMedicationDialog) {
        AddMedicationDialog(
            onConfirm = { onAddMedication(it); showAddMedicationDialog = false },
            onDismiss = { showAddMedicationDialog = false }
        )
    }

    if (showAddReactionDialog) {
        AddStringDialog(
            title = stringResource(R.string.dialog_add_adverse_reaction),
            label = stringResource(R.string.label_reaction),
            onConfirm = { onAddAdverseReaction(it); showAddReactionDialog = false },
            onDismiss = { showAddReactionDialog = false }
        )
    }
}

@Composable
private fun MedicalHistoryEditCard(
    medicalHistory: MedicalHistory,
    onAddHereditaryDisease: (String) -> Unit,
    onRemoveHereditaryDisease: (Int) -> Unit,
    onAddChronicCondition: (String) -> Unit,
    onRemoveChronicCondition: (Int) -> Unit,
    onAddSurgicalHistory: (String) -> Unit,
    onRemoveSurgicalHistory: (Int) -> Unit
) {
    var showAddDiseaseDialog by remember { mutableStateOf(false) }
    var showAddConditionDialog by remember { mutableStateOf(false) }
    var showAddProcedureDialog by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.medical_history_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Hereditary Diseases
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_hereditary_diseases),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { showAddDiseaseDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_hereditary_disease))
                }
            }
            medicalHistory.hereditaryDiseases.forEachIndexed { index, disease ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• $disease",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onRemoveHereditaryDisease(index) }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_hereditary_disease))
                    }
                }
            }

            // Chronic Conditions
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_chronic_conditions),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { showAddConditionDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_chronic_condition))
                }
            }
            medicalHistory.chronicConditions.forEachIndexed { index, condition ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• $condition",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onRemoveChronicCondition(index) }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_chronic_condition))
                    }
                }
            }

            // Surgical History
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_surgical_history),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { showAddProcedureDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_surgical_procedure))
                }
            }
            medicalHistory.surgicalHistory.forEachIndexed { index, procedure ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• $procedure",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onRemoveSurgicalHistory(index) }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_surgical_procedure))
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddDiseaseDialog) {
        AddStringDialog(
            title = stringResource(R.string.dialog_add_hereditary_disease),
            label = stringResource(R.string.label_disease),
            onConfirm = { onAddHereditaryDisease(it); showAddDiseaseDialog = false },
            onDismiss = { showAddDiseaseDialog = false }
        )
    }

    if (showAddConditionDialog) {
        AddStringDialog(
            title = stringResource(R.string.dialog_add_chronic_condition),
            label = stringResource(R.string.label_condition),
            onConfirm = { onAddChronicCondition(it); showAddConditionDialog = false },
            onDismiss = { showAddConditionDialog = false }
        )
    }

    if (showAddProcedureDialog) {
        AddStringDialog(
            title = stringResource(R.string.dialog_add_surgical_procedure),
            label = stringResource(R.string.label_procedure),
            onConfirm = { onAddSurgicalHistory(it); showAddProcedureDialog = false },
            onDismiss = { showAddProcedureDialog = false }
        )
    }
}

@Composable
private fun EmergencyContactsEditCard(
    emergencyContacts: EmergencyContacts,
    onAddContact: (EmergencyContact) -> Unit,
    onRemoveContact: (Int) -> Unit
) {
    var showAddContactDialog by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.emergency_contacts_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { showAddContactDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_emergency_contact))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (emergencyContacts.contacts.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_emergency_contacts),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                emergencyContacts.contacts.forEachIndexed { index, contact ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = contact.relationship,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = contact.phoneNumber,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            IconButton(onClick = { onRemoveContact(index) }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_contact))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddContactDialog) {
        AddContactDialog(
            onConfirm = { onAddContact(it); showAddContactDialog = false },
            onDismiss = { showAddContactDialog = false }
        )
    }
}

// ==================== DIALOGS ====================

@Composable
private fun AddStringDialog(
    title: String,
    label: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddMedicationDialog(
    onConfirm: (MedicationEntry) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_add_medication)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_drug_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text(stringResource(R.string.label_dosage)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = schedule,
                    onValueChange = { schedule = it },
                    label = { Text(stringResource(R.string.label_schedule)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text(stringResource(R.string.label_purpose)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(MedicationEntry(
                        name = name,
                        dosage = dosage,
                        schedule = schedule,
                        purpose = purpose
                    ))
                },
                enabled = name.isNotBlank() && dosage.isNotBlank() && schedule.isNotBlank()
            ) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddContactDialog(
    onConfirm: (EmergencyContact) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_add_emergency_contact)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text(stringResource(R.string.label_relationship)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text(stringResource(R.string.label_phone_number)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(EmergencyContact(
                        name = name,
                        relationship = relationship,
                        phoneNumber = phoneNumber
                    ))
                },
                enabled = name.isNotBlank() && phoneNumber.isNotBlank()
            ) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}