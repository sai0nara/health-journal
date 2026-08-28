package com.example.healthjournal.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class JournalDaoTest {

    private lateinit var journalDao: JournalDao
    private lateinit var db: JournalDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, JournalDatabase::class.java
        ).build()
        journalDao = db.journalDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeEntryAndReadInList() = runBlocking {
        val entry = JournalEntry(description = "Morning jog")
        journalDao.insertEntry(entry)
        val allEntries = journalDao.getAllEntries().first()
        assertEquals(allEntries[0].description, entry.description)
    }

    @Test
    @Throws(Exception::class)
    fun getEntryByIdReturnsNullForNonExistent() = runBlocking {
        val entry = journalDao.getEntryById("non_existent_id")
        assertNull(entry)
    }

    @Test
    fun deleteEntriesByIdsRemovesTagCrossRefs() = runBlocking {
        val entry1 = JournalEntry(description = "Kept")
        val entry2 = JournalEntry(description = "Deleted")
        journalDao.insertEntry(entry1)
        journalDao.insertEntry(entry2)
        journalDao.insertTag(EntryTagCrossRef(entry1.entry_id, "RUN"))
        journalDao.insertTag(EntryTagCrossRef(entry2.entry_id, "GYM"))

        journalDao.deleteAllTagsForEntries(listOf(entry2.entry_id))
        journalDao.deleteEntriesByIds(listOf(entry2.entry_id))

        assertEquals(listOf("RUN"), journalDao.getTagsForEntry(entry1.entry_id))
        assertTrue(journalDao.getTagsForEntry(entry2.entry_id).isEmpty())
    }

    @Test
    fun getEntriesSortedByDateAsc() = runBlocking {
        val entry1 = JournalEntry(description = "First", timestamp = 1000)
        val entry2 = JournalEntry(description = "Second", timestamp = 2000)
        journalDao.insertEntry(entry2)
        journalDao.insertEntry(entry1)
        
        val entries = journalDao.getEntriesSortedByDate(isAsc = true).first()
        assertEquals("First", entries[0].description)
        assertEquals("Second", entries[1].description)
    }

    @Test
    fun getEntriesSortedByDateDesc() = runBlocking {
        val entry1 = JournalEntry(description = "First", timestamp = 1000)
        val entry2 = JournalEntry(description = "Second", timestamp = 2000)
        journalDao.insertEntry(entry1)
        journalDao.insertEntry(entry2)
        
        val entries = journalDao.getEntriesSortedByDate(isAsc = false).first()
        assertEquals("Second", entries[0].description)
        assertEquals("First", entries[1].description)
    }

    @Test
    fun searchEntriesByKeyword() = runBlocking {
        val entry1 = JournalEntry(description = "Apple juice")
        val entry2 = JournalEntry(description = "Orange juice")
        val entry3 = JournalEntry(description = "Water")
        journalDao.insertEntry(entry1)
        journalDao.insertEntry(entry2)
        journalDao.insertEntry(entry3)
        
        val results = journalDao.searchEntries("juice", isAsc = false).first()
        assertEquals(2, results.size)
        assertTrue(results.any { it.description == "Apple juice" })
        assertTrue(results.any { it.description == "Orange juice" })
    }

    @Test
    fun testTypeConverters_HandlesNullAndMalformedInput() {
        val converters = JournalTypeConverters()
        
        // String List
        assertEquals(emptyList<String>(), converters.toStringList(null))
        assertEquals(emptyList<String>(), converters.toStringList("invalid json"))
        
        // Attachment List
        assertEquals(emptyList<AttachmentData>(), converters.toAttachmentList(null))
        assertEquals(emptyList<AttachmentData>(), converters.toAttachmentList("invalid json"))
    }

    @Test
    fun getPendingSyncEntries_returnsOnlyPendingEntries() = runBlocking {
        val entry1 = JournalEntry(description = "Pending", syncStatus = "PENDING_SYNC")
        val entry2 = JournalEntry(description = "Synced", syncStatus = "SYNCED")
        
        journalDao.insertEntry(entry1)
        journalDao.insertEntry(entry2)
        
        val pending = journalDao.getPendingSyncEntries()
        assertEquals(1, pending.size)
        assertEquals("Pending", pending[0].description)
        assertEquals("PENDING_SYNC", pending[0].syncStatus)
    }

    @Test
    fun updateSyncStatus_correctlyUpdatesStatus() = runBlocking {
        val entry = JournalEntry(description = "Test", syncStatus = "PENDING_SYNC")
        journalDao.insertEntry(entry)
        
        journalDao.updateSyncStatus(entry.entry_id, "SYNCING")
        
        val updated = journalDao.getEntryById(entry.entry_id)
        assertNotNull(updated)
        assertEquals("SYNCING", updated!!.syncStatus)
    }

    @Test
    fun updateAttachments_correctlyUpdatesAttachmentsAndStatus() = runBlocking {
        val entry = JournalEntry(description = "Test", syncStatus = "PENDING_SYNC", attachments = emptyList())
        journalDao.insertEntry(entry)
        
        val newAttachments = listOf(
            AttachmentData(name = "report.pdf", uri = "file:///path/to/report.pdf", mimeType = "application/pdf")
        )
        
        journalDao.updateAttachments(entry.entry_id, newAttachments, "SYNCED")
        
        val updated = journalDao.getEntryById(entry.entry_id)
        assertNotNull(updated)
        assertEquals("SYNCED", updated!!.syncStatus)
        assertNotNull(updated.attachments)
        assertEquals(1, updated.attachments!!.size)
        assertEquals("report.pdf", updated.attachments!![0].name)
    }

    @Test
    fun searchEntriesWithTags_FiltersCorrectly() = runBlocking {
        val entry1 = JournalEntry(description = "Blood pressure checkup")
        val entry2 = JournalEntry(description = "Morning exercise")
        val entry3 = JournalEntry(description = "Doctor visit for fever")
        journalDao.insertEntry(entry1)
        journalDao.insertEntry(entry2)
        journalDao.insertEntry(entry3)

        journalDao.insertTag(EntryTagCrossRef(entry1.entry_id, "CHECKUP"))
        journalDao.insertTag(EntryTagCrossRef(entry3.entry_id, "CHECKUP"))
        journalDao.insertTag(EntryTagCrossRef(entry3.entry_id, "DOCTOR"))

        // Search for "checkup" with tag "CHECKUP" -> should find entry1 only
        // (entry3 has the CHECKUP tag but its description does not contain "checkup")
        val results = journalDao.searchEntriesWithTags(
            query = "checkup", 
            tags = listOf("CHECKUP"), 
            tagCount = 1, 
            isAsc = false
        ).first()
        
        assertEquals(1, results.size)
        assertTrue(results.any { it.description == "Blood pressure checkup" })
        assertFalse(results.any { it.description == "Doctor visit for fever" })
    }

    @Test
    fun searchEntriesWithTags_RequiresAllTags() = runBlocking {
        val entry1 = JournalEntry(description = "Checkup with Dr. Smith")
        val entry2 = JournalEntry(description = "General Checkup")
        journalDao.insertEntry(entry1)
        journalDao.insertEntry(entry2)

        journalDao.insertTag(EntryTagCrossRef(entry1.entry_id, "CHECKUP"))
        journalDao.insertTag(EntryTagCrossRef(entry1.entry_id, "DOCTOR"))
        journalDao.insertTag(EntryTagCrossRef(entry2.entry_id, "CHECKUP"))

        // Search for entries that have BOTH CHECKUP and DOCTOR
        val results = journalDao.searchEntriesWithTags(
            query = "", 
            tags = listOf("CHECKUP", "DOCTOR"), 
            tagCount = 2, 
            isAsc = false
        ).first()
        
        assertEquals(1, results.size)
        assertEquals("Checkup with Dr. Smith", results[0].description)
    }

    @Test
    fun tagManagement_InsertDeleteRead() = runBlocking {
        val entry = JournalEntry(description = "Tagging test")
        journalDao.insertEntry(entry)

        // Test Insert
        journalDao.insertTag(EntryTagCrossRef(entry.entry_id, "TEST_TAG"))
        var tags = journalDao.getTagsForEntry(entry.entry_id)
        assertEquals(1, tags.size)
        assertEquals("TEST_TAG", tags[0])

        // Test Delete
        journalDao.deleteTag(entry.entry_id, "TEST_TAG")
        tags = journalDao.getTagsForEntry(entry.entry_id)
        assertTrue(tags.isEmpty())
    }

    @Test
    fun getAllTags_returnsAllTagCrossRefs() = runBlocking {
        val entry1 = JournalEntry(description = "A")
        val entry2 = JournalEntry(description = "B")
        journalDao.insertEntry(entry1)
        journalDao.insertEntry(entry2)
        journalDao.insertTag(EntryTagCrossRef(entry1.entry_id, "RUN"))
        journalDao.insertTag(EntryTagCrossRef(entry1.entry_id, "GYM"))
        journalDao.insertTag(EntryTagCrossRef(entry2.entry_id, "SWIM"))

        val all = journalDao.getAllTags()
        assertEquals(3, all.size)
        assertTrue(all.any { it.entryId == entry1.entry_id && it.tag == "RUN" })
        assertTrue(all.any { it.entryId == entry1.entry_id && it.tag == "GYM" })
        assertTrue(all.any { it.entryId == entry2.entry_id && it.tag == "SWIM" })
    }
}
