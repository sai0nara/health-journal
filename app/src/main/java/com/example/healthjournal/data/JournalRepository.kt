package com.example.healthjournal.data

import com.example.healthjournal.data.local.DeletedEntry
import com.example.healthjournal.data.local.JournalDao
import com.example.healthjournal.data.local.JournalEntry
import kotlinx.coroutines.flow.Flow

class JournalRepository(private val journalDao: JournalDao) {
    val allEntries: Flow<List<JournalEntry>> = journalDao.getAllEntries()
    val archivedEntries: Flow<List<JournalEntry>> = journalDao.getArchivedEntries()

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

    suspend fun archiveEntry(entryId: String) {
        journalDao.updateArchiveStatus(entryId, true, System.currentTimeMillis())
    }

    suspend fun restoreEntry(entryId: String) {
        journalDao.updateArchiveStatus(entryId, false, System.currentTimeMillis())
    }

    suspend fun deleteEntries(entryIds: List<String>) {
        entryIds.forEach { id ->
            journalDao.insertDeletedEntry(DeletedEntry(id))
        }
        journalDao.deleteEntriesByIds(entryIds)
    }

    suspend fun deleteAllArchived() {
        // Record all archived IDs for sync before deleting
        val archivedIds = journalDao.getArchivedEntriesIds()
        archivedIds.forEach { id ->
            journalDao.insertDeletedEntry(DeletedEntry(id))
        }
        journalDao.deleteAllArchivedEntries()
    }
    
    suspend fun getDeletedEntryIds(): List<String> {
        return journalDao.getAllDeletedEntries().map { it.entry_id }
    }
    
    suspend fun clearDeletedEntries(entryIds: List<String>) {
        journalDao.removeDeletedEntries(entryIds)
    }
}
