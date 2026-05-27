package com.example.healthjournal.data

import com.example.healthjournal.data.local.JournalDao
import com.example.healthjournal.data.local.JournalEntry
import kotlinx.coroutines.flow.Flow

class JournalRepository(private val journalDao: JournalDao) {
    val allEntries: Flow<List<JournalEntry>> = journalDao.getAllEntries()

    fun getEntriesSortedByDate(isAsc: Boolean): Flow<List<JournalEntry>> {
        return journalDao.getEntriesSortedByDate(isAsc)
    }

    fun searchEntries(query: String, isAsc: Boolean): Flow<List<JournalEntry>> {
        return journalDao.searchEntries(query, isAsc)
    }

    suspend fun insert(entry: JournalEntry) {
        journalDao.insertEntry(entry)
    }

    suspend fun getEntryById(entryId: String): JournalEntry? {
        return journalDao.getEntryById(entryId)
    }

    suspend fun importAll(entries: List<JournalEntry>) {
        journalDao.insertAll(entries)
    }
}
