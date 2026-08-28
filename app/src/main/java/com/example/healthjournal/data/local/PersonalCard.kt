package com.example.healthjournal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BloodType(val displayName: String) {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    companion object {
        fun fromDisplayName(displayName: String): BloodType? {
            return entries.find { it.displayName == displayName }
        }
    }
}

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
    val bloodType: BloodType? = null,
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
    val id: String = "personal_card",
    val timestamp: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val isSynced: Boolean? = false,
    val syncStatus: String? = "PENDING_SYNC",
    val demographics: Demographics = Demographics(),
    val medicalProfile: MedicalProfile = MedicalProfile(),
    val medicalHistory: MedicalHistory = MedicalHistory(),
    val emergencyContacts: EmergencyContacts = EmergencyContacts()
)
