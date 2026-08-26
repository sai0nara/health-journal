package com.example.healthjournal.data

import com.example.healthjournal.data.local.GoalDao
import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.domain.MeasurementField
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for GoalsRepository: thin DAO delegation plus timestamping of
 * goal writes so cross-device sync can resolve conflicts newest-wins.
 */
class GoalsRepositoryTest {

    private val dao: GoalDao = mockk(relaxed = true)
    private val repository = GoalsRepository(dao)

    @Test
    fun setGoal_upsertsWithParameterIdAndFreshTimestamp() = runTest {
        val before = System.currentTimeMillis()

        repository.setGoal(MeasurementField.WAIST, 82.0)

        val captured = slot<GoalEntity>()
        coVerify { dao.upsertGoal(capture(captured)) }
        assertEquals("WAIST", captured.captured.parameterId)
        assertEquals(82.0, captured.captured.target, 0.001)
        assertTrue(captured.captured.lastModified >= before)
    }

    @Test
    fun clearGoal_delegatesToDeleteById() = runTest {
        repository.clearGoal("WEIGHT")

        coVerify(exactly = 1) { dao.deleteById("WEIGHT") }
    }

    @Test
    fun clearAll_delegatesToDaoWipe() = runTest {
        repository.clearAll()

        coVerify(exactly = 1) { dao.clear() }
    }

    @Test
    fun getAll_returnsDaoSnapshot() = runTest {
        val rows = listOf(GoalEntity("WAIST", 80.0, 1_000L))
        io.mockk.coEvery { dao.getAll() } returns rows

        assertEquals(rows, repository.getAll())
    }

    @Test
    fun goalsFlow_passesThroughDaoObservable() {
        val upstream = MutableStateFlow(listOf(GoalEntity("WEIGHT", 75.0, 1_000L)))
        io.mockk.every { dao.observeAll() } returns upstream

        // Construct after stubbing: the val captures the DAO flow eagerly.
        assertEquals(upstream, GoalsRepository(dao).goals)
    }
}
