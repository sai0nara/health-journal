package com.example.healthjournal.sync

import com.example.healthjournal.data.local.ProfileDeletionEntity
import com.example.healthjournal.data.local.ProfileEntity
import com.example.healthjournal.data.local.ProfileRelationship
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSyncPayloadTest {

    @Test
    fun toJsonFromJson_roundTripsProfilesAndDeletions() {
        val profiles = listOf(
            ProfileEntity(
                id = SELF_PROFILE_ID,
                name = "Me",
                relationship = ProfileRelationship.SELF.name,
                accentColor = 1,
                isArchived = false,
                createdAt = 1_000L,
                lastModified = 2_000L,
                isSynced = true,
                syncStatus = "SYNCED"
            ),
            ProfileEntity(
                id = "sib-1",
                name = "Alex",
                relationship = ProfileRelationship.SIBLING.name,
                accentColor = 3,
                isArchived = true,
                createdAt = 3_000L,
                lastModified = 4_000L,
                isSynced = true,
                syncStatus = "SYNCED"
            )
        )
        val deletions = listOf(
            ProfileDeletionEntity(profileId = "sib-deleted", deletedAt = 5_000L)
        )

        val json = ProfileSyncPayload.toJson(profiles, deletions)
        val parsed = ProfileSyncPayload.fromJson(json)

        assertEquals(profiles, parsed.profiles)
        assertEquals(deletions, parsed.deletions)
    }

    @Test
    fun fromJson_nullOrBlank_returnsEmptySnapshot() {
        assertTrue(ProfileSyncPayload.fromJson(null).profiles.isEmpty())
        assertTrue(ProfileSyncPayload.fromJson(null).deletions.isEmpty())
        assertTrue(ProfileSyncPayload.fromJson("").profiles.isEmpty())
        assertTrue(ProfileSyncPayload.fromJson("   ").deletions.isEmpty())
    }

    @Test
    fun fromJson_garbageJson_returnsEmptySnapshot() {
        val parsed = ProfileSyncPayload.fromJson("{not-valid-json")
        assertTrue(parsed.profiles.isEmpty())
        assertTrue(parsed.deletions.isEmpty())
    }
}
