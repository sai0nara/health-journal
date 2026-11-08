package com.example.healthjournal.data

import com.example.healthjournal.data.local.BodyMeasurementDao
import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.DeletedEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class BodyMeasurementRepository(
    private val dao: BodyMeasurementDao,
    private val activeProfile: ActiveProfileProvider
) {

    val allEntries: Flow<List<BodyMeasurementEntry>> =
        activeProfile.activeProfileId.flatMapLatest { dao.getAllEntries(it) }

    fun currentProfileId(): String = activeProfile.currentProfileId()

    suspend fun insert(entry: BodyMeasurementEntry) {
        insert(currentProfileId(), entry)
    }

    suspend fun insert(profileId: String, entry: BodyMeasurementEntry) {
        dao.insertEntry(entry.copy(profileId = profileId))
    }

    suspend fun getEntryById(entryId: String): BodyMeasurementEntry? =
        getEntryById(currentProfileId(), entryId)

    suspend fun getEntryById(profileId: String, entryId: String): BodyMeasurementEntry? =
        dao.getEntryById(profileId, entryId)

    suspend fun getAllEntriesList(profileId: String): List<BodyMeasurementEntry> =
        dao.getAllEntriesList(profileId)

    /**
     * Permanently deletes a measurement: records a sync tombstone first so a
     * stale cloud copy cannot resurrect the row, then removes it locally.
     */
    suspend fun deleteEntry(entryId: String) {
        val profileId = currentProfileId()
        dao.insertDeletedEntry(DeletedEntry(entryId, profileId = profileId))
        dao.deleteEntriesByIds(profileId, listOf(entryId))
    }

    /** Snapshot of entries awaiting upload, used by the sync worker. */
    suspend fun getPendingSyncEntries(): List<BodyMeasurementEntry> =
        getPendingSyncEntries(currentProfileId())

    suspend fun getPendingSyncEntries(profileId: String): List<BodyMeasurementEntry> =
        dao.getPendingSyncEntries(profileId)

    suspend fun updateSyncStatus(entryId: String, syncStatus: String) {
        dao.updateSyncStatus(currentProfileId(), entryId, syncStatus)
    }

    suspend fun markEntryDirty(entryId: String) {
        dao.markEntryDirty(currentProfileId(), entryId, System.currentTimeMillis())
    }

    suspend fun importAll(profileId: String, entries: List<BodyMeasurementEntry>) {
        dao.importAll(profileId, entries)
    }
}
