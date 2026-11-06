package com.example.healthjournal.sync

import com.example.healthjournal.data.local.Demographics
import com.example.healthjournal.data.local.PersonalCard
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import org.junit.Assert.assertEquals
import org.junit.Test

class PersonalCardSyncMergeTest {

    @Test
    fun mergePersonalCard_compositeKeyByProfileAndId_doesNotCrossReassign() {
        val selfCloud = PersonalCard(
            id = "personal_card",
            profileId = SELF_PROFILE_ID,
            lastModified = 2_000L,
            demographics = Demographics(fullName = "Self Cloud")
        )
        val selfLocal = PersonalCard(
            id = "personal_card",
            profileId = SELF_PROFILE_ID,
            lastModified = 1_000L,
            demographics = Demographics(fullName = "Self Local")
        )
        val sibCloud = PersonalCard(
            id = "personal_card",
            profileId = "sib-1",
            lastModified = 1_000L,
            demographics = Demographics(fullName = "Sib Cloud")
        )
        val sibLocal = PersonalCard(
            id = "personal_card",
            profileId = "sib-1",
            lastModified = 3_000L,
            demographics = Demographics(fullName = "Sib Local")
        )

        val merged = SyncMerge.mergePersonalCard(
            cloudCards = listOf(selfCloud, sibCloud),
            localCards = listOf(selfLocal, sibLocal)
        )
        val byProfile = merged.associateBy { it.profileId }

        assertEquals(2, merged.size)
        assertEquals("Self Cloud", byProfile.getValue(SELF_PROFILE_ID).demographics.fullName)
        assertEquals("Sib Local", byProfile.getValue("sib-1").demographics.fullName)
    }
}
