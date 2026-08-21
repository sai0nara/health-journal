package com.example.healthjournal.data

import com.example.healthjournal.data.local.BodyMeasurementDao
import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.DeletedEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BodyMeasurementRepositoryTest {

    private lateinit var repository: BodyMeasurementRepository
    private val dao: BodyMeasurementDao = mockk()

    @Before
    fun setup() {
        coEvery { dao.getAllEntries() } returns flowOf(emptyList())
        repository = BodyMeasurementRepository(dao)
    }

    @Test
    fun insertCallsDao() = runBlocking {
        val entry = BodyMeasurementEntry(waist_cm = 85.0)
        coEvery { dao.insertEntry(any()) } returns Unit

        repository.insert(entry)

        coVerify { dao.insertEntry(entry) }
    }

    @Test
    fun allEntriesReturnsFlowFromDao() = runBlocking {
        val entries = listOf(BodyMeasurementEntry(weight_kg = 78.5))
        coEvery { dao.getAllEntries() } returns flowOf(entries)

        val result = BodyMeasurementRepository(dao).allEntries.first()

        assertEquals(entries, result)
    }

    @Test
    fun getEntryByIdCallsDao() = runBlocking {
        val entryId = "m1"
        val entry = BodyMeasurementEntry(entry_id = entryId, weight_kg = 78.5)
        coEvery { dao.getEntryById(entryId) } returns entry

        val result = repository.getEntryById(entryId)

        assertEquals(entry, result)
    }

    @Test
    fun getEntryByIdReturnsNullWhenMissing() = runBlocking {
        coEvery { dao.getEntryById("missing") } returns null

        assertEquals(null, repository.getEntryById("missing"))
    }

    @Test
    fun deleteEntryWritesTombstoneBeforeDeletingRow() = runBlocking {
        val entryId = "m2"
        coEvery { dao.insertDeletedEntry(any<DeletedEntry>()) } returns Unit
        coEvery { dao.deleteEntriesByIds(any()) } returns Unit

        repository.deleteEntry(entryId)

        coVerify {
            dao.insertDeletedEntry(match { it.entry_id == entryId })
            dao.deleteEntriesByIds(listOf(entryId))
        }
    }

    @Test
    fun getPendingSyncEntriesCallsDao() = runBlocking {
        val pending = listOf(BodyMeasurementEntry(weight_kg = 78.5))
        coEvery { dao.getPendingSyncEntries() } returns pending

        assertEquals(pending, repository.getPendingSyncEntries())
    }

    @Test
    fun updateSyncStatusCallsDao() = runBlocking {
        coEvery { dao.updateSyncStatus("m3", "SYNCED") } returns Unit

        repository.updateSyncStatus("m3", "SYNCED")

        coVerify { dao.updateSyncStatus("m3", "SYNCED") }
    }

    @Test
    fun markEntryDirtyUsesCurrentTime() = runBlocking {
        coEvery { dao.markEntryDirty("m4", any()) } returns Unit

        repository.markEntryDirty("m4")

        coVerify { dao.markEntryDirty("m4", match { it <= System.currentTimeMillis() }) }
    }
}
