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

        val rows = dao.getAll(SELF_PROFILE_ID)
        assertEquals(1, rows.size)
        assertEquals(82.0, rows.single().target, 0.001)
    }

    @Test
    fun deleteById_removesOnlyThatRow() = runBlocking {
        val dao = database.goalDao()
        dao.upsertGoal(GoalEntity("WEIGHT", 75.0, 1_000L))
        dao.upsertGoal(GoalEntity("WAIST", 80.0, 1_000L))

        dao.deleteById(SELF_PROFILE_ID, "WEIGHT")

        val remaining = dao.getAll(SELF_PROFILE_ID)
        assertEquals(1, remaining.size)
        assertEquals("WAIST", remaining.single().parameterId)
    }

    @Test
    fun clear_wipesAllRows() = runBlocking {
        val dao = database.goalDao()
        dao.upsertGoal(GoalEntity("WEIGHT", 75.0, 1_000L))
        dao.upsertGoal(GoalEntity("BICEP", 40.0, 1_000L))

        dao.clear(SELF_PROFILE_ID)

        assertTrue(dao.getAll(SELF_PROFILE_ID).isEmpty())
    }

    @Test
    fun observeAll_emitsCurrentSnapshot() = runBlocking {
        val dao = database.goalDao()
        dao.upsertGoal(GoalEntity("CALF", 38.5, 1_000L))

        val snapshot = dao.observeAll(SELF_PROFILE_ID).first()

        assertEquals(1, snapshot.size)
        assertFalse(snapshot.isEmpty())
    }

    @Test
    fun sameParameterCanHoldIndependentGoalsPerProfile() = runBlocking {
        val dao = database.goalDao()
        dao.upsertGoal(GoalEntity("WEIGHT", 75.0, 1_000L))
        dao.upsertGoal(GoalEntity("WEIGHT", 60.0, 2_000L, profileId = "sibling"))
        dao.upsertGoal(GoalEntity("WEIGHT", 55.0, 3_000L, profileId = "sibling"))

        assertEquals(75.0, dao.getAll(SELF_PROFILE_ID).single().target, 0.001)
        assertEquals(55.0, dao.getAll("sibling").single().target, 0.001)

        dao.deleteById(SELF_PROFILE_ID, "WEIGHT")
        assertTrue(dao.getAll(SELF_PROFILE_ID).isEmpty())
        assertEquals(1, dao.getAll("sibling").size)

        dao.clear(SELF_PROFILE_ID)
        assertEquals(1, dao.getAll("sibling").size)
    }
}
