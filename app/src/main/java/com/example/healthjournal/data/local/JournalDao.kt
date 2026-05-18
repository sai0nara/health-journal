package com.example.healthjournal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<JournalEntry>>

    @Query("""
        SELECT * FROM journal_entries 
        ORDER BY 
        CASE WHEN :isAsc = 1 THEN timestamp END ASC,
        CASE WHEN :isAsc = 0 THEN timestamp END DESC
    """)
    fun getEntriesSortedByDate(isAsc: Boolean): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE description LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchEntries(query: String): Flow<List<JournalEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntry)

    @Query("SELECT * FROM journal_entries WHERE entry_id = :entryId")
    suspend fun getEntryById(entryId: String): JournalEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<JournalEntry>)
}
