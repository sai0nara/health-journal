package com.example.healthjournal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMeasurementDao {

    @Query("SELECT * FROM body_measurements ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<BodyMeasurementEntry>>

    /** One-shot snapshot for the sync worker (Flow is for live UI). */
    @Query("SELECT * FROM body_measurements")
    suspend fun getAllEntriesList(): List<BodyMeasurementEntry>

    @Query("SELECT * FROM body_measurements WHERE entry_id = :entryId")
    suspend fun getEntryById(entryId: String): BodyMeasurementEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: BodyMeasurementEntry)

    /** Upserts the merged sync result, marking records as synced. */
    @Transaction
    suspend fun importAll(entries: List<BodyMeasurementEntry>) {
        entries.forEach { entry ->
            insertEntry(entry.copy(isSynced = true, syncStatus = "SYNCED"))
        }
    }

    @Query("DELETE FROM body_measurements WHERE entry_id IN (:entryIds)")
    suspend fun deleteEntriesByIds(entryIds: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedEntry(deletedEntry: DeletedEntry)

    /** Full deletion ledger snapshot for merge with the remote ledger. */
    @Query("SELECT * FROM deleted_entries")
    suspend fun getAllDeletedEntries(): List<DeletedEntry>

    @Query("DELETE FROM deleted_entries WHERE deletedAt < :cutoffTimestamp")
    suspend fun removeDeletedEntriesBefore(cutoffTimestamp: Long)

    @Query("SELECT * FROM body_measurements WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncEntries(): List<BodyMeasurementEntry>

    @Query("SELECT weight_kg FROM body_measurements ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestWeight(): Double?

    @Query("UPDATE body_measurements SET syncStatus = :syncStatus WHERE entry_id = :entryId")
    suspend fun updateSyncStatus(entryId: String, syncStatus: String)

    @Query("UPDATE body_measurements SET syncStatus = 'PENDING_SYNC', lastModified = :lastModified WHERE entry_id = :entryId")
    suspend fun markEntryDirty(entryId: String, lastModified: Long)
}
