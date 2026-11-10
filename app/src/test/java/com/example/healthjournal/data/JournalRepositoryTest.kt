package com.example.healthjournal.data

import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.data.local.JournalDao
import com.example.healthjournal.data.local.EntryTagCrossRef
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class JournalRepositoryTest {

    private lateinit var repository: JournalRepository
    private lateinit var activeProfile: FakeActiveProfileProvider
    private val journalDao: JournalDao = mockk()

    @Before
    fun setup() {
        every { journalDao.getAllEntries(any()) } returns flowOf(emptyList())
        every { journalDao.getArchivedEntries(any()) } returns flowOf(emptyList())
        every { journalDao.observeAllTags(any()) } returns flowOf(emptyList())
        activeProfile = FakeActiveProfileProvider()
        repository = JournalRepository(journalDao, activeProfile)
    }

    @Test
    fun insertEntryCallsDao() = runBlocking {
        val entry = JournalEntry(description = "Test Entry")
        coEvery { journalDao.insertEntry(any()) } returns Unit

        repository.insert(entry)

        coVerify { journalDao.insertEntry(entry.copy(profileId = SELF_PROFILE_ID)) }
    }

    @Test
    fun insertEntryWithExplicitProfileStampsThatProfile() = runBlocking {
        val entry = JournalEntry(description = "Sibling entry")
        coEvery { journalDao.insertEntry(any()) } returns Unit

        repository.insert("sibling", entry)

        coVerify { journalDao.insertEntry(entry.copy(profileId = "sibling")) }
    }

    @Test
    fun addTagWithExplicitProfileWritesUnderThatProfile() = runBlocking {
        coEvery { journalDao.getEntryById("sibling", "entry") } returns
            JournalEntry(entry_id = "entry", description = "Sibling", profileId = "sibling")
        coEvery { journalDao.insertTag(any()) } returns Unit

        repository.addTag("sibling", "entry", "Fitness")

        coVerify { journalDao.insertTag(match { it.entryId == "entry" && it.tag == "fitness" }) }
    }

    @Test
    fun getTagsForEntryWithExplicitProfileUsesThatProfile() = runBlocking {
        coEvery { journalDao.getTagsForEntry("sibling", "entry") } returns listOf("fitness")

        val tags = repository.getTagsForEntry("sibling", "entry")

        assertEquals(listOf("fitness"), tags)
    }

    @Test
    fun clearDeletedEntriesRemovesOnlyExpiredTombstones() = runBlocking {
        val now = 1_000_000L
        val graceMs = JournalRepository.TOMBSTONE_GRACE_PERIOD_MS
        coEvery { journalDao.removeDeletedEntriesBefore(any(), any()) } returns Unit

        repository.clearDeletedEntries(now)

        coVerify { journalDao.removeDeletedEntriesBefore(SELF_PROFILE_ID, now - graceMs) }
    }

    @Test
    fun clearDeletedEntriesWithNoTimestampUsesCurrentTime() = runBlocking {
        val graceMs = JournalRepository.TOMBSTONE_GRACE_PERIOD_MS
        coEvery { journalDao.removeDeletedEntriesBefore(any(), any()) } returns Unit

        repository.clearDeletedEntries()

        coVerify {
            journalDao.removeDeletedEntriesBefore(
                SELF_PROFILE_ID,
                match { it <= System.currentTimeMillis() - graceMs + 1 }
            )
        }
    }

    @Test
    fun getEntryByIdCallsDao() = runBlocking {
        val entryId = "test_id"
        val entry = JournalEntry(entry_id = entryId, description = "Test Entry")
        coEvery { journalDao.getEntryById(SELF_PROFILE_ID, entryId) } returns entry
        coEvery { journalDao.getTagsForEntry(SELF_PROFILE_ID, entryId) } returns emptyList()

        val result = repository.getEntryById(entryId)

        assertEquals(entry.withTags(emptyList()), result)
        coVerify { journalDao.getEntryById(SELF_PROFILE_ID, entryId) }
    }

    @Test
    fun allEntriesReloadsForSwitchedProfile() = runBlocking {
        val meEntry = JournalEntry(description = "Me")
        val siblingEntry = JournalEntry(description = "Sibling")
        every { journalDao.getAllEntries(SELF_PROFILE_ID) } returns flowOf(listOf(meEntry))
        every { journalDao.getAllEntries("sibling") } returns flowOf(listOf(siblingEntry))

        assertEquals("Me", repository.allEntries.first().single().description)

        activeProfile.switchTo("sibling")

        assertEquals("Sibling", repository.allEntries.first().single().description)
    }

    @Test
    fun archivedEntriesReloadForSwitchedProfile() = runBlocking {
        val meEntry = JournalEntry(description = "Me archived", isArchived = true)
        val siblingEntry = JournalEntry(description = "Sibling archived", isArchived = true)
        every { journalDao.getArchivedEntries(SELF_PROFILE_ID) } returns flowOf(listOf(meEntry))
        every { journalDao.getArchivedEntries("sibling") } returns flowOf(listOf(siblingEntry))

        assertEquals("Me archived", repository.archivedEntries.first().single().description)

        activeProfile.switchTo("sibling")

        assertEquals("Sibling archived", repository.archivedEntries.first().single().description)
    }

    @Test
    fun allTagsReloadForSwitchedProfile() = runBlocking {
        val meTag = EntryTagCrossRef("entry-a", "me")
        val siblingTag = EntryTagCrossRef("entry-b", "sibling")
        every { journalDao.observeAllTags(SELF_PROFILE_ID) } returns flowOf(listOf(meTag))
        every { journalDao.observeAllTags("sibling") } returns flowOf(listOf(siblingTag))

        assertEquals(listOf(meTag), repository.allTags.first())

        activeProfile.switchTo("sibling")

        assertEquals(listOf(siblingTag), repository.allTags.first())
    }

    @Test
    fun searchEntriesReloadForSwitchedProfile() = runBlocking {
        val meEntry = JournalEntry(description = "Me match")
        val siblingEntry = JournalEntry(description = "Sibling match")
        every {
            journalDao.searchEntries(SELF_PROFILE_ID, "match", false)
        } returns flowOf(listOf(meEntry))
        every {
            journalDao.searchEntries("sibling", "match", false)
        } returns flowOf(listOf(siblingEntry))

        assertEquals("Me match", repository.searchEntries("match", false).first().single().description)

        activeProfile.switchTo("sibling")

        assertEquals(
            "Sibling match",
            repository.searchEntries("match", false).first().single().description
        )
    }

    @Test
    fun importAllStampsActiveProfile() = runBlocking {
        val entries = listOf(JournalEntry(description = "Test Entry"))
        coEvery { journalDao.insertAll(any()) } returns Unit

        repository.importAll(entries)

        coVerify {
            journalDao.insertAll(entries.map { it.copy(profileId = SELF_PROFILE_ID) })
        }
    }

    @Test
    fun addTagCallsDaoForActiveProfile() = runBlocking {
        val entryId = "test_id"
        val tag = "health"
        coEvery { journalDao.getEntryById(SELF_PROFILE_ID, entryId) } returns
            JournalEntry(entry_id = entryId, description = "Test")
        coEvery { journalDao.insertTag(any()) } returns Unit

        repository.addTag(entryId, tag)

        coVerify { journalDao.insertTag(match { it.entryId == entryId && it.tag == tag }) }
    }

    @Test
    fun removeTagCallsDaoForActiveProfile() = runBlocking {
        val entryId = "test_id"
        val tag = "health"
        coEvery { journalDao.deleteTag(any(), any(), any()) } returns Unit

        repository.removeTag(entryId, tag)

        coVerify { journalDao.deleteTag(SELF_PROFILE_ID, entryId, tag) }
    }

    @Test
    fun getTagsForEntryCallsDao() = runBlocking {
        val entryId = "test_id"
        val tags = listOf("TAG1", "TAG2")
        coEvery { journalDao.getTagsForEntry(SELF_PROFILE_ID, entryId) } returns tags

        val result = repository.getTagsForEntry(entryId)

        assertEquals(tags, result)
        coVerify { journalDao.getTagsForEntry(SELF_PROFILE_ID, entryId) }
    }

    @Test
    fun searchEntriesWithTagsCallsDao() = runBlocking {
        val query = "test"
        val tags = listOf("TAG1", "TAG2")
        val entries = listOf(JournalEntry(description = "Test Entry"))
        every {
            journalDao.searchEntriesWithTags(SELF_PROFILE_ID, query, tags, tags.size, any())
        } returns flowOf(entries)

        val result = repository.searchEntriesWithTags(query, tags, false).first()

        assertEquals(entries, result)
        coVerify {
            journalDao.searchEntriesWithTags(SELF_PROFILE_ID, query, tags, tags.size, false)
        }
    }

    @Test
    fun getPendingSyncEntries_delegatesToDao() = runBlocking {
        val pendingEntries = listOf(JournalEntry(description = "Pending", syncStatus = "PENDING_SYNC"))
        coEvery { journalDao.getPendingSyncEntries(SELF_PROFILE_ID) } returns pendingEntries

        val result = repository.getPendingSyncEntries()

        assertEquals(pendingEntries, result)
        coVerify { journalDao.getPendingSyncEntries(SELF_PROFILE_ID) }
    }

    @Test
    fun updateSyncStatus_delegatesToDao() = runBlocking {
        val entryId = "test_id"
        val status = "SYNCED"
        coEvery { journalDao.updateSyncStatus(SELF_PROFILE_ID, entryId, status) } returns Unit

        repository.updateSyncStatus(entryId, status)

        coVerify { journalDao.updateSyncStatus(SELF_PROFILE_ID, entryId, status) }
    }

    @Test
    fun updateAttachments_delegatesToDao() = runBlocking {
        val entryId = "test_id"
        val attachments = listOf(AttachmentData("file.pdf", "uri", "pdf"))
        val status = "SYNCED"
        coEvery { journalDao.updateAttachments(SELF_PROFILE_ID, entryId, attachments, status) } returns Unit

        repository.updateAttachments(entryId, attachments, status)

        coVerify { journalDao.updateAttachments(SELF_PROFILE_ID, entryId, attachments, status) }
    }

    @Test
    fun saveAttachmentLocally_delegatesToDao() = runBlocking {
        val entryId = "test_id"
        val entry = JournalEntry(entry_id = entryId, description = "Test", attachments = emptyList())
        val attachment = AttachmentData("file.pdf", "uri", "pdf")
        coEvery { journalDao.getEntryById(SELF_PROFILE_ID, entryId) } returns entry
        coEvery { journalDao.insertEntry(any()) } returns Unit

        repository.saveAttachmentLocally(entryId, attachment)

        coVerify {
            journalDao.insertEntry(match {
                it.entry_id == entryId &&
                    it.attachments?.contains(attachment) == true &&
                    it.syncStatus == "PENDING_SYNC"
            })
        }
    }

    @Test
    fun getVitalsEntries_queriesDaoWithActiveProfile() = runBlocking {
        val entry = JournalEntry(entry_id = "v1", description = "", bp_systolic = 120.0, bp_diastolic = 80.0)
        every { journalDao.getVitalsEntries(SELF_PROFILE_ID) } returns flowOf(listOf(entry))

        val result = repository.vitalsEntries.first()

        assertEquals(listOf(entry), result)
    }

    @Test
    fun getVitalsEntries_withExplicitProfile_queriesDao() = runBlocking {
        val entry = JournalEntry(entry_id = "v2", description = "", blood_glucose_mmol = 5.5, profileId = "child_1")
        every { journalDao.getVitalsEntries("child_1") } returns flowOf(listOf(entry))

        val result = repository.getVitalsEntries("child_1").first()

        assertEquals(listOf(entry), result)
    }
}
