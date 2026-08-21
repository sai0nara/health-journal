package com.example.healthjournal.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class BodyMeasurementDaoTest {

    private lateinit var bodyMeasurementDao: BodyMeasurementDao
    private lateinit var db: JournalDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, JournalDatabase::class.java
        ).build()
        bodyMeasurementDao = db.bodyMeasurementDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeEntryAndReadInList() = runBlocking {
        val entry = BodyMeasurementEntry(weight_kg = 78.5)
        bodyMeasurementDao.insertEntry(entry)

        val all = bodyMeasurementDao.getAllEntries().first()

        assertEquals(1, all.size)
        assertEquals(78.5, all[0].weight_kg!!, 0.0)
        assertEquals("PENDING_SYNC", all[0].syncStatus)
    }

    @Test
    fun getAllEntries_ordersByTimestampDescending() = runBlocking {
        bodyMeasurementDao.insertEntry(BodyMeasurementEntry(timestamp = 1_000L, weight_kg = 70.0))
        bodyMeasurementDao.insertEntry(BodyMeasurementEntry(timestamp = 3_000L, waist_cm = 85.0))
        bodyMeasurementDao.insertEntry(BodyMeasurementEntry(timestamp = 2_000L, chest_cm = 100.0))

        val all = bodyMeasurementDao.getAllEntries().first()

        assertEquals(listOf(3_000L, 2_000L, 1_000L), all.map { it.timestamp })
    }

    @Test
    fun partialEntry_preservesNullColumns() = runBlocking {
        val entry = BodyMeasurementEntry(timestamp = 5_000L, bicep_cm = 32.5)
        bodyMeasurementDao.insertEntry(entry)

        val loaded = bodyMeasurementDao.getEntryById(entry.entry_id)

        assertEquals(32.5, loaded?.bicep_cm!!, 0.0)
        assertNull(loaded.weight_kg)
        assertNull(loaded.chest_cm)
        assertNull(loaded.waist_cm)
        assertNull(loaded.glute_cm)
        assertNull(loaded.thigh_cm)
        assertNull(loaded.calf_cm)
    }

    @Test
    fun getEntryByIdReturnsNullForNonExistent() = runBlocking {
        assertNull(bodyMeasurementDao.getEntryById("non_existent_id"))
    }

    @Test
    fun deleteEntriesByIds_removesRow() = runBlocking {
        val entry = BodyMeasurementEntry(weight_kg = 78.5)
        bodyMeasurementDao.insertEntry(entry)

        bodyMeasurementDao.deleteEntriesByIds(listOf(entry.entry_id))

        assertTrue(bodyMeasurementDao.getAllEntries().first().isEmpty())
    }

    @Test
    fun undoRestore_reinsertedCopyKeepsAllValues() = runBlocking {
        val original = BodyMeasurementEntry(
            timestamp = 7_000L,
            weight_kg = 80.0,
            waist_cm = 86.0,
            syncStatus = "SYNCED"
        )
        bodyMeasurementDao.insertEntry(original)

        // Simulate the Undo flow: snapshot -> hard delete -> re-insert snapshot.
        val snapshot = bodyMeasurementDao.getEntryById(original.entry_id)
        bodyMeasurementDao.deleteEntriesByIds(listOf(original.entry_id))
        bodyMeasurementDao.insertEntry(snapshot!!)

        val restored = bodyMeasurementDao.getAllEntries().first().single()
        assertEquals(snapshot, restored)
        assertEquals("SYNCED", restored.syncStatus)
    }

    @Test
    fun getPendingSyncEntries_onlyReturnsPendingRows() = runBlocking {
        bodyMeasurementDao.insertEntry(BodyMeasurementEntry(weight_kg = 78.0))
        bodyMeasurementDao.insertEntry(
            BodyMeasurementEntry(waist_cm = 85.0).copy(syncStatus = "SYNCED")
        )

        val pending = bodyMeasurementDao.getPendingSyncEntries()

        assertEquals(1, pending.size)
        assertEquals(78.0, pending[0].weight_kg!!, 0.0)
    }

    @Test
    fun updateSyncStatus_updatesTargetRowOnly() = runBlocking {
        val a = BodyMeasurementEntry(weight_kg = 78.0)
        val b = BodyMeasurementEntry(waist_cm = 85.0)
        bodyMeasurementDao.insertEntry(a)
        bodyMeasurementDao.insertEntry(b)

        bodyMeasurementDao.updateSyncStatus(a.entry_id, "SYNCED")

        assertEquals("SYNCED", bodyMeasurementDao.getEntryById(a.entry_id)?.syncStatus)
        assertEquals("PENDING_SYNC", bodyMeasurementDao.getEntryById(b.entry_id)?.syncStatus)
    }

    @Test
    fun markEntryDirty_resetsSyncStatusAndBumpsLastModified() = runBlocking {
        val entry = BodyMeasurementEntry(timestamp = 1_000L, lastModified = 1_000L)
        bodyMeasurementDao.insertEntry(entry.copy(syncStatus = "SYNCED"))

        bodyMeasurementDao.markEntryDirty(entry.entry_id, 9_000L)

        val dirty = bodyMeasurementDao.getEntryById(entry.entry_id)!!
        assertEquals("PENDING_SYNC", dirty.syncStatus)
        assertEquals(9_000L, dirty.lastModified)
    }
}
