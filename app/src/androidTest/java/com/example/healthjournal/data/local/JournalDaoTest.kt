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
        
        val results = journalDao.searchEntries("juice").first()
        assertEquals(2, results.size)
        assertTrue(results.any { it.description == "Apple juice" })
        assertTrue(results.any { it.description == "Orange juice" })
    }
}
