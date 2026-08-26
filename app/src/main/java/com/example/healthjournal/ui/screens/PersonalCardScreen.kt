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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthjournal.data.local.Demographics
import com.example.healthjournal.data.local.EmergencyContact
import com.example.healthjournal.data.local.EmergencyContacts
import com.example.healthjournal.data.local.MedicalHistory
import com.example.healthjournal.data.local.MedicalProfile
import com.example.healthjournal.data.local.MedicationEntry
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
                title = { Text("Personal Card") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isEditing) {
                            viewModel.cancelEditing()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                },
                actions = {
                    if (uiState.isEditing) {
                        TextButton(onClick = { viewModel.cancelEditing() }) {
                            Text("Cancel")
                        }
                        TextButton(onClick = { viewModel.saveChanges() }) {
                            Text("Save")
                        }
                    } else {
                        IconButton(onClick = { viewModel.startEditing() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit personal card")
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
                    text = "Loading...",
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
                            onFullNameChanged = viewModel::onFullNameChanged,
                            onDateOfBirthChanged = viewModel::onDateOfBirthChanged,
                            onSexChanged = viewModel::onSexChanged,
                            onHeightChanged = viewModel::onHeightChanged,
                            onWeightChanged = viewModel::onWeightChanged,
                            onRaceEthnicityChanged = viewModel::onRaceEthnicityChanged
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
                text = "Demographics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("demographics_title")
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (demographics.fullName.isEmpty() && demographics.dateOfBirth.isEmpty() && demographics.sex.isEmpty()) {
                Text(
                    text = "No demographics information added yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                InfoRow(label = "Name", value = demographics.fullName)
                InfoRow(label = "Date of Birth", value = demographics.dateOfBirth)
                InfoRow(label = "Sex", value = demographics.sex)
                InfoRow(label = "Height", value = demographics.heightCm?.let { "$it cm" } ?: "")
                InfoRow(label = "Weight", value = demographics.weightKg?.let { "$it kg" } ?: "")
                InfoRow(label = "Race/Ethnicity", value = demographics.raceEthnicity)
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
                text = "Medical Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("medical_profile_title")
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (medicalProfile.bloodType.isEmpty() && medicalProfile.allergies.isEmpty() && medicalProfile.medications.isEmpty()) {
                Text(
                    text = "No medical profile information added yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                InfoRow(label = "Blood Type", value = medicalProfile.bloodType)

                if (medicalProfile.allergies.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Allergies",
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
                        text = "Medications",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    medicalProfile.medications.forEach { medication ->
                        Text(
                            text = "• ${medication.name} ${medication.dosage} - ${medication.schedule}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                if (medicalProfile.adverseReactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Adverse Reactions",
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
                text = "Medical History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("medical_history_title")
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (medicalHistory.hereditaryDiseases.isEmpty() && medicalHistory.chronicConditions.isEmpty() && medicalHistory.surgicalHistory.isEmpty()) {
                Text(
                    text = "No medical history information added yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (medicalHistory.hereditaryDiseases.isNotEmpty()) {
                    Text(
                        text = "Hereditary Diseases",
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
                        text = "Chronic Conditions",
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
                        text = "Surgical History",
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
                text = "Emergency Contacts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("emergency_contacts_title")
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (emergencyContacts.contacts.isEmpty()) {
                Text(
                    text = "No emergency contacts added yet.",
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

@Composable
private fun DemographicsEditCard(
    demographics: Demographics,
    onFullNameChanged: (String) -> Unit,
    onDateOfBirthChanged: (String) -> Unit,
    onSexChanged: (String) -> Unit,
    onHeightChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onRaceEthnicityChanged: (String) -> Unit
) {
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
                text = "Demographics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = demographics.fullName,
                onValueChange = onFullNameChanged,
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = demographics.dateOfBirth,
                onValueChange = onDateOfBirthChanged,
                label = { Text("Date of Birth (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = demographics.sex,
                onValueChange = onSexChanged,
                label = { Text("Sex") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = demographics.heightCm?.toString() ?: "",
                onValueChange = onHeightChanged,
                label = { Text("Height (cm)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = demographics.weightKg?.toString() ?: "",
                onValueChange = onWeightChanged,
                label = { Text("Weight (kg)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = demographics.raceEthnicity,
                onValueChange = onRaceEthnicityChanged,
                label = { Text("Race/Ethnicity") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MedicalProfileEditCard(
    medicalProfile: MedicalProfile,
    onBloodTypeChanged: (String) -> Unit,
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
                text = "Medical Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = medicalProfile.bloodType,
                onValueChange = onBloodTypeChanged,
                label = { Text("Blood Type") },
                modifier = Modifier.fillMaxWidth()
            )

            // Allergies
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Allergies",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { showAddAllergyDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add allergy")
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
                        Icon(Icons.Default.Close, contentDescription = "Remove allergy")
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
                    text = "Medications",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { showAddMedicationDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add medication")
                }
            }
            medicalProfile.medications.forEachIndexed { index, medication ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• ${medication.name} ${medication.dosage} - ${medication.schedule}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onRemoveMedication(index) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove medication")
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
                    text = "Adverse Reactions",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { showAddReactionDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add adverse reaction")
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
                        Icon(Icons.Default.Close, contentDescription = "Remove adverse reaction")
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddAllergyDialog) {
        AddStringDialog(
            title = "Add Allergy",
            label = "Allergy",
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
            title = "Add Adverse Reaction",
            label = "Reaction",
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
                text = "Medical History",
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
                    text = "Hereditary Diseases",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { showAddDiseaseDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add hereditary disease")
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
                        Icon(Icons.Default.Close, contentDescription = "Remove hereditary disease")
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
                    text = "Chronic Conditions",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { showAddConditionDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add chronic condition")
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
                        Icon(Icons.Default.Close, contentDescription = "Remove chronic condition")
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
                    text = "Surgical History",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { showAddProcedureDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add surgical procedure")
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
                        Icon(Icons.Default.Close, contentDescription = "Remove surgical procedure")
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddDiseaseDialog) {
        AddStringDialog(
            title = "Add Hereditary Disease",
            label = "Disease",
            onConfirm = { onAddHereditaryDisease(it); showAddDiseaseDialog = false },
            onDismiss = { showAddDiseaseDialog = false }
        )
    }

    if (showAddConditionDialog) {
        AddStringDialog(
            title = "Add Chronic Condition",
            label = "Condition",
            onConfirm = { onAddChronicCondition(it); showAddConditionDialog = false },
            onDismiss = { showAddConditionDialog = false }
        )
    }

    if (showAddProcedureDialog) {
        AddStringDialog(
            title = "Add Surgical Procedure",
            label = "Procedure (with date if known)",
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
                    text = "Emergency Contacts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { showAddContactDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add emergency contact")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (emergencyContacts.contacts.isEmpty()) {
                Text(
                    text = "No emergency contacts added yet.",
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
                                Icon(Icons.Default.Close, contentDescription = "Remove contact")
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
                Text("Add")
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
        title = { Text("Add Medication") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Drug Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage (e.g., 500mg)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = schedule,
                    onValueChange = { schedule = it },
                    label = { Text("Schedule (e.g., Twice daily)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text("Purpose (optional)") },
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
                Text("Add")
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
        title = { Text("Add Emergency Contact") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Relationship") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
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
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}