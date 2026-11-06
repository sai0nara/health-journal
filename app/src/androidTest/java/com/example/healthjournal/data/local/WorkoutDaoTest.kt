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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class WorkoutDaoTest {

    private lateinit var db: JournalDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, JournalDatabase::class.java).build()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun sessionsAndPresetsAreScopedToTheirProfile() = runBlocking {
        val sessions = db.workoutSessionDao()
        val presets = db.workoutPresetDao()
        val mine = WorkoutSession(session_id = "mine", startTimestamp = 10L)
        val theirs = WorkoutSession(session_id = "theirs", startTimestamp = 20L, profileId = "sibling")
        sessions.upsertSession(mine)
        sessions.upsertSession(theirs)
        presets.upsertPreset(WorkoutPreset(id = "p1", name = "Mine"))
        presets.upsertPreset(WorkoutPreset(id = "p2", name = "Theirs", profileId = "sibling"))

        assertEquals(listOf("mine"), sessions.getAllSessions(SELF_PROFILE_ID).first().map { it.session_id })
        assertEquals(listOf("theirs"), sessions.getAllSessions("sibling").first().map { it.session_id })
        assertNull(sessions.getSessionById(SELF_PROFILE_ID, "theirs"))
        assertNull(sessions.getSessionById("sibling", "mine"))

        assertEquals("Mine", presets.getAllPresets(SELF_PROFILE_ID).first().single().name)
        assertEquals("Theirs", presets.getAllPresets("sibling").first().single().name)
        assertEquals("Mine", presets.getPresetById(SELF_PROFILE_ID, "p1")?.name)
        assertNull(presets.getPresetById(SELF_PROFILE_ID, "p2"))

        sessions.deleteSessionById(SELF_PROFILE_ID, "theirs")
        presets.deletePresetById(SELF_PROFILE_ID, "p2")
        assertEquals(1, sessions.getAllSessions("sibling").first().size)
        assertEquals(1, presets.getAllPresets("sibling").first().size)
    }

    @Test
    fun insertSessionIfNoUnfinished_guardsEachProfileIndependently() = runBlocking {
        val dao = db.workoutSessionDao()
        val first = WorkoutSession(session_id = "first", status = WorkoutStatus.ACTIVE.name)
        val second = WorkoutSession(session_id = "second", status = WorkoutStatus.PAUSED.name)

        assertTrue(dao.insertSessionIfNoUnfinished(SELF_PROFILE_ID, first))
        assertFalse(dao.insertSessionIfNoUnfinished(SELF_PROFILE_ID, second))
        assertTrue(dao.insertSessionIfNoUnfinished("sibling", second))

        assertEquals(first.session_id, dao.getUnfinishedSession(SELF_PROFILE_ID)?.session_id)
        assertEquals(second.session_id, dao.getUnfinishedSession("sibling")?.session_id)
        assertEquals(SELF_PROFILE_ID, dao.getSessionById(SELF_PROFILE_ID, first.session_id)?.profileId)
        assertEquals("sibling", dao.getSessionById("sibling", second.session_id)?.profileId)
    }

    @Test
    fun clearAll_onlyDeletesTheRequestedProfile() = runBlocking {
        val sessions = db.workoutSessionDao()
        val presets = db.workoutPresetDao()
        sessions.upsertSession(WorkoutSession(session_id = "mine"))
        sessions.upsertSession(WorkoutSession(session_id = "theirs", profileId = "sibling"))
        presets.upsertPreset(WorkoutPreset(id = "mine", name = "Mine"))
        presets.upsertPreset(WorkoutPreset(id = "theirs", name = "Theirs", profileId = "sibling"))

        sessions.clearAll(SELF_PROFILE_ID)
        presets.clearAll(SELF_PROFILE_ID)

        assertTrue(sessions.getAllSessions(SELF_PROFILE_ID).first().isEmpty())
        assertTrue(presets.getAllPresets(SELF_PROFILE_ID).first().isEmpty())
        assertEquals(1, sessions.getAllSessions("sibling").first().size)
        assertEquals(1, presets.getAllPresets("sibling").first().size)
    }
}
