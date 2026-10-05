package com.example.healthjournal.sync

import com.example.healthjournal.data.local.PersonalCard
import com.google.gson.Gson

/**
 * Gson codec for the personal_card.json Drive payload: a bare
 * JSON list. A missing or unparseable cloud file deserializes to
 * an empty list so the sync gracefully falls back to local data.
 */
object PersonalCardSyncPayload {
    private val gson = Gson()


    fun toJson(cards: List<PersonalCard>): String =
        gson.toJson(cards)

    fun fromJson(json: String?): List<PersonalCard> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson(json, Array<PersonalCard>::class.java)?.toList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
