package com.example.healthjournal.data

import com.example.healthjournal.data.local.GoalDao
import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import com.example.healthjournal.domain.MeasurementField
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for GoalsRepository: profile-scoped DAO delegation plus
 * timestamping of goal writes so sync can resolve conflicts newest-wins.
 */
class GoalsRepositoryTest {

    private val dao: GoalDao = mockk(relaxed = true)
    private val activeProfile = FakeActiveProfileProvider()
    private val repository = GoalsRepository(dao, activeProfile)

    @Test
    fun setGoal_upsertsWithParameterIdAndFreshTimestamp() = runTest {
        val before = System.currentTimeMillis()

        repository.setGoal(MeasurementField.WAIST, 82.0)

        val captured = slot<GoalEntity>()
        coVerify { dao.upsertGoal(capture(captured)) }
        assertEquals("WAIST", captured.captured.parameterId)
        assertEquals(82.0, captured.captured.target, 0.001)
        assertEquals(SELF_PROFILE_ID, captured.captured.profileId)
        assertTrue(captured.captured.lastModified >= before)
    }

    @Test
    fun clearGoal_delegatesToDeleteById() = runTest {
        repository.clearGoal("WEIGHT")

        coVerify(exactly = 1) { dao.deleteById(SELF_PROFILE_ID, "WEIGHT") }
    }

    @Test
    fun clearAll_delegatesToProfileWipe() = runTest {
        repository.clearAll(SELF_PROFILE_ID)

        coVerify(exactly = 1) { dao.clear(SELF_PROFILE_ID) }
    }

    @Test
    fun getAll_returnsDaoSnapshot() = runTest {
        val rows = listOf(GoalEntity("WAIST", 80.0, 1_000L))
        coEvery { dao.getAll(SELF_PROFILE_ID) } returns rows

        assertEquals(rows, repository.getAll())
    }

    @Test
    fun goalsReloadForSwitchedProfile() = runTest {
        val meGoals = listOf(GoalEntity("WAIST", 80.0, 1_000L))
        val siblingGoals = listOf(GoalEntity("WAIST", 65.0, 2_000L))
        every { dao.observeAll(SELF_PROFILE_ID) } returns flowOf(meGoals)
        every { dao.observeAll("sibling") } returns flowOf(siblingGoals)

        assertEquals(meGoals, repository.goals.first())

        activeProfile.switchTo("sibling")

        assertEquals(siblingGoals, repository.goals.first())
    }

    @Test
    fun workoutWeeklyTarget_defaultsToFourWhenUnset() = runTest {
        every { dao.observeAll(SELF_PROFILE_ID) } returns flowOf(emptyList())

        assertEquals(4, repository.workoutWeeklyTarget.first())
    }

    @Test
    fun setWorkoutWeeklyTarget_persistsForActiveProfileWithinRange() = runTest {
        repository.setWorkoutWeeklyTarget(6)

        val captured = slot<GoalEntity>()
        coVerify { dao.upsertGoal(capture(captured)) }
        assertEquals(GoalsRepository.WORKOUT_WEEKLY_TARGET, captured.captured.parameterId)
        assertEquals(6.0, captured.captured.target, 0.001)
        assertEquals(SELF_PROFILE_ID, captured.captured.profileId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun setWorkoutWeeklyTarget_rejectsZero() = runTest {
        repository.setWorkoutWeeklyTarget(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun setWorkoutWeeklyTarget_rejectsAboveFourteen() = runTest {
        repository.setWorkoutWeeklyTarget(15)
    }

    @Test
    fun workoutWeeklyTarget_reloadsPerProfile() = runTest {
        every { dao.observeAll(SELF_PROFILE_ID) } returns flowOf(
            listOf(GoalEntity(GoalsRepository.WORKOUT_WEEKLY_TARGET, 5.0, 1_000L, SELF_PROFILE_ID))
        )
        every { dao.observeAll("sibling") } returns flowOf(
            listOf(GoalEntity(GoalsRepository.WORKOUT_WEEKLY_TARGET, 2.0, 2_000L, "sibling"))
        )

        assertEquals(5, repository.workoutWeeklyTarget.first())

        activeProfile.switchTo("sibling")

        assertEquals(2, repository.workoutWeeklyTarget.first())
    }
}
