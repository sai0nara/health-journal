package com.example.healthjournal.sync

import com.example.healthjournal.data.local.ProfileDeletionEntity
import com.example.healthjournal.data.local.ProfileEntity
import com.example.healthjournal.data.local.ProfileRelationship
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMergeProfileTest {

    @Test
    fun mergeProfiles_newerLastModifiedWins_andCloudWinsTies() {
        val cloudSelf = ProfileEntity(
            id = SELF_PROFILE_ID,
            name = "Me Cloud",
            relationship = ProfileRelationship.SELF.name,
            createdAt = 100L,
            lastModified = 2_000L
        )
        val localSelf = ProfileEntity(
            id = SELF_PROFILE_ID,
            name = "Me Local",
            relationship = ProfileRelationship.SELF.name,
            createdAt = 100L,
            lastModified = 1_000L
        )
        val cloudSib = ProfileEntity(
            id = "sib-1",
            name = "Alex Cloud",
            relationship = ProfileRelationship.SIBLING.name,
            createdAt = 200L,
            lastModified = 1_000L
        )
        val localSib = ProfileEntity(
            id = "sib-1",
            name = "Alex Local",
            relationship = ProfileRelationship.SIBLING.name,
            createdAt = 200L,
            lastModified = 3_000L
        )
        val cloudTie = ProfileEntity(
            id = "sib-tie",
            name = "Tie Cloud",
            relationship = ProfileRelationship.OTHER.name,
            createdAt = 300L,
            lastModified = 1_500L
        )
        val localTie = ProfileEntity(
            id = "sib-tie",
            name = "Tie Local",
            relationship = ProfileRelationship.OTHER.name,
            createdAt = 300L,
            lastModified = 1_500L
        )

        val result = SyncMerge.mergeProfiles(
            cloudProfiles = listOf(cloudSelf, cloudSib, cloudTie),
            localProfiles = listOf(localSelf, localSib, localTie)
        )
        val byId = result.profiles.associateBy { it.id }

        assertEquals("Me Cloud", byId.getValue(SELF_PROFILE_ID).name)
        assertEquals("Alex Local", byId.getValue("sib-1").name)
        assertEquals("Tie Cloud", byId.getValue("sib-tie").name)
    }

    @Test
    fun mergeProfiles_deletionMarkerRemovesProfileAndReportsDeletedId() {
        val localSib = ProfileEntity(
            id = "sib-del",
            name = "Sam",
            relationship = ProfileRelationship.SIBLING.name,
            isArchived = true,
            createdAt = 100L,
            lastModified = 1_000L
        )
        val cloudDeletion = ProfileDeletionEntity(
            profileId = "sib-del",
            deletedAt = 2_000L
        )

        val result = SyncMerge.mergeProfiles(
            cloudProfiles = emptyList(),
            localProfiles = listOf(localSib),
            cloudDeletions = listOf(cloudDeletion),
            localDeletions = emptyList()
        )

        assertFalse(result.profiles.any { it.id == "sib-del" })
        assertTrue("sib-del" in result.deletedProfileIds)
        assertEquals(listOf(cloudDeletion), result.deletions)
    }

    @Test
    fun mergeProfiles_legacyEmptyCloud_preservesLocalProfilesAndDefaultsSelfWhenEmpty() {
        val localSelf = ProfileEntity(
            id = SELF_PROFILE_ID,
            name = "Me",
            relationship = ProfileRelationship.SELF.name,
            createdAt = 100L,
            lastModified = 100L
        )
        val resultWithLocal = SyncMerge.mergeProfiles(
            cloudProfiles = emptyList(),
            localProfiles = listOf(localSelf)
        )
        assertEquals(listOf(localSelf), resultWithLocal.profiles)

        val resultBothEmpty = SyncMerge.mergeProfiles(
            cloudProfiles = emptyList(),
            localProfiles = emptyList()
        )
        assertEquals(1, resultBothEmpty.profiles.size)
        assertEquals(SELF_PROFILE_ID, resultBothEmpty.profiles.single().id)
    }

    @Test
    fun mergeProfiles_selfCannotBeDeletedByDeletionMarker() {
        val localSelf = ProfileEntity(
            id = SELF_PROFILE_ID,
            name = "Me",
            relationship = ProfileRelationship.SELF.name,
            createdAt = 100L,
            lastModified = 100L
        )
        val bogusSelfDeletion = ProfileDeletionEntity(
            profileId = SELF_PROFILE_ID,
            deletedAt = 9_000L
        )

        val result = SyncMerge.mergeProfiles(
            cloudProfiles = emptyList(),
            localProfiles = listOf(localSelf),
            cloudDeletions = listOf(bogusSelfDeletion),
            localDeletions = emptyList()
        )

        assertTrue(result.profiles.any { it.id == SELF_PROFILE_ID })
        assertFalse(SELF_PROFILE_ID in result.deletedProfileIds)
    }
}
