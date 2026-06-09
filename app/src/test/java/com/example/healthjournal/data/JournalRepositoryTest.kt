package com.example.healthjournal.data

import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.data.local.JournalDao
import com.example.healthjournal.data.local.JournalEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    // ---- New tests for sync and attachment functionality ----

    @Test
    fun getPendingSyncEntries_delegatesToDao() = runBlocking {
        val pendingEntries = listOf(
            JournalEntry(description = "Pending", syncStatus = "PENDING_SYNC")
        )
        coEvery { journalDao.getPendingSyncEntries() } returns pendingEntries

        val result = repository.getPendingSyncEntries()

        assertEquals(pendingEntries, result)
        coVerify { journalDao.getPendingSyncEntries() }
    }

    @Test
    fun updateSyncStatus_delegatesToDao() = runBlocking {
        coEvery { journalDao.updateSyncStatus(any(), any()) } returns Unit

        repository.updateSyncStatus("entry-1", "SYNCED")

        coVerify { journalDao.updateSyncStatus("entry-1", "SYNCED") }
    }

    @Test
    fun updateAttachments_delegatesToDao() = runBlocking {
        val attachments = listOf(
            AttachmentData("photo.jpg", "https://cdn.com/photo.jpg", "image/jpeg", false)
        )
        coEvery { journalDao.updateAttachments(any(), any(), any()) } returns Unit

        repository.updateAttachments("entry-1", attachments, "SYNCED")

        coVerify { journalDao.updateAttachments("entry-1", attachments, "SYNCED") }
    }

    @Test
    fun saveAttachmentLocally_updatesEntryWithNewAttachment() = runBlocking {
        val existingEntry = JournalEntry(
            entry_id = "entry-1",
            description = "Test",
            attachments = listOf(
                AttachmentData("old.jpg", "/data/old.jpg", "image/jpeg", true)
            ),
            syncStatus = "SYNCED"
        )
        val newAttachment = AttachmentData(
            name = "new.pdf",
            uri = "/data/new.pdf",
            mimeType = "application/pdf",
            isLocalOnly = true
        )

        coEvery { journalDao.getEntryById("entry-1") } returns existingEntry
        coEvery { journalDao.insertEntry(any()) } returns Unit

        repository.saveAttachmentLocally("entry-1", newAttachment)

        coVerify {
            journalDao.insertEntry(match {
                it.entry_id == "entry-1" &&
                it.attachments.size == 2 &&
                it.attachments[1].name == "new.pdf" &&
                it.syncStatus == "PENDING_SYNC"
            })
        }
    }

    @Test
    fun saveAttachmentLocally_createsNewListIfEntryHasNoAttachments() = runBlocking {
        val existingEntry = JournalEntry(
            entry_id = "entry-2",
            description = "No attachments",
            attachments = emptyList(),
            syncStatus = "SYNCED"
        )
        val newAttachment = AttachmentData(
            name = "doc.pdf",
            uri = "/data/doc.pdf",
            mimeType = "application/pdf",
            isLocalOnly = true
        )

        coEvery { journalDao.getEntryById("entry-2") } returns existingEntry
        coEvery { journalDao.insertEntry(any()) } returns Unit

        repository.saveAttachmentLocally("entry-2", newAttachment)

        coVerify {
            journalDao.insertEntry(match {
                it.entry_id == "entry-2" &&
                it.attachments.size == 1 &&
                it.attachments[0].name == "doc.pdf" &&
                it.syncStatus == "PENDING_SYNC"
            })
        }
    }
}

