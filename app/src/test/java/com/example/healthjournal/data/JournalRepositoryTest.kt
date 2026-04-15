package com.example.healthjournal.data

import com.example.healthjournal.data.local.JournalDao
import com.example.healthjournal.data.local.JournalEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
}
