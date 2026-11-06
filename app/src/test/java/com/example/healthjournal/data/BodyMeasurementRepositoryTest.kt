package com.example.healthjournal.data

import com.example.healthjournal.data.local.BodyMeasurementDao
import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.DeletedEntry
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BodyMeasurementRepositoryTest {

    private lateinit var repository: BodyMeasurementRepository
    private lateinit var activeProfile: FakeActiveProfileProvider
    private val dao: BodyMeasurementDao = mockk()

    @Before
    fun setup() {
        every { dao.getAllEntries(any()) } returns flowOf(emptyList())
        activeProfile = FakeActiveProfileProvider()
        repository = BodyMeasurementRepository(dao, activeProfile)
    }

    @Test
    fun insertCallsDaoWithActiveProfile() = runBlocking {
        val entry = BodyMeasurementEntry(waist_cm = 85.0)
        coEvery { dao.insertEntry(any()) } returns Unit

        repository.insert(entry)

        coVerify { dao.insertEntry(entry.copy(profileId = SELF_PROFILE_ID)) }
    }

    @Test
    fun allEntriesReloadsForSwitchedProfile() = runBlocking {
        val me = BodyMeasurementEntry(weight_kg = 78.5)
        val sibling = BodyMeasurementEntry(weight_kg = 60.0)
        every { dao.getAllEntries(SELF_PROFILE_ID) } returns flowOf(listOf(me))
        every { dao.getAllEntries("sibling") } returns flowOf(listOf(sibling))

        assertEquals(me, repository.allEntries.first().single())

        activeProfile.switchTo("sibling")

        assertEquals(sibling, repository.allEntries.first().single())
    }

    @Test
    fun getEntryByIdCallsDao() = runBlocking {
        val entryId = "m1"
        val entry = BodyMeasurementEntry(entry_id = entryId, weight_kg = 78.5)
        coEvery { dao.getEntryById(SELF_PROFILE_ID, entryId) } returns entry

        val result = repository.getEntryById(entryId)

        assertEquals(entry, result)
    }

    @Test
    fun getEntryByIdReturnsNullWhenMissing() = runBlocking {
        coEvery { dao.getEntryById(SELF_PROFILE_ID, "missing") } returns null

        assertEquals(null, repository.getEntryById("missing"))
    }

    @Test
    fun deleteEntryWritesTombstoneBeforeDeletingRow() = runBlocking {
        val entryId = "m2"
        coEvery { dao.insertDeletedEntry(any<DeletedEntry>()) } returns Unit
        coEvery { dao.deleteEntriesByIds(any(), any()) } returns Unit

        repository.deleteEntry(entryId)

        coVerify {
            dao.insertDeletedEntry(match {
                it.entry_id == entryId && it.profileId == SELF_PROFILE_ID
            })
            dao.deleteEntriesByIds(SELF_PROFILE_ID, listOf(entryId))
        }
    }

    @Test
    fun getPendingSyncEntriesCallsDao() = runBlocking {
        val pending = listOf(BodyMeasurementEntry(weight_kg = 78.5))
        coEvery { dao.getPendingSyncEntries(SELF_PROFILE_ID) } returns pending

        assertEquals(pending, repository.getPendingSyncEntries())
    }

    @Test
    fun updateSyncStatusCallsDao() = runBlocking {
        coEvery { dao.updateSyncStatus(SELF_PROFILE_ID, "m3", "SYNCED") } returns Unit

        repository.updateSyncStatus("m3", "SYNCED")

        coVerify { dao.updateSyncStatus(SELF_PROFILE_ID, "m3", "SYNCED") }
    }

    @Test
    fun markEntryDirtyUsesCurrentTime() = runBlocking {
        coEvery { dao.markEntryDirty(SELF_PROFILE_ID, "m4", any()) } returns Unit

        repository.markEntryDirty("m4")

        coVerify {
            dao.markEntryDirty(SELF_PROFILE_ID, "m4", match { it <= System.currentTimeMillis() })
        }
    }
}
