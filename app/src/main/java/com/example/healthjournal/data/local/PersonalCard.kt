package com.example.healthjournal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

data class MedicationEntry(
    val name: String = "",
    val dosage: String = "",
    val schedule: String = "",
    val purpose: String = ""
)

data class EmergencyContact(
    val name: String = "",
    val relationship: String = "",
    val phoneNumber: String = ""
)

data class Demographics(
    val fullName: String = "",
    val dateOfBirth: String = "",
    val sex: String = "",
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val raceEthnicity: String = ""
)

data class MedicalProfile(
    val bloodType: String = "",
    val allergies: List<String> = emptyList(),
    val medications: List<MedicationEntry> = emptyList(),
    val adverseReactions: List<String> = emptyList()
)

data class MedicalHistory(
    val hereditaryDiseases: List<String> = emptyList(),
    val chronicConditions: List<String> = emptyList(),
    val surgicalHistory: List<String> = emptyList()
)

data class EmergencyContacts(
    val contacts: List<EmergencyContact> = emptyList()
)

@Entity(tableName = "personal_card")
data class PersonalCard(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val isSynced: Boolean? = false,
    val syncStatus: String? = "PENDING_SYNC",
    val demographics: Demographics = Demographics(),
    val medicalProfile: MedicalProfile = MedicalProfile(),
    val medicalHistory: MedicalHistory = MedicalHistory(),
    val emergencyContacts: EmergencyContacts = EmergencyContacts()
)