package com.example.healthjournal.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented smoke tests for GoalDao against a real in-memory Room
 * instance, verifying the v10->v11 schema (goals table) end-to-end:
 * upsert-replace semantics, deleteById, clear, and the observe flow.
 */
@RunWith(AndroidJUnit4::class)
class GoalDaoTest {

    private lateinit var database: JournalDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, JournalDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsert_replacesExistingParameterGoal() = runBlocking {
        val dao = database.goalDao()

        dao.upsertGoal(GoalEntity("WAIST", 90.0, 1_000L))
        dao.upsertGoal(GoalEntity("WAIST", 82.0, 2_000L))

        val rows = dao.getAll()
        assertEquals(1, rows.size)
        assertEquals(82.0, rows.single().target, 0.001)
    }

    @Test
    fun deleteById_removesOnlyThatRow() = runBlocking {
        val dao = database.goalDao()
        dao.upsertGoal(GoalEntity("WEIGHT", 75.0, 1_000L))
        dao.upsertGoal(GoalEntity("WAIST", 80.0, 1_000L))

        dao.deleteById("WEIGHT")

        val remaining = dao.getAll()
        assertEquals(1, remaining.size)
        assertEquals("WAIST", remaining.single().parameterId)
    }

    @Test
    fun clear_wipesAllRows() = runBlocking {
        val dao = database.goalDao()
        dao.upsertGoal(GoalEntity("WEIGHT", 75.0, 1_000L))
        dao.upsertGoal(GoalEntity("BICEP", 40.0, 1_000L))

        dao.clear()

        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun observeAll_emitsCurrentSnapshot() = runBlocking {
        val dao = database.goalDao()
        dao.upsertGoal(GoalEntity("CALF", 38.5, 1_000L))

        val snapshot = dao.observeAll().first()

        assertEquals(1, snapshot.size)
        assertFalse(snapshot.isEmpty())
    }
}
