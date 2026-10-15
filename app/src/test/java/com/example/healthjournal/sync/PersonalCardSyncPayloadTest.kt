package com.example.healthjournal.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract for the personal_card.json cloud payload: explicit JSON nulls
 * (or missing objects) from another writer must sanitize to defaults so
 * the read-only cards never crash dereferencing non-null Kotlin fields.
 */
class PersonalCardSyncPayloadTest {

    private val poisonedJson = """
        [{
          "id": "personal_card",
          "timestamp": 1,
          "lastModified": 2,
          "isSynced": false,
          "syncStatus": "SYNCED",
          "demographics": {"fullName": null, "dateOfBirth": null, "sex": null, "heightCm": 180.0, "weightKg": null, "raceEthnicity": null},
          "medicalProfile": {"bloodType": null, "allergies": null, "medications": null, "adverseReactions": null},
          "medicalHistory": null,
          "emergencyContacts": null
        }]
    """.trimIndent()

    @Test
    fun payload_nullPoisonedJson_sanitizesToDefaults() {
        val card = PersonalCardSyncPayload.fromJson(poisonedJson).single()

        // Mirrors what DemographicsCard/MedicalProfileCard dereference on open.
        assertEquals("", card.demographics.fullName)
        assertEquals("", card.demographics.dateOfBirth)
        assertEquals("", card.demographics.sex)
        assertEquals("", card.demographics.raceEthnicity)
        assertEquals(180.0, card.demographics.heightCm)
        assertTrue(card.medicalProfile.allergies.isEmpty())
        assertTrue(card.medicalProfile.medications.isEmpty())
        assertTrue(card.medicalProfile.adverseReactions.isEmpty())
        assertTrue(card.medicalHistory.hereditaryDiseases.isEmpty())
        assertTrue(card.emergencyContacts.contacts.isEmpty())
    }

    @Test
    fun payload_nullListElements_keepsCardWithEmptyLists() {
        val json = """
            [{
              "id": "personal_card",
              "demographics": {"fullName": "A", "dateOfBirth": "", "sex": "", "heightCm": 180.0, "weightKg": 70.0, "raceEthnicity": ""},
              "medicalProfile": {"allergies": [null], "medications": [null], "adverseReactions": [null]},
              "medicalHistory": {"hereditaryDiseases": [null], "chronicConditions": [], "surgicalHistory": []},
              "emergencyContacts": {"contacts": [null]}
            }]
        """.trimIndent()

        val card = PersonalCardSyncPayload.fromJson(json).single()

        assertTrue(card.medicalProfile.allergies.isEmpty())
        assertTrue(card.medicalProfile.medications.isEmpty())
        assertTrue(card.medicalProfile.adverseReactions.isEmpty())
        assertTrue(card.medicalHistory.hereditaryDiseases.isEmpty())
        assertTrue(card.emergencyContacts.contacts.isEmpty())
    }

    @Test
    fun payload_nonFiniteDoubles_sanitizeToUnset() {
        val json = """
            [{
              "id": "personal_card",
              "demographics": {"fullName": "A", "dateOfBirth": "", "sex": "", "heightCm": 1e999, "weightKg": 70.0, "raceEthnicity": ""},
              "medicalProfile": {"allergies": [], "medications": [], "adverseReactions": []},
              "medicalHistory": {"hereditaryDiseases": [], "chronicConditions": [], "surgicalHistory": []},
              "emergencyContacts": {"contacts": []}
            }]
        """.trimIndent()

        val card = PersonalCardSyncPayload.fromJson(json).single()

        assertNull(card.demographics.heightCm)
        assertEquals(70.0, card.demographics.weightKg)
    }
}
