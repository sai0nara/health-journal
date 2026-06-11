package com.example.healthjournal.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class AttachmentSchemaTest {

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
    fun testAttachmentsWithSyncStatusAndRemoteUrl() = runBlocking {
        // This should fail to compile initially because AttachmentData doesn't have these fields
        val attachment = AttachmentData(
            name = "report.pdf",
            uri = "file:///local/path",
            mimeType = "application/pdf",
            isLocalOnly = false,
            remoteUrl = "https://cloud.com/report.pdf",
            syncStatus = "SYNCED" // Using string for now to avoid enum dependency if it doesn't exist
        )
        val entry = JournalEntry(
            description = "Health report",
            attachments = listOf(attachment)
        )
        journalDao.insertEntry(entry)
        val retrieved = journalDao.getEntryById(entry.entry_id)
        assertNotNull(retrieved)
        assertEquals(1, retrieved!!.attachments?.size)
        val retrievedAttachment = retrieved.attachments!![0]
        assertEquals("SYNCED", retrievedAttachment.syncStatus)
        assertEquals("https://cloud.com/report.pdf", retrievedAttachment.remoteUrl)
    }
}
