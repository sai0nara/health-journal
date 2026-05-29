package com.example.healthjournal.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class JournalTypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value ?: emptyList<String>())
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
        return gson.toJson(value ?: emptyList<AttachmentData>())
    }

    @TypeConverter
    fun toAttachmentList(value: String?): List<AttachmentData> {
        if (value == null) return emptyList()
        return try {
            val listType = object : TypeToken<List<AttachmentData>>() {}.type
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
