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

/**
 * Gson allocates Kotlin data classes without calling constructors, so
 * explicit JSON nulls (e.g. from another writer's cloud payload) land in
 * non-null fields and crash readers later. Sanitize at decode boundaries
 * so poisoned data can never reach the UI or the database.
 */
@Suppress("UNNECESSARY_SAFE_CALL", "UNNECESSARY_ELVIS")
fun Demographics.sanitized(): Demographics = copy(
    fullName = fullName ?: "",
    dateOfBirth = dateOfBirth ?: "",
    sex = sex ?: "",
    heightCm = heightCm?.takeIf { it.isFinite() },
    weightKg = weightKg?.takeIf { it.isFinite() },
    raceEthnicity = raceEthnicity ?: ""
)

/** See [Demographics.sanitized] for why this exists. */
@Suppress("UNNECESSARY_SAFE_CALL", "UNNECESSARY_ELVIS")
fun MedicalProfile.sanitized(): MedicalProfile = copy(
    allergies = allergies?.filterNotNull() ?: emptyList(),
    medications = medications?.mapNotNull { med ->
        med?.copy(
            name = med?.name ?: "",
            dosage = med?.dosage ?: "",
            schedule = med?.schedule ?: "",
            purpose = med?.purpose ?: ""
        )
    } ?: emptyList(),
    adverseReactions = adverseReactions?.filterNotNull() ?: emptyList()
)

/** See [Demographics.sanitized] for why this exists. */
@Suppress("UNNECESSARY_SAFE_CALL", "UNNECESSARY_ELVIS")
fun MedicalHistory.sanitized(): MedicalHistory = copy(
    hereditaryDiseases = hereditaryDiseases?.filterNotNull() ?: emptyList(),
    chronicConditions = chronicConditions?.filterNotNull() ?: emptyList(),
    surgicalHistory = surgicalHistory?.filterNotNull() ?: emptyList()
)

/** See [Demographics.sanitized] for why this exists. */
@Suppress("UNNECESSARY_SAFE_CALL", "UNNECESSARY_ELVIS")
fun EmergencyContacts.sanitized(): EmergencyContacts = copy(
    contacts = contacts?.mapNotNull { contact ->
        contact?.copy(
            name = contact?.name ?: "",
            relationship = contact?.relationship ?: "",
            phoneNumber = contact?.phoneNumber ?: ""
        )
    } ?: emptyList()
)

/** See [Demographics.sanitized] for why this exists. */
@Suppress("UNNECESSARY_SAFE_CALL", "UNNECESSARY_ELVIS")
fun PersonalCard.sanitized(): PersonalCard = copy(
    demographics = demographics?.sanitized() ?: Demographics(),
    medicalProfile = medicalProfile?.sanitized() ?: MedicalProfile(),
    medicalHistory = medicalHistory?.sanitized() ?: MedicalHistory(),
    emergencyContacts = emergencyContacts?.sanitized() ?: EmergencyContacts()
)
