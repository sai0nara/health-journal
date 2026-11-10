package com.example.healthjournal.data

import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.data.local.DeletedEntry
import com.example.healthjournal.data.local.EntryTagCrossRef
import com.example.healthjournal.data.local.JournalDao
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.domain.TagIdentity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class JournalRepository(
    private val journalDao: JournalDao,
    private val activeProfile: ActiveProfileProvider
) {
    val allEntries: Flow<List<JournalEntry>> =
        activeProfile.activeProfileId.flatMapLatest { journalDao.getAllEntries(it) }
    val vitalsEntries: Flow<List<JournalEntry>> =
        activeProfile.activeProfileId.flatMapLatest { journalDao.getVitalsEntries(it) }
    val allTags: Flow<List<EntryTagCrossRef>> =
        activeProfile.activeProfileId.flatMapLatest { journalDao.observeAllTags(it) }
    val archivedEntries: Flow<List<JournalEntry>> =
        activeProfile.activeProfileId.flatMapLatest { journalDao.getArchivedEntries(it) }
    val activeProfileId: StateFlow<String> = activeProfile.activeProfileId

    fun currentProfileId(): String = activeProfile.currentProfileId()

    fun getVitalsEntries(profileId: String): Flow<List<JournalEntry>> =
        journalDao.getVitalsEntries(profileId)

    fun getEntriesSortedByDate(isAsc: Boolean): Flow<List<JournalEntry>> =
        activeProfile.activeProfileId.flatMapLatest { journalDao.getEntriesSortedByDate(it, isAsc) }

    fun searchEntries(query: String, isAsc: Boolean): Flow<List<JournalEntry>> =
        activeProfile.activeProfileId.flatMapLatest { journalDao.searchEntries(it, query, isAsc) }

    fun searchEntriesWithTags(query: String, tags: List<String>, isAsc: Boolean): Flow<List<JournalEntry>> =
        activeProfile.activeProfileId.flatMapLatest {
            journalDao.searchEntriesWithTags(it, query, tags, tags.size, isAsc)
        }

    fun searchArchivedEntries(query: String): Flow<List<JournalEntry>> =
        activeProfile.activeProfileId.flatMapLatest { journalDao.searchArchivedEntries(it, query) }

    fun searchArchivedEntriesWithTags(query: String, tags: List<String>): Flow<List<JournalEntry>> =
        activeProfile.activeProfileId.flatMapLatest {
            journalDao.searchArchivedEntriesWithTags(it, query, tags, tags.size)
        }

    suspend fun insert(entry: JournalEntry) {
        insert(currentProfileId(), entry)
    }

    /** Writes [entry] under [profileId] so a profile switch cannot redirect a stale form. */
    suspend fun insert(profileId: String, entry: JournalEntry) {
        journalDao.insertEntry(entry.copy(profileId = profileId))
    }

    suspend fun getEntryById(entryId: String): JournalEntry? =
        getEntryById(currentProfileId(), entryId)

    suspend fun getEntryById(profileId: String, entryId: String): JournalEntry? {
        val entry = journalDao.getEntryById(profileId, entryId) ?: return null
        return entry.withTags(journalDao.getTagsForEntry(profileId, entryId))
    }

    suspend fun importAll(entries: List<JournalEntry>) {
        importAll(currentProfileId(), entries)
    }

    suspend fun importAll(profileId: String, entries: List<JournalEntry>) {
        journalDao.insertAll(entries.map { it.copy(profileId = profileId) })
    }

    /**
     * Returns all entries (including archived) whose timestamp falls within the
     * given range, for export purposes.
     */
    suspend fun getAllEntriesInDateRange(startDate: Long, endDate: Long): List<JournalEntry> =
        getAllEntriesInDateRange(currentProfileId(), startDate, endDate)

    suspend fun getAllEntriesInDateRange(
        profileId: String,
        startDate: Long,
        endDate: Long
    ): List<JournalEntry> = journalDao.getAllEntriesInDateRange(profileId, startDate, endDate)

    suspend fun archiveEntry(entryId: String) {
        journalDao.updateArchiveStatus(currentProfileId(), entryId, true, System.currentTimeMillis())
    }

    suspend fun restoreEntry(entryId: String) {
        journalDao.updateArchiveStatus(currentProfileId(), entryId, false, System.currentTimeMillis())
    }

    suspend fun deleteEntries(entryIds: List<String>) {
        deleteEntries(currentProfileId(), entryIds)
    }

    suspend fun deleteEntries(profileId: String, entryIds: List<String>) {
        // Remove tag cross-refs first: the table has no FK cascade, and orphaned
        // rows would silently re-attach stale tags if the entry is ever
        // re-imported (e.g. tombstone expired while another device kept it).
        journalDao.deleteAllTagsForEntries(profileId, entryIds)
        journalDao.deleteEntriesByIds(profileId, entryIds)
        entryIds.forEach { id ->
            journalDao.insertDeletedEntry(DeletedEntry(id, profileId = profileId))
        }
    }

    suspend fun delete(profileId: String, entryId: String) {
        deleteEntries(profileId, listOf(entryId))
    }

    suspend fun deleteAllArchived() {
        val profileId = currentProfileId()
        // Record all archived IDs for sync before deleting
        val archivedIds = journalDao.getArchivedEntriesIds(profileId)
        journalDao.deleteAllTagsForEntries(profileId, archivedIds)
        journalDao.deleteAllArchivedEntries(profileId)
        archivedIds.forEach { id ->
            journalDao.insertDeletedEntry(DeletedEntry(id, profileId = profileId))
        }
    }

    /**
     * Fetches full entries for the given IDs. Used by callers that need entry
     * metadata (e.g. attachment file paths) before the rows are deleted.
     */
    suspend fun getEntriesByIds(entryIds: List<String>): List<JournalEntry> =
        getEntriesByIds(currentProfileId(), entryIds)

    suspend fun getEntriesByIds(profileId: String, entryIds: List<String>): List<JournalEntry> =
        journalDao.getEntriesByIds(profileId, entryIds)

    /**
     * Snapshot of all archived entries. Used by callers that need entry metadata
     * (e.g. attachment file paths) before emptying the archive.
     */
    suspend fun getArchivedEntriesList(): List<JournalEntry> =
        getArchivedEntriesList(currentProfileId())

    suspend fun getArchivedEntriesList(profileId: String): List<JournalEntry> =
        journalDao.getArchivedEntriesList(profileId)

    suspend fun getDeletedEntryIds(): List<String> = getDeletedEntryIds(currentProfileId())

    suspend fun getDeletedEntryIds(profileId: String): List<String> =
        journalDao.getAllDeletedEntries(profileId).map { it.entry_id }

    /**
     * Removes tombstone records older than the grace period. Tombstones younger
     * than [TOMBSTONE_GRACE_PERIOD_MS] are kept so that a stale cloud copy arriving
     * in a later sync cycle cannot resurrect a deleted entry.
     */
    suspend fun clearDeletedEntries(now: Long = System.currentTimeMillis()) {
        clearDeletedEntries(currentProfileId(), now)
    }

    suspend fun clearDeletedEntries(profileId: String, now: Long = System.currentTimeMillis()) {
        journalDao.removeDeletedEntriesBefore(profileId, now - TOMBSTONE_GRACE_PERIOD_MS)
    }

    companion object {
        const val TOMBSTONE_GRACE_PERIOD_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
    }

    /**
     * Returns all entries with a syncStatus of PENDING_SYNC.
     * Used by the PeriodicSyncWorker to identify entries that need uploading.
     */
    suspend fun getPendingSyncEntries(): List<JournalEntry> =
        getPendingSyncEntries(currentProfileId())

    suspend fun getPendingSyncEntries(profileId: String): List<JournalEntry> =
        journalDao.getPendingSyncEntries(profileId)

    /**
     * Updates the sync status for a specific entry.
     * @param entryId The entry to update.
     * @param syncStatus One of: PENDING_SYNC, SYNCING, SYNCED, SYNC_ERROR
     */
    suspend fun updateSyncStatus(entryId: String, syncStatus: String) {
        journalDao.updateSyncStatus(currentProfileId(), entryId, syncStatus)
    }

    /**
     * Marks an entry as dirty (PENDING_SYNC) and bumps lastModified so tag
     * changes participate in last-write-wins conflict resolution.
     */
    suspend fun markEntryDirty(entryId: String) {
        journalDao.markEntryDirty(currentProfileId(), entryId, System.currentTimeMillis())
    }

    /**
     * Replaces the full attachment list for an entry and updates its sync status.
     * Used after cloud upload completes to swap local URIs for cloud URLs.
     */
    suspend fun updateAttachments(entryId: String, attachments: List<AttachmentData>, syncStatus: String) {
        journalDao.updateAttachments(currentProfileId(), entryId, attachments, syncStatus)
    }

    /**
     * Adds a single attachment to an existing entry and marks it as PENDING_SYNC.
     * This is the primary method called from the UI when a user picks a file.
     */
    suspend fun saveAttachmentLocally(entryId: String, attachment: AttachmentData) {
        val profileId = currentProfileId()
        val entry = journalDao.getEntryById(profileId, entryId) ?: return
        val updatedAttachments = (entry.attachments ?: emptyList()) + attachment
        val updatedEntry = entry.copy(
            attachments = updatedAttachments,
            syncStatus = "PENDING_SYNC",
            lastModified = System.currentTimeMillis()
        )
        journalDao.insertEntry(updatedEntry)
    }

    suspend fun addTag(entryId: String, tag: String) {
        addTag(currentProfileId(), entryId, tag)
    }

    suspend fun addTag(profileId: String, entryId: String, tag: String) {
        val canonical = TagIdentity.canonical(tag)
        if (canonical.isEmpty()) return
        // The entry id is globally unique; write only if it belongs to the
        // target profile so a stale screen cannot tag another profile's entry.
        if (journalDao.getEntryById(profileId, entryId) == null) return
        journalDao.insertTag(EntryTagCrossRef(entryId, canonical))
    }

    suspend fun removeTag(entryId: String, tag: String) {
        removeTag(currentProfileId(), entryId, tag)
    }

    suspend fun removeTag(profileId: String, entryId: String, tag: String) {
        journalDao.deleteTag(profileId, entryId, TagIdentity.canonical(tag))
    }

    suspend fun getTagsForEntry(entryId: String): List<String> =
        getTagsForEntry(currentProfileId(), entryId)

    suspend fun getTagsForEntry(profileId: String, entryId: String): List<String> =
        journalDao.getTagsForEntry(profileId, entryId)
}
