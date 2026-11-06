package com.example.healthjournal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries WHERE profileId = :profileId AND isArchived = 0 ORDER BY timestamp DESC")
    fun getAllEntries(profileId: String): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE profileId = :profileId ORDER BY lastModified DESC")
    fun getAllEntriesIncludingArchived(profileId: String): Flow<List<JournalEntry>>

    @Query(
        "SELECT * FROM journal_entries WHERE profileId = :profileId " +
            "AND timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC"
    )
    suspend fun getAllEntriesInDateRange(profileId: String, startDate: Long, endDate: Long): List<JournalEntry>

    @Query("""
        SELECT * FROM journal_entries
        WHERE profileId = :profileId AND isArchived = 0
        ORDER BY
        CASE WHEN :isAsc = 1 THEN timestamp END ASC,
        CASE WHEN :isAsc = 0 THEN timestamp END DESC
    """)
    fun getEntriesSortedByDate(profileId: String, isAsc: Boolean): Flow<List<JournalEntry>>

    @Query("""
        SELECT * FROM journal_entries
        WHERE profileId = :profileId AND isArchived = 0 AND description LIKE '%' || :query || '%'
        ORDER BY
        CASE WHEN :isAsc = 1 THEN timestamp END ASC,
        CASE WHEN :isAsc = 0 THEN timestamp END DESC
    """)
    fun searchEntries(profileId: String, query: String, isAsc: Boolean): Flow<List<JournalEntry>>

    @Query("""
        SELECT * FROM journal_entries
        WHERE profileId = :profileId AND isArchived = 0
        AND description LIKE '%' || :query || '%'
        AND (
            (:tagCount = 0) OR
            (entry_id IN (
                SELECT c.entryId FROM EntryTagCrossRef c
                INNER JOIN journal_entries j ON j.entry_id = c.entryId
                WHERE j.profileId = :profileId AND c.tag IN (:tags)
                GROUP BY c.entryId
                HAVING COUNT(DISTINCT c.tag) = :tagCount
            ))
        )
        ORDER BY
        CASE WHEN :isAsc = 1 THEN timestamp END ASC,
        CASE WHEN :isAsc = 0 THEN timestamp END DESC
    """)
    fun searchEntriesWithTags(
        profileId: String,
        query: String,
        tags: List<String>,
        tagCount: Int,
        isAsc: Boolean
    ): Flow<List<JournalEntry>>

    @Query("""
        SELECT * FROM journal_entries
        WHERE profileId = :profileId AND isArchived = 1
        AND description LIKE '%' || :query || '%'
        AND (
            (:tagCount = 0) OR
            (entry_id IN (
                SELECT c.entryId FROM EntryTagCrossRef c
                INNER JOIN journal_entries j ON j.entry_id = c.entryId
                WHERE j.profileId = :profileId AND c.tag IN (:tags)
                GROUP BY c.entryId
                HAVING COUNT(DISTINCT c.tag) = :tagCount
            ))
        )
        ORDER BY lastModified DESC
    """)
    fun searchArchivedEntriesWithTags(
        profileId: String,
        query: String,
        tags: List<String>,
        tagCount: Int
    ): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE profileId = :profileId AND isArchived = 1 ORDER BY lastModified DESC")
    fun getArchivedEntries(profileId: String): Flow<List<JournalEntry>>

    @Query("""
        SELECT * FROM journal_entries
        WHERE profileId = :profileId AND isArchived = 1 AND description LIKE '%' || :query || '%'
        ORDER BY lastModified DESC
    """)
    fun searchArchivedEntries(profileId: String, query: String): Flow<List<JournalEntry>>

    @Query("SELECT entry_id FROM journal_entries WHERE profileId = :profileId AND isArchived = 1")
    suspend fun getArchivedEntriesIds(profileId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntry)

    @Query("SELECT * FROM journal_entries WHERE profileId = :profileId AND entry_id = :entryId")
    suspend fun getEntryById(profileId: String, entryId: String): JournalEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<JournalEntry>)

    @Query(
        "UPDATE journal_entries SET isArchived = :isArchived, isSynced = 0, lastModified = :lastModified " +
            "WHERE profileId = :profileId AND entry_id = :entryId"
    )
    suspend fun updateArchiveStatus(profileId: String, entryId: String, isArchived: Boolean, lastModified: Long)

    @Query("DELETE FROM journal_entries WHERE profileId = :profileId AND entry_id IN (:entryIds)")
    suspend fun deleteEntriesByIds(profileId: String, entryIds: List<String>)

    @Query("DELETE FROM journal_entries WHERE profileId = :profileId AND isArchived = 1")
    suspend fun deleteAllArchivedEntries(profileId: String)

    @Query("SELECT * FROM journal_entries WHERE profileId = :profileId AND entry_id IN (:entryIds)")
    suspend fun getEntriesByIds(profileId: String, entryIds: List<String>): List<JournalEntry>

    @Query("SELECT * FROM journal_entries WHERE profileId = :profileId AND isArchived = 1")
    suspend fun getArchivedEntriesList(profileId: String): List<JournalEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedEntry(deletedEntry: DeletedEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDeletedEntries(entries: List<DeletedEntry>)

    @Query("SELECT * FROM deleted_entries WHERE profileId = :profileId")
    suspend fun getAllDeletedEntries(profileId: String): List<DeletedEntry>

    @Query("DELETE FROM deleted_entries WHERE profileId = :profileId AND entry_id IN (:entryIds)")
    suspend fun removeDeletedEntries(profileId: String, entryIds: List<String>)

    @Query("DELETE FROM deleted_entries WHERE profileId = :profileId AND deletedAt < :cutoffTimestamp")
    suspend fun removeDeletedEntriesBefore(profileId: String, cutoffTimestamp: Long)

    @Query("SELECT * FROM journal_entries WHERE profileId = :profileId AND syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncEntries(profileId: String): List<JournalEntry>

    @Query("UPDATE journal_entries SET syncStatus = :syncStatus WHERE profileId = :profileId AND entry_id = :entryId")
    suspend fun updateSyncStatus(profileId: String, entryId: String, syncStatus: String)

    @Query(
        "UPDATE journal_entries SET syncStatus = 'PENDING_SYNC', lastModified = :lastModified " +
            "WHERE profileId = :profileId AND entry_id = :entryId"
    )
    suspend fun markEntryDirty(profileId: String, entryId: String, lastModified: Long)

    @Query(
        "UPDATE journal_entries SET attachments = :attachments, syncStatus = :syncStatus " +
            "WHERE profileId = :profileId AND entry_id = :entryId"
    )
    suspend fun updateAttachments(
        profileId: String,
        entryId: String,
        attachments: List<AttachmentData>,
        syncStatus: String
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(crossRef: EntryTagCrossRef)

    @Query(
        "DELETE FROM EntryTagCrossRef WHERE entryId = :entryId AND tag = :tag " +
            "AND entryId IN (SELECT entry_id FROM journal_entries WHERE profileId = :profileId)"
    )
    suspend fun deleteTag(profileId: String, entryId: String, tag: String)

    @Query(
        "DELETE FROM EntryTagCrossRef WHERE entryId = :entryId " +
            "AND entryId IN (SELECT entry_id FROM journal_entries WHERE profileId = :profileId)"
    )
    suspend fun deleteAllTagsForEntry(profileId: String, entryId: String)

    @Query(
        "DELETE FROM EntryTagCrossRef WHERE entryId IN (:entryIds) " +
            "AND entryId IN (SELECT entry_id FROM journal_entries WHERE profileId = :profileId)"
    )
    suspend fun deleteAllTagsForEntries(profileId: String, entryIds: List<String>)

    @Query(
        "SELECT c.tag FROM EntryTagCrossRef c " +
            "INNER JOIN journal_entries j ON j.entry_id = c.entryId " +
            "WHERE j.profileId = :profileId AND c.entryId = :entryId"
    )
    suspend fun getTagsForEntry(profileId: String, entryId: String): List<String>

    @Query(
        "SELECT c.* FROM EntryTagCrossRef c " +
            "INNER JOIN journal_entries j ON j.entry_id = c.entryId " +
            "WHERE j.profileId = :profileId"
    )
    suspend fun getAllTags(profileId: String): List<EntryTagCrossRef>

    @Query(
        "SELECT c.* FROM EntryTagCrossRef c " +
            "INNER JOIN journal_entries j ON j.entry_id = c.entryId " +
            "WHERE j.profileId = :profileId"
    )
    fun observeAllTags(profileId: String): Flow<List<EntryTagCrossRef>>

    @Query("DELETE FROM journal_entries WHERE profileId = :profileId")
    suspend fun clearAllEntries(profileId: String)

    @Query("DELETE FROM deleted_entries WHERE profileId = :profileId")
    suspend fun clearAllDeletedEntries(profileId: String)

    @Query(
        "DELETE FROM EntryTagCrossRef WHERE entryId IN " +
            "(SELECT entry_id FROM journal_entries WHERE profileId = :profileId)"
    )
    suspend fun clearAllTags(profileId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTags(tags: List<EntryTagCrossRef>)

    @Transaction
    suspend fun deleteAllForProfile(profileId: String) {
        clearAllTags(profileId)
        clearAllDeletedEntries(profileId)
        clearAllEntries(profileId)
    }
}
