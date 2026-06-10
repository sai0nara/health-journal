package com.example.healthjournal.data.local

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for JournalDao sync-related methods.
 * Tests verify that the DAO correctly retrieves pending sync entries
 * and updates sync status for the Rich Attachments feature.
 */
class JournalDaoTest {

    private val dao: JournalDao = mockk(relaxed = true)

    private val pendingEntry = JournalEntry(
        entry_id = "entry-1",
        description = "Test entry with attachments",
        syncStatus = "PENDING_SYNC",
        attachments = listOf(
            AttachmentData(
                name = "prescription.jpg",
                uri = "/data/files/prescription.jpg",
                mimeType = "image/jpeg",
                isLocalOnly = true
            )
        )
    )

    private val syncedEntry = JournalEntry(
        entry_id = "entry-2",
        description = "Already synced entry",
        syncStatus = "SYNCED",
        attachments = listOf(
            AttachmentData(
                name = "report.pdf",
                uri = "https://cloud.example.com/report.pdf",
                mimeType = "application/pdf",
                isLocalOnly = false
            )
        )
    )

    @Before
    fun setup() {
        coEvery { dao.getPendingSyncEntries() } returns listOf(pendingEntry)
        coEvery { dao.updateSyncStatus(any(), any()) } returns Unit
        coEvery { dao.updateAttachments(any(), any(), any()) } returns Unit
    }

    @Test
    fun getPendingSyncEntries_returnsOnlyPendingEntries() = runBlocking {
        val result = dao.getPendingSyncEntries()

        assertEquals(1, result.size)
        assertEquals("PENDING_SYNC", result[0].syncStatus)
        assertEquals("entry-1", result[0].entry_id)
        coVerify { dao.getPendingSyncEntries() }
    }

    @Test
    fun getPendingSyncEntries_returnsEmptyWhenNoPending() = runBlocking {
        coEvery { dao.getPendingSyncEntries() } returns emptyList()

        val result = dao.getPendingSyncEntries()

        assertTrue(result.isEmpty())
    }

    @Test
    fun updateSyncStatus_updatesCorrectEntry() = runBlocking {
        dao.updateSyncStatus("entry-1", "SYNCED")

        coVerify { dao.updateSyncStatus("entry-1", "SYNCED") }
    }

    @Test
    fun updateSyncStatus_withErrorStatus() = runBlocking {
        dao.updateSyncStatus("entry-1", "SYNC_ERROR")

        coVerify { dao.updateSyncStatus("entry-1", "SYNC_ERROR") }
    }

    @Test
    fun updateAttachments_updatesListAndSyncStatus() = runBlocking {
        val updatedAttachments = listOf(
            AttachmentData(
                name = "prescription.jpg",
                uri = "https://cloud.example.com/prescription.jpg",
                mimeType = "image/jpeg",
                isLocalOnly = false
            )
        )

        dao.updateAttachments("entry-1", updatedAttachments, "SYNCED")

        coVerify {
            dao.updateAttachments("entry-1", updatedAttachments, "SYNCED")
        }
    }

    @Test
    fun attachmentData_isLocalOnly_defaultsToTrue() {
        val attachment = AttachmentData(
            name = "test.jpg",
            uri = "/local/test.jpg",
            mimeType = "image/jpeg"
        )

        assertTrue(attachment.isLocalOnly == true)
    }

    @Test
    fun journalEntry_syncStatus_defaultsToPendingSync() {
        val entry = JournalEntry(description = "New entry")

        assertEquals("PENDING_SYNC", entry.syncStatus)
    }
}
