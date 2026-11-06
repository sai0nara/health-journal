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
        val allEntries = journalDao.getAllEntries(SELF_PROFILE_ID).first()
        assertEquals(allEntries[0].description, entry.description)
    }

    @Test
    fun writeEntryWithVitals_roundTripsCanonicalValues() = runBlocking {
        val entry = JournalEntry(
            description = "Vitals",
            bp_systolic = 120.0,
            bp_diastolic = 80.0,
            heart_rate_avg = 72,
            blood_glucose_mmol = 5.4,
            body_temperature_c = 36.6,
            spo2_percent = 98
        )

        journalDao.insertEntry(entry)
        val loaded = journalDao.getEntryById(SELF_PROFILE_ID, entry.entry_id)!!

        assertEquals(120.0, loaded.bp_systolic!!, 0.001)
        assertEquals(80.0, loaded.bp_diastolic!!, 0.001)
        assertEquals(72, loaded.heart_rate_avg)
        assertEquals(5.4, loaded.blood_glucose_mmol!!, 0.001)
        assertEquals(36.6, loaded.body_temperature_c!!, 0.001)
        assertEquals(98, loaded.spo2_percent)
    }

    @Test
    fun sameAttachmentNameIsScopedToItsProfileEntry() = runBlocking {
        val meEntry = JournalEntry(
            description = "Me",
            profileId = SELF_PROFILE_ID,
            attachments = listOf(
                AttachmentData(
                    name = "report.pdf",
                    uri = "file:///me/report.pdf",
                    mimeType = "application/pdf"
                )
            )
        )
        val siblingEntry = JournalEntry(
            description = "Sibling",
            profileId = "sibling",
            attachments = listOf(
                AttachmentData(
                    name = "report.pdf",
                    uri = "file:///sibling/report.pdf",
                    mimeType = "application/pdf"
                )
            )
        )

        journalDao.insertEntry(meEntry)
        journalDao.insertEntry(siblingEntry)

        val loadedMe = journalDao.getEntryById(SELF_PROFILE_ID, meEntry.entry_id)!!
        val loadedSibling = journalDao.getEntryById("sibling", siblingEntry.entry_id)!!
        assertEquals("file:///me/report.pdf", loadedMe.attachments!!.single().uri)
        assertEquals("file:///sibling/report.pdf", loadedSibling.attachments!!.single().uri)
        assertEquals(1, journalDao.getAllEntries(SELF_PROFILE_ID).first().size)
        assertEquals(1, journalDao.getAllEntries("sibling").first().size)
    }

    @Test
    @Throws(Exception::class)
    fun getEntryByIdReturnsNullForNonExistent() = runBlocking {
        val entry = journalDao.getEntryById(SELF_PROFILE_ID, "non_existent_id")
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

        journalDao.deleteAllTagsForEntries(SELF_PROFILE_ID, listOf(entry2.entry_id))
        journalDao.deleteEntriesByIds(SELF_PROFILE_ID, listOf(entry2.entry_id))

        assertEquals(listOf("RUN"), journalDao.getTagsForEntry(SELF_PROFILE_ID, entry1.entry_id))
        assertTrue(journalDao.getTagsForEntry(SELF_PROFILE_ID, entry2.entry_id).isEmpty())
    }

    @Test
    fun getEntriesSortedByDateAsc() = runBlocking {
        val entry1 = JournalEntry(description = "First", timestamp = 1000)
        val entry2 = JournalEntry(description = "Second", timestamp = 2000)
        journalDao.insertEntry(entry2)
        journalDao.insertEntry(entry1)
        
        val entries = journalDao.getEntriesSortedByDate(SELF_PROFILE_ID, isAsc = true).first()
        assertEquals("First", entries[0].description)
        assertEquals("Second", entries[1].description)
    }

    @Test
    fun getEntriesSortedByDateDesc() = runBlocking {
        val entry1 = JournalEntry(description = "First", timestamp = 1000)
        val entry2 = JournalEntry(description = "Second", timestamp = 2000)
        journalDao.insertEntry(entry1)
        journalDao.insertEntry(entry2)
        
        val entries = journalDao.getEntriesSortedByDate(SELF_PROFILE_ID, isAsc = false).first()
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
        
        val results = journalDao.searchEntries(SELF_PROFILE_ID, "juice", isAsc = false).first()
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
        
        val pending = journalDao.getPendingSyncEntries(SELF_PROFILE_ID)
        assertEquals(1, pending.size)
        assertEquals("Pending", pending[0].description)
        assertEquals("PENDING_SYNC", pending[0].syncStatus)
    }

    @Test
    fun updateSyncStatus_correctlyUpdatesStatus() = runBlocking {
        val entry = JournalEntry(description = "Test", syncStatus = "PENDING_SYNC")
        journalDao.insertEntry(entry)
        
        journalDao.updateSyncStatus(SELF_PROFILE_ID, entry.entry_id, "SYNCING")
        
        val updated = journalDao.getEntryById(SELF_PROFILE_ID, entry.entry_id)
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
        
        journalDao.updateAttachments(SELF_PROFILE_ID, entry.entry_id, newAttachments, "SYNCED")
        
        val updated = journalDao.getEntryById(SELF_PROFILE_ID, entry.entry_id)
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
            SELF_PROFILE_ID,
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
            SELF_PROFILE_ID,
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
        var tags = journalDao.getTagsForEntry(SELF_PROFILE_ID, entry.entry_id)
        assertEquals(1, tags.size)
        assertEquals("TEST_TAG", tags[0])

        // Test Delete
        journalDao.deleteTag(SELF_PROFILE_ID, entry.entry_id, "TEST_TAG")
        tags = journalDao.getTagsForEntry(SELF_PROFILE_ID, entry.entry_id)
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

        val all = journalDao.getAllTags(SELF_PROFILE_ID)
        assertEquals(3, all.size)
        assertTrue(all.any { it.entryId == entry1.entry_id && it.tag == "RUN" })
        assertTrue(all.any { it.entryId == entry1.entry_id && it.tag == "GYM" })
        assertTrue(all.any { it.entryId == entry2.entry_id && it.tag == "SWIM" })
    }

    @Test
    fun profileScopedReadsWritesTagsAndTombstonesStayIsolated() = runBlocking {
        val me = JournalEntry(entry_id = "me-entry", description = "Mine")
        val sibling = JournalEntry(entry_id = "sib-entry", description = "Theirs", profileId = "sibling")
        journalDao.insertEntry(me)
        journalDao.insertEntry(sibling)

        assertEquals(listOf("Mine"), journalDao.getAllEntries(SELF_PROFILE_ID).first().map { it.description })
        assertEquals(listOf("Theirs"), journalDao.getAllEntries("sibling").first().map { it.description })
        assertNull(journalDao.getEntryById(SELF_PROFILE_ID, sibling.entry_id))
        assertNull(journalDao.getEntryById("sibling", me.entry_id))

        journalDao.insertTag(EntryTagCrossRef(me.entry_id, "MINE"))
        journalDao.insertTag(EntryTagCrossRef(sibling.entry_id, "THEIRS"))
        assertEquals(listOf("MINE"), journalDao.getTagsForEntry(SELF_PROFILE_ID, me.entry_id))
        assertTrue(journalDao.getTagsForEntry(SELF_PROFILE_ID, sibling.entry_id).isEmpty())
        assertEquals(1, journalDao.getAllTags(SELF_PROFILE_ID).size)

        journalDao.insertDeletedEntry(DeletedEntry("me-del", deletedAt = 10L))
        journalDao.insertDeletedEntry(DeletedEntry("sib-del", deletedAt = 20L, profileId = "sibling"))
        assertEquals(listOf("me-del"), journalDao.getAllDeletedEntries(SELF_PROFILE_ID).map { it.entry_id })

        journalDao.deleteEntriesByIds(SELF_PROFILE_ID, listOf(sibling.entry_id))
        assertEquals(1, journalDao.getAllEntries("sibling").first().size)
        journalDao.clearAllTags(SELF_PROFILE_ID)
        assertTrue(journalDao.getAllTags("sibling").isNotEmpty())
        journalDao.clearAllEntries(SELF_PROFILE_ID)
        journalDao.clearAllDeletedEntries(SELF_PROFILE_ID)
        assertTrue(journalDao.getAllEntries(SELF_PROFILE_ID).first().isEmpty())
        assertTrue(journalDao.getAllDeletedEntries(SELF_PROFILE_ID).isEmpty())
        assertEquals(1, journalDao.getAllEntries("sibling").first().size)
        assertEquals(1, journalDao.getAllDeletedEntries("sibling").size)
    }
}
