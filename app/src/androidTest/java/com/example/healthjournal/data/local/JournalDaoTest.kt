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
    @Throws(Exception::class)
    fun getEntryByIdReturnsCorrectEntry() = runBlocking {
        val entry = JournalEntry(description = "Afternoon yoga")
        journalDao.insertEntry(entry)
        val fetchedEntry = journalDao.getEntryById(entry.entry_id)
        assertNotNull(fetchedEntry)
        assertEquals(fetchedEntry?.description, entry.description)
    }
}
