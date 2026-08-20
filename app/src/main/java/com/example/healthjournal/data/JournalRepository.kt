package com.example.healthjournal.data

import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.data.local.DeletedEntry
import com.example.healthjournal.data.local.EntryTagCrossRef
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

    fun searchEntriesWithTags(query: String, tags: List<String>, isAsc: Boolean): Flow<List<JournalEntry>> {
        return journalDao.searchEntriesWithTags(query, tags, tags.size, isAsc)
    }

    fun searchArchivedEntries(query: String): Flow<List<JournalEntry>> {
        return journalDao.searchArchivedEntries(query)
    }

    fun searchArchivedEntriesWithTags(query: String, tags: List<String>): Flow<List<JournalEntry>> {
        return journalDao.searchArchivedEntriesWithTags(query, tags, tags.size)
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

    /**
     * Returns all entries with a syncStatus of PENDING_SYNC.
     * Used by the PeriodicSyncWorker to identify entries that need uploading.
     */
    suspend fun getPendingSyncEntries(): List<JournalEntry> {
        return journalDao.getPendingSyncEntries()
    }

    /**
     * Updates the sync status for a specific entry.
     * @param entryId The entry to update.
     * @param syncStatus One of: PENDING_SYNC, SYNCING, SYNCED, SYNC_ERROR
     */
    suspend fun updateSyncStatus(entryId: String, syncStatus: String) {
        journalDao.updateSyncStatus(entryId, syncStatus)
    }

    /**
     * Marks an entry as dirty (PENDING_SYNC) and bumps lastModified so tag
     * changes participate in last-write-wins conflict resolution.
     */
    suspend fun markEntryDirty(entryId: String) {
        journalDao.markEntryDirty(entryId, System.currentTimeMillis())
    }

    /**
     * Replaces the full attachment list for an entry and updates its sync status.
     * Used after cloud upload completes to swap local URIs for cloud URLs.
     */
    suspend fun updateAttachments(entryId: String, attachments: List<AttachmentData>, syncStatus: String) {
        journalDao.updateAttachments(entryId, attachments, syncStatus)
    }

    /**
     * Adds a single attachment to an existing entry and marks it as PENDING_SYNC.
     * This is the primary method called from the UI when a user picks a file.
     */
    suspend fun saveAttachmentLocally(entryId: String, attachment: AttachmentData) {
        val entry = journalDao.getEntryById(entryId) ?: return
        val updatedAttachments = (entry.attachments ?: emptyList()) + attachment
        val updatedEntry = entry.copy(
            attachments = updatedAttachments,
            syncStatus = "PENDING_SYNC",
            lastModified = System.currentTimeMillis()
        )
        journalDao.insertEntry(updatedEntry)
    }

    suspend fun addTag(entryId: String, tag: String) {
        journalDao.insertTag(EntryTagCrossRef(entryId, tag))
    }

    suspend fun removeTag(entryId: String, tag: String) {
        journalDao.deleteTag(entryId, tag)
    }

    suspend fun getTagsForEntry(entryId: String): List<String> {
        return journalDao.getTagsForEntry(entryId)
    }
}
