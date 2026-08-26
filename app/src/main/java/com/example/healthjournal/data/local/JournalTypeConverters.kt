package com.example.healthjournal.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class JournalTypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value == null) return emptyList()
        return try {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromAttachmentList(value: List<AttachmentData>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toAttachmentList(value: String?): List<AttachmentData> {
        if (value == null) return emptyList()
        return try {
            val listType = object : TypeToken<List<AttachmentData>>() {}.type
            val rawList = gson.fromJson<List<AttachmentData>>(value, listType) ?: emptyList()
            rawList.map { att ->
                att.copy(
                    syncStatus = att.syncStatus ?: "PENDING",
                    isLocalOnly = att.isLocalOnly ?: true
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromDemographics(value: Demographics): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toDemographics(value: String?): Demographics {
        if (value == null) return Demographics()
        return try {
            gson.fromJson(value, Demographics::class.java) ?: Demographics()
        } catch (e: Exception) {
            Demographics()
        }
    }

    @TypeConverter
    fun fromMedicalProfile(value: MedicalProfile): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toMedicalProfile(value: String?): MedicalProfile {
        if (value == null) return MedicalProfile()
        return try {
            gson.fromJson(value, MedicalProfile::class.java) ?: MedicalProfile()
        } catch (e: Exception) {
            MedicalProfile()
        }
    }

    @TypeConverter
    fun fromMedicalHistory(value: MedicalHistory): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toMedicalHistory(value: String?): MedicalHistory {
        if (value == null) return MedicalHistory()
        return try {
            gson.fromJson(value, MedicalHistory::class.java) ?: MedicalHistory()
        } catch (e: Exception) {
            MedicalHistory()
        }
    }

    @TypeConverter
    fun fromEmergencyContacts(value: EmergencyContacts): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toEmergencyContacts(value: String?): EmergencyContacts {
        if (value == null) return EmergencyContacts()
        return try {
            gson.fromJson(value, EmergencyContacts::class.java) ?: EmergencyContacts()
        } catch (e: Exception) {
            EmergencyContacts()
        }
    }

    @TypeConverter
    fun fromMedicationEntryList(value: List<MedicationEntry>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toMedicationEntryList(value: String?): List<MedicationEntry> {
        if (value == null) return emptyList()
        return try {
            val listType = object : TypeToken<List<MedicationEntry>>() {}.type
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromEmergencyContactList(value: List<EmergencyContact>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toEmergencyContactList(value: String?): List<EmergencyContact> {
        if (value == null) return emptyList()
        return try {
            val listType = object : TypeToken<List<EmergencyContact>>() {}.type
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
