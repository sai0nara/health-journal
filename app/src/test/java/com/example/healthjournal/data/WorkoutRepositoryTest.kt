package com.example.healthjournal.data

import com.example.healthjournal.data.local.SELF_PROFILE_ID
import com.example.healthjournal.data.local.WorkoutSession
import com.example.healthjournal.data.local.WorkoutSessionDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WorkoutRepositoryTest {

    private lateinit var repository: WorkoutRepository
    private lateinit var activeProfile: FakeActiveProfileProvider
    private val dao: WorkoutSessionDao = mockk()

    @Before
    fun setup() {
        every { dao.getAllSessions(any()) } returns flowOf(emptyList())
        every { dao.getCompletedSessions(any()) } returns flowOf(emptyList())
        activeProfile = FakeActiveProfileProvider()
        repository = WorkoutRepository(dao, activeProfile)
    }

    @Test
    fun saveSession_stampsLastModifiedAndProfile() = runBlocking {
        val session = WorkoutSession()
        coEvery { dao.upsertSession(any()) } returns Unit

        val before = System.currentTimeMillis()
        repository.saveSession(session)
        val after = System.currentTimeMillis()

        coVerify {
            dao.upsertSession(withArg { saved ->
                assertTrue(saved.lastModified in before..after)
                assertEquals(SELF_PROFILE_ID, saved.profileId)
            })
        }
    }

    @Test
    fun saveSession_preservesWarmupMetadata() = runBlocking {
        val session = WorkoutSession(
            warmupName = "Rowing",
            warmupDurationSeconds = 300,
            warmupElapsedSeconds = 180,
            warmupOutcome = "SKIPPED"
        )
        coEvery { dao.upsertSession(any()) } returns Unit

        repository.saveSession(session)

        coVerify {
            dao.upsertSession(withArg { saved ->
                assertEquals("Rowing", saved.warmupName)
                assertEquals(300, saved.warmupDurationSeconds)
                assertEquals(180, saved.warmupElapsedSeconds)
                assertEquals("SKIPPED", saved.warmupOutcome)
            })
        }
    }

    @Test
    fun sessionsReloadForSwitchedProfile() = runBlocking {
        val me = WorkoutSession(session_id = "me")
        val sibling = WorkoutSession(session_id = "sibling")
        every { dao.getAllSessions(SELF_PROFILE_ID) } returns flowOf(listOf(me))
        every { dao.getAllSessions("sibling") } returns flowOf(listOf(sibling))

        assertEquals(me, repository.sessions.first().single())

        activeProfile.switchTo("sibling")

        assertEquals(sibling, repository.sessions.first().single())
    }

    @Test
    fun completedSessions_exposesCompletedOnlyDaoFlow() = runBlocking {
        val completed = listOf(WorkoutSession(), WorkoutSession())
        every { dao.getCompletedSessions(SELF_PROFILE_ID) } returns flowOf(completed)

        val result = repository.completedSessions.first()

        assertEquals(completed, result)
    }

    @Test
    fun getUnfinishedSession_returnsDaoResult() = runBlocking {
        val unfinished = WorkoutSession()
        coEvery { dao.getUnfinishedSession(SELF_PROFILE_ID) } returns unfinished
        assertEquals(unfinished, repository.getUnfinishedSession())

        coEvery { dao.getUnfinishedSession(SELF_PROFILE_ID) } returns null
        assertNull(repository.getUnfinishedSession())
    }

    @Test
    fun deleteSession_delegatesToDao() = runBlocking {
        coEvery { dao.deleteSessionById(any(), any()) } returns Unit

        repository.deleteSession("s1")

        coVerify { dao.deleteSessionById(SELF_PROFILE_ID, "s1") }
    }

    @Test
    fun saveSessionWithExplicitProfile_doesNotReassignToActiveProfile() = runBlocking {
        val session = WorkoutSession(profileId = "self")
        coEvery { dao.upsertSession(any()) } returns Unit

        activeProfile.switchTo("sibling")
        repository.saveSession("self", session)

        coVerify {
            dao.upsertSession(withArg { saved ->
                assertEquals("self", saved.profileId)
            })
        }
    }
}
