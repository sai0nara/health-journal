package com.example.healthjournal.sync

import com.example.healthjournal.data.local.EntryTagCrossRef
import com.example.healthjournal.data.local.JournalEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncMergeTest {

    private val localTags: (String) -> List<String> = { id ->
        when (id) {
            "local_id" -> listOf("ILLNESS", "DOCTOR")
            "empty_local" -> emptyList()
            else -> emptyList()
        }
    }

    @Test
    fun localWins_keepsLocalTags() {
        val cloud = JournalEntry(entry_id = "local_id", description = "Cloud", lastModified = 1000)
        val local = JournalEntry(entry_id = "local_id", description = "Local", lastModified = 2000)

        val merged = runBlocking { SyncMerge.merge(listOf(cloud), listOf(local), localTags) }

        assertEquals("Local", merged.single().description)
        assertEquals(listOf("ILLNESS", "DOCTOR"), merged.single().tags)
    }

    @Test
    fun cloudWinsWithTags_overridesLocalTags() {
        val cloud = JournalEntry(entry_id = "local_id", description = "Cloud", lastModified = 2000)
            .withTags(listOf("CHECKUP"))
        val local = JournalEntry(entry_id = "local_id", description = "Local", lastModified = 1000)

        val merged = runBlocking { SyncMerge.merge(listOf(cloud), listOf(local), localTags) }

        assertEquals("Cloud", merged.single().description)
        assertEquals(listOf("CHECKUP"), merged.single().tags)
    }

    @Test
    fun cloudWinsWithoutTags_preservesLocalTags() {
        val cloud = JournalEntry(entry_id = "local_id", description = "Cloud", lastModified = 2000)
        val local = JournalEntry(entry_id = "local_id", description = "Local", lastModified = 1000)

        val merged = runBlocking { SyncMerge.merge(listOf(cloud), listOf(local), localTags) }

        assertEquals("Cloud", merged.single().description)
        assertEquals(listOf("ILLNESS", "DOCTOR"), merged.single().tags)
    }

    @Test
    fun tie_cloudWinsWithTags() {
        val cloud = JournalEntry(entry_id = "local_id", description = "Cloud", lastModified = 1500)
            .withTags(listOf("EXERCISES"))
        val local = JournalEntry(entry_id = "local_id", description = "Local", lastModified = 1500)

        val merged = runBlocking { SyncMerge.merge(listOf(cloud), listOf(local), localTags) }

        assertEquals("Cloud", merged.single().description)
        assertEquals(listOf("EXERCISES"), merged.single().tags)
    }

    @Test
    fun cloudOnly_keepsCloudTags() {
        val cloud = JournalEntry(entry_id = "cloud_only", description = "Cloud Only", lastModified = 2000)
            .withTags(listOf("CHECKUP"))

        val merged = runBlocking { SyncMerge.merge(listOf(cloud), emptyList(), localTags) }

        assertEquals("Cloud Only", merged.single().description)
        assertEquals(listOf("CHECKUP"), merged.single().tags)
    }

    @Test
    fun canonicalTagRefs_collapsesVariantsAndDropsBlanks() {
        assertEquals(
            listOf(
                EntryTagCrossRef("e1", "fitness"),
                EntryTagCrossRef("e1", "sleep")
            ),
            SyncMerge.canonicalTagRefs(
                "e1",
                listOf("FITNESS", "fitness", "Fitness", "", "Sleep", "SLEEP")
            )
        )
    }

    @Test
    fun localOnly_keepsLocalTags() {
        val local = JournalEntry(entry_id = "local_id", description = "Local Only", lastModified = 2000)

        val merged = runBlocking { SyncMerge.merge(emptyList(), listOf(local), localTags) }

        assertEquals("Local Only", merged.single().description)
        assertEquals(listOf("ILLNESS", "DOCTOR"), merged.single().tags)
    }

    @Test
    fun merge_compositeKeyByProfileAndEntryId_preventsCrossProfileReassignment() {
        val selfEntry = JournalEntry(
            entry_id = "same_id",
            profileId = "self",
            description = "Self Entry",
            lastModified = 1_000L
        )
        val siblingEntry = JournalEntry(
            entry_id = "same_id",
            profileId = "sib-1",
            description = "Sibling Entry",
            lastModified = 2_000L
        )

        val merged = runBlocking {
            SyncMerge.merge(listOf(siblingEntry), listOf(selfEntry), localTags)
        }
        val byProfile = merged.associateBy { it.profileId }

        assertEquals(2, merged.size)
        assertEquals("Self Entry", byProfile.getValue("self").description)
        assertEquals("Sibling Entry", byProfile.getValue("sib-1").description)
    }

    @Test
    fun journalSyncPayload_missingProfileId_defaultsToSelf() {
        val legacyJson = """[{"entry_id":"e1","timestamp":1000,"description":"Legacy","lastModified":1000}]"""
        val parsed = JournalSyncPayload.fromJson(legacyJson)

        assertEquals(1, parsed.size)
        assertEquals("self", parsed.single().profileId)
    }
}
