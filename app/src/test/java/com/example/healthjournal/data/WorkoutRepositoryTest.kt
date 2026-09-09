package com.example.healthjournal.data

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
    private val dao: WorkoutSessionDao = mockk()

    @Before
    fun setup() {
        coEvery { dao.getAllSessions() } returns flowOf(emptyList())
        every { dao.getCompletedSessions() } returns flowOf(emptyList())
        repository = WorkoutRepository(dao)
    }

    @Test
    fun saveSession_stampsLastModifiedAndUpserts() = runBlocking {
        val session = WorkoutSession()
        coEvery { dao.upsertSession(any()) } returns Unit

        val before = System.currentTimeMillis()
        repository.saveSession(session)
        val after = System.currentTimeMillis()

        coVerify {
            dao.upsertSession(withArg { saved ->
                assertTrue(saved.lastModified in before..after)
            })
        }
    }

    @Test
    fun sessions_exposesDaoFlow() = runBlocking {
        val sessions = listOf(WorkoutSession(), WorkoutSession())
        coEvery { dao.getAllSessions() } returns flowOf(sessions)

        val result = WorkoutRepository(dao).sessions.first()

        assertEquals(sessions, result)
    }

    @Test
    fun completedSessions_exposesCompletedOnlyDaoFlow() = runBlocking {
        val completed = listOf(WorkoutSession(), WorkoutSession())
        every { dao.getCompletedSessions() } returns flowOf(completed)

        val result = WorkoutRepository(dao).completedSessions.first()

        assertEquals(completed, result)
    }

    @Test
    fun getUnfinishedSession_returnsDaoResult() = runBlocking {
        val unfinished = WorkoutSession()
        coEvery { dao.getUnfinishedSession() } returns unfinished
        assertEquals(unfinished, repository.getUnfinishedSession())

        coEvery { dao.getUnfinishedSession() } returns null
        assertNull(WorkoutRepository(dao).getUnfinishedSession())
    }

    @Test
    fun deleteSession_delegatesToDao() = runBlocking {
        coEvery { dao.deleteSessionById(any()) } returns Unit

        repository.deleteSession("s1")

        coVerify { dao.deleteSessionById("s1") }
    }
}
