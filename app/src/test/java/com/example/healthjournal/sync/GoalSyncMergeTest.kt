package com.example.healthjournal.sync

import com.example.healthjournal.data.local.GoalEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalSyncMergeTest {

    @Test
    fun mergeGoals_disjointIds_prunesLocalOnly() {
        // Local goal absent from cloud snapshot is pruned (goal was cleared on
        // another device); only cloud-present parameters survive.
        val cloud = GoalEntity("WEIGHT", 75.0, 100)
        val local = GoalEntity("WAIST", 82.0, 200)

        val merged = SyncMerge.mergeGoals(listOf(cloud), listOf(local))

        assertEquals(setOf("WEIGHT"), merged.map { it.parameterId }.toSet())
    }

    @Test
    fun mergeGoals_cloudNewer_cloudWins() {
        val cloud = GoalEntity("WEIGHT", 75.0, 2000)
        val local = GoalEntity("WEIGHT", 80.0, 1000)

        val merged = SyncMerge.mergeGoals(listOf(cloud), listOf(local))

        assertEquals(75.0, merged.single().target, 0.001)
    }

    @Test
    fun mergeGoals_localNewer_localWins() {
        val cloud = GoalEntity("WEIGHT", 75.0, 1000)
        val local = GoalEntity("WEIGHT", 80.0, 2000)

        val merged = SyncMerge.mergeGoals(listOf(cloud), listOf(local))

        assertEquals(80.0, merged.single().target, 0.001)
    }

    @Test
    fun mergeGoals_tie_cloudWins() {
        val cloud = GoalEntity("WEIGHT", 75.0, 1500)
        val local = GoalEntity("WEIGHT", 80.0, 1500)

        val merged = SyncMerge.mergeGoals(listOf(cloud), listOf(local))

        assertEquals(75.0, merged.single().target, 0.001)
    }

    @Test
    fun mergeGoals_emptyCloud_prunesLocal() {
        val local = GoalEntity("WEIGHT", 75.0, 100)

        val merged = SyncMerge.mergeGoals(emptyList(), listOf(local))

        assertTrue(merged.isEmpty())
    }

    @Test
    fun mergeGoals_emptyLocal_keepsCloud() {
        val cloud = GoalEntity("WEIGHT", 75.0, 100)

        val merged = SyncMerge.mergeGoals(listOf(cloud), emptyList())

        assertEquals("WEIGHT", merged.single().parameterId)
    }

    @Test
    fun mergeGoals_emptyBoth_emptyResult() {
        val merged = SyncMerge.mergeGoals(emptyList(), emptyList())

        assertTrue(merged.isEmpty())
    }
}
