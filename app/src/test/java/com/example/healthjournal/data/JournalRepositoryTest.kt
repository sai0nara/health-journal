package com.example.healthjournal.data

import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.data.local.JournalDao
import com.example.healthjournal.data.local.JournalEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class JournalRepositoryTest {

    private lateinit var repository: JournalRepository
    private val journalDao: JournalDao = mockk()

    @Before
    fun setup() {
        coEvery { journalDao.getAllEntries() } returns flowOf(emptyList())
        coEvery { journalDao.getArchivedEntries() } returns flowOf(emptyList())
        repository = JournalRepository(journalDao)
    }

    @Test
    fun insertEntryCallsDao() = runBlocking {
        val entry = JournalEntry(description = "Test Entry")
        coEvery { journalDao.insertEntry(any()) } returns Unit
        
        repository.insert(entry)
        
        coVerify { journalDao.insertEntry(entry) }
    }

    @Test
    fun clearDeletedEntriesRemovesOnlyExpiredTombstones() = runBlocking {
        val now = 1_000_000L
        val graceMs = JournalRepository.TOMBSTONE_GRACE_PERIOD_MS
        coEvery { journalDao.removeDeletedEntriesBefore(any()) } returns Unit

        repository.clearDeletedEntries(now)

        coVerify { journalDao.removeDeletedEntriesBefore(now - graceMs) }
    }

    @Test
    fun clearDeletedEntriesWithNoTimestampUsesCurrentTime() = runBlocking {
        val graceMs = JournalRepository.TOMBSTONE_GRACE_PERIOD_MS
        coEvery { journalDao.removeDeletedEntriesBefore(any()) } returns Unit

        repository.clearDeletedEntries()

        coVerify { journalDao.removeDeletedEntriesBefore(match { it <= System.currentTimeMillis() - graceMs + 1 }) }
    }

    @Test
    fun getEntryByIdCallsDao() = runBlocking {
        val entryId = "test_id"
        val entry = JournalEntry(entry_id = entryId, description = "Test Entry")
        coEvery { journalDao.getEntryById(entryId) } returns entry
        
        val result = repository.getEntryById(entryId)
        
        assertEquals(entry, result)
        coVerify { journalDao.getEntryById(entryId) }
    }

    @Test
    fun allEntriesReturnsFlowFromDao() = runBlocking {
        val entries = listOf(JournalEntry(description = "Test Entry"))
        coEvery { journalDao.getAllEntries() } returns flowOf(entries)
        
        // Recreate repository to pick up the new mock flow
        val testRepository = JournalRepository(journalDao)
        
        testRepository.allEntries.collect {
            assertEquals(entries, it)
        }
    }

    @Test
    fun importAllCallsDao() = runBlocking {
        val entries = listOf(JournalEntry(description = "Test Entry"))
        coEvery { journalDao.insertAll(any()) } returns Unit
        
        repository.importAll(entries)
        
        coVerify { journalDao.insertAll(entries) }
    }

    @Test
    fun addTagCallsDao() = runBlocking {
        val entryId = "test_id"
        val tag = "HEALTH"
        coEvery { journalDao.insertTag(any()) } returns Unit
        
        repository.addTag(entryId, tag)
        
        coVerify { journalDao.insertTag(match { it.entryId == entryId && it.tag == tag }) }
    }

    @Test
    fun removeTagCallsDao() = runBlocking {
        val entryId = "test_id"
        val tag = "HEALTH"
        coEvery { journalDao.deleteTag(any(), any()) } returns Unit
        
        repository.removeTag(entryId, tag)
        
        coVerify { journalDao.deleteTag(entryId, tag) }
    }

    @Test
    fun getTagsForEntryCallsDao() = runBlocking {
        val entryId = "test_id"
        val tags = listOf("TAG1", "TAG2")
        coEvery { journalDao.getTagsForEntry(entryId) } returns tags
        
        val result = repository.getTagsForEntry(entryId)
        
        assertEquals(tags, result)
        coVerify { journalDao.getTagsForEntry(entryId) }
    }

    @Test
    fun searchEntriesWithTagsCallsDao() = runBlocking {
        val query = "test"
        val tags = listOf("TAG1", "TAG2")
        val entries = listOf(JournalEntry(description = "Test Entry"))
        coEvery { journalDao.searchEntriesWithTags(query, tags, tags.size, any()) } returns flowOf(entries)
        
        val result = repository.searchEntriesWithTags(query, tags, false).first()
        
        assertEquals(entries, result)
        coVerify { journalDao.searchEntriesWithTags(query, tags, tags.size, false) }
    }

    @Test
    fun getPendingSyncEntries_delegatesToDao() = runBlocking {
        val pendingEntries = listOf(JournalEntry(description = "Pending", syncStatus = "PENDING_SYNC"))
        coEvery { journalDao.getPendingSyncEntries() } returns pendingEntries
        
        val result = repository.getPendingSyncEntries()
        
        assertEquals(pendingEntries, result)
        coVerify { journalDao.getPendingSyncEntries() }
    }

    @Test
    fun updateSyncStatus_delegatesToDao() = runBlocking {
        val entryId = "test_id"
        val status = "SYNCED"
        coEvery { journalDao.updateSyncStatus(entryId, status) } returns Unit
        
        repository.updateSyncStatus(entryId, status)
        
        coVerify { journalDao.updateSyncStatus(entryId, status) }
    }

    @Test
    fun updateAttachments_delegatesToDao() = runBlocking {
        val entryId = "test_id"
        val attachments = listOf(AttachmentData("file.pdf", "uri", "pdf"))
        val status = "SYNCED"
        coEvery { journalDao.updateAttachments(entryId, attachments, status) } returns Unit
        
        repository.updateAttachments(entryId, attachments, status)
        
        coVerify { journalDao.updateAttachments(entryId, attachments, status) }
    }

    @Test
    fun saveAttachmentLocally_delegatesToDao() = runBlocking {
        val entryId = "test_id"
        val entry = JournalEntry(entry_id = entryId, description = "Test", attachments = emptyList())
        val attachment = AttachmentData("file.pdf", "uri", "pdf")
        coEvery { journalDao.getEntryById(entryId) } returns entry
        coEvery { journalDao.insertEntry(any()) } returns Unit
        
        repository.saveAttachmentLocally(entryId, attachment)
        
        coVerify { journalDao.insertEntry(match { 
            it.entry_id == entryId && 
            it.attachments?.contains(attachment) == true &&
            it.syncStatus == "PENDING_SYNC" 
        }) }
    }
}
