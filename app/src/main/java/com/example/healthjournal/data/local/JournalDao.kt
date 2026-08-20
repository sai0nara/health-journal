package com.example.healthjournal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries WHERE isArchived = 0 ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries ORDER BY lastModified DESC")
    fun getAllEntriesIncludingArchived(): Flow<List<JournalEntry>>

    @Query("""
        SELECT * FROM journal_entries 
        WHERE isArchived = 0
        ORDER BY 
        CASE WHEN :isAsc = 1 THEN timestamp END ASC,
        CASE WHEN :isAsc = 0 THEN timestamp END DESC
    """)
    fun getEntriesSortedByDate(isAsc: Boolean): Flow<List<JournalEntry>>

    @Query("""
        SELECT * FROM journal_entries
        WHERE isArchived = 0 AND description LIKE '%' || :query || '%'
        ORDER BY
        CASE WHEN :isAsc = 1 THEN timestamp END ASC,
        CASE WHEN :isAsc = 0 THEN timestamp END DESC
    """)
    fun searchEntries(query: String, isAsc: Boolean): Flow<List<JournalEntry>>

    @Query("""
        SELECT * FROM journal_entries
        WHERE isArchived = 0 
        AND description LIKE '%' || :query || '%'
        AND (
            (:tagCount = 0) OR 
            (entry_id IN (
                SELECT entryId FROM EntryTagCrossRef 
                WHERE tag IN (:tags) 
                GROUP BY entryId 
                HAVING COUNT(DISTINCT tag) = :tagCount
            ))
        )
        ORDER BY
        CASE WHEN :isAsc = 1 THEN timestamp END ASC,
        CASE WHEN :isAsc = 0 THEN timestamp END DESC
    """)
    fun searchEntriesWithTags(query: String, tags: List<String>, tagCount: Int, isAsc: Boolean): Flow<List<JournalEntry>>

    @Query("""
        SELECT * FROM journal_entries
        WHERE isArchived = 1 
        AND description LIKE '%' || :query || '%'
        AND (
            (:tagCount = 0) OR 
            (entry_id IN (
                SELECT entryId FROM EntryTagCrossRef 
                WHERE tag IN (:tags) 
                GROUP BY entryId 
                HAVING COUNT(DISTINCT tag) = :tagCount
            ))
        )
        ORDER BY lastModified DESC
    """)
    fun searchArchivedEntriesWithTags(query: String, tags: List<String>, tagCount: Int): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE isArchived = 1 ORDER BY lastModified DESC")
    fun getArchivedEntries(): Flow<List<JournalEntry>>

    @Query("""
        SELECT * FROM journal_entries 
        WHERE isArchived = 1 AND description LIKE '%' || :query || '%'
        ORDER BY lastModified DESC
    """)
    fun searchArchivedEntries(query: String): Flow<List<JournalEntry>>

    @Query("SELECT entry_id FROM journal_entries WHERE isArchived = 1")
    suspend fun getArchivedEntriesIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntry)

    @Query("SELECT * FROM journal_entries WHERE entry_id = :entryId")
    suspend fun getEntryById(entryId: String): JournalEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<JournalEntry>)

    @Query("UPDATE journal_entries SET isArchived = :isArchived, isSynced = 0, lastModified = :lastModified WHERE entry_id = :entryId")
    suspend fun updateArchiveStatus(entryId: String, isArchived: Boolean, lastModified: Long)

    @Query("DELETE FROM journal_entries WHERE entry_id IN (:entryIds)")
    suspend fun deleteEntriesByIds(entryIds: List<String>)

    @Query("DELETE FROM journal_entries WHERE isArchived = 1")
    suspend fun deleteAllArchivedEntries()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedEntry(deletedEntry: DeletedEntry)

    @Query("SELECT * FROM deleted_entries")
    suspend fun getAllDeletedEntries(): List<DeletedEntry>

    @Query("DELETE FROM deleted_entries WHERE entry_id IN (:entryIds)")
    suspend fun removeDeletedEntries(entryIds: List<String>)

    @Query("DELETE FROM deleted_entries WHERE deletedAt < :cutoffTimestamp")
    suspend fun removeDeletedEntriesBefore(cutoffTimestamp: Long)

    @Query("SELECT * FROM journal_entries WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncEntries(): List<JournalEntry>

    @Query("UPDATE journal_entries SET syncStatus = :syncStatus WHERE entry_id = :entryId")
    suspend fun updateSyncStatus(entryId: String, syncStatus: String)

    @Query("UPDATE journal_entries SET attachments = :attachments, syncStatus = :syncStatus WHERE entry_id = :entryId")
    suspend fun updateAttachments(entryId: String, attachments: List<AttachmentData>, syncStatus: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(crossRef: EntryTagCrossRef)

    @Query("DELETE FROM EntryTagCrossRef WHERE entryId = :entryId AND tag = :tag")
    suspend fun deleteTag(entryId: String, tag: String)

    @Query("DELETE FROM EntryTagCrossRef WHERE entryId = :entryId")
    suspend fun deleteAllTagsForEntry(entryId: String)

    @Query("SELECT tag FROM EntryTagCrossRef WHERE entryId = :entryId")
    suspend fun getTagsForEntry(entryId: String): List<String>
}
