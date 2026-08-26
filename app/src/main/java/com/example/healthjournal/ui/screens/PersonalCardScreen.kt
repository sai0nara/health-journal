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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startEditing() }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit personal card")
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
                    DemographicsCard(demographics = uiState.demographics)
                }
                item {
                    MedicalProfileCard(medicalProfile = uiState.medicalProfile)
                }
                item {
                    MedicalHistoryCard(medicalHistory = uiState.medicalHistory)
                }
                item {
                    EmergencyContactsCard(emergencyContacts = uiState.emergencyContacts)
                }
            }
        }
    }
}

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