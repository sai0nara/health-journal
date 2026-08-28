package com.example.healthjournal.data

import com.example.healthjournal.data.local.BodyMeasurementDao
import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.DeletedEntry
import kotlinx.coroutines.flow.Flow

class BodyMeasurementRepository(private val dao: BodyMeasurementDao) {

    val allEntries: Flow<List<BodyMeasurementEntry>> = dao.getAllEntries()

    suspend fun insert(entry: BodyMeasurementEntry) {
        dao.insertEntry(entry)
    }

    suspend fun getEntryById(entryId: String): BodyMeasurementEntry? {
        return dao.getEntryById(entryId)
    }

    /**
     * Permanently deletes a measurement: records a sync tombstone first so a
     * stale cloud copy cannot resurrect the row, then removes it locally.
     */
    suspend fun deleteEntry(entryId: String) {
        dao.insertDeletedEntry(DeletedEntry(entryId))
        dao.deleteEntriesByIds(listOf(entryId))
    }

    /** Snapshot of entries awaiting upload, used by the sync worker. */
    suspend fun getPendingSyncEntries(): List<BodyMeasurementEntry> {
        return dao.getPendingSyncEntries()
    }

    suspend fun getLatestWeight(): Double? {
        return dao.getLatestWeight()
    }

    suspend fun updateSyncStatus(entryId: String, syncStatus: String) {
        dao.updateSyncStatus(entryId, syncStatus)
    }

    suspend fun markEntryDirty(entryId: String) {
        dao.markEntryDirty(entryId, System.currentTimeMillis())
    }
}
