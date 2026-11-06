package com.example.healthjournal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMeasurementDao {

    @Query("SELECT * FROM body_measurements WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getAllEntries(profileId: String): Flow<List<BodyMeasurementEntry>>

    /** One-shot snapshot for the sync worker (Flow is for live UI). */
    @Query("SELECT * FROM body_measurements WHERE profileId = :profileId")
    suspend fun getAllEntriesList(profileId: String): List<BodyMeasurementEntry>

    @Query("SELECT * FROM body_measurements WHERE profileId = :profileId AND entry_id = :entryId")
    suspend fun getEntryById(profileId: String, entryId: String): BodyMeasurementEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: BodyMeasurementEntry)

    /** Upserts the merged sync result, marking records as synced. */
    @Transaction
    suspend fun importAll(profileId: String, entries: List<BodyMeasurementEntry>) {
        entries.forEach { entry ->
            insertEntry(entry.copy(profileId = profileId, isSynced = true, syncStatus = "SYNCED"))
        }
    }

    @Query("DELETE FROM body_measurements WHERE profileId = :profileId AND entry_id IN (:entryIds)")
    suspend fun deleteEntriesByIds(profileId: String, entryIds: List<String>)

    @Query("DELETE FROM body_measurements WHERE profileId = :profileId")
    suspend fun clearAll(profileId: String)

    /** Plain replace-insert that preserves each record's original sync fields
     *  (used by restore so restored data is re-synced, unlike [importAll]). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceAll(entries: List<BodyMeasurementEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedEntry(deletedEntry: DeletedEntry)

    /** Full deletion ledger snapshot for merge with the remote ledger. */
    @Query("SELECT * FROM deleted_entries WHERE profileId = :profileId")
    suspend fun getAllDeletedEntries(profileId: String): List<DeletedEntry>

    @Query("DELETE FROM deleted_entries WHERE profileId = :profileId AND deletedAt < :cutoffTimestamp")
    suspend fun removeDeletedEntriesBefore(profileId: String, cutoffTimestamp: Long)

    @Query("SELECT * FROM body_measurements WHERE profileId = :profileId AND syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncEntries(profileId: String): List<BodyMeasurementEntry>

    @Query("UPDATE body_measurements SET syncStatus = :syncStatus WHERE profileId = :profileId AND entry_id = :entryId")
    suspend fun updateSyncStatus(profileId: String, entryId: String, syncStatus: String)

    @Query(
        "UPDATE body_measurements SET syncStatus = 'PENDING_SYNC', lastModified = :lastModified " +
            "WHERE profileId = :profileId AND entry_id = :entryId"
    )
    suspend fun markEntryDirty(profileId: String, entryId: String, lastModified: Long)
}
