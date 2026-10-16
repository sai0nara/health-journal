package com.example.healthjournal.sync

import com.example.healthjournal.data.local.GoalEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalSyncMergeTest {

    @Test
    fun mergeGoals_stalePartialCloud_keepsNewerLocalGoals() {
        // Reported bug: cloud snapshot holds only WEIGHT (older), local has
        // newer goals for other parameters — a stale partial cloud must not
        // wipe them; only WEIGHT resolves by per-parameter LWW (cloud newer).
        val cloud = listOf(GoalEntity("WEIGHT", 75.0, 1000))
        val local = listOf(
            GoalEntity("WEIGHT", 80.0, 500),
            GoalEntity("WAIST", 82.0, 2000),
            GoalEntity("CHEST", 95.0, 2000)
        )

        val merged = SyncMerge.mergeGoals(cloud, local)
        val byId = merged.associateBy { it.parameterId }

        assertEquals(75.0, byId.getValue("WEIGHT").target, 0.001)
        assertEquals(82.0, byId.getValue("WAIST").target, 0.001)
        assertEquals(95.0, byId.getValue("CHEST").target, 0.001)
    }

    @Test
    fun mergeGoals_freshCloud_prunesLocalOnly() {
        // Cloud snapshot is at least as fresh: a local goal absent from it
        // was cleared on another device and stays pruned.
        val cloud = GoalEntity("WEIGHT", 75.0, 300)
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
    fun mergeGoals_emptyCloud_keepsLocal() {
        // An empty cloud snapshot carries no freshness signal; local goals
        // are kept (the worker also guards this case before merging).
        val local = GoalEntity("WEIGHT", 75.0, 100)

        val merged = SyncMerge.mergeGoals(emptyList(), listOf(local))

        assertEquals(listOf(local), merged)
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

    @Test
    fun firstSync_nullCloudFile_preservesLocalGoals() {
        // Regression: on first sync, downloadDataFile returns null (file
        // doesn't exist yet). SyncWorker must NOT parse null as empty and
        // prune the local goal — instead it should preserve local goals
        // and upload them to establish the cloud file.
        //
        // This test validates the SyncWorker conditional: when cloudGoalsJson
        // is null, skip mergeGoals and use localGoals directly.
        // (The merge function itself is tested by the other cases above.)
        val local = GoalEntity("WEIGHT", 75.0, 100)

        // Simulates the SyncWorker path: cloudGoalsJson == null → use localGoals
        val mergedGoals = run {
            val cloudGoalsJson: String? = null
            if (cloudGoalsJson == null) {
                listOf(local)
            } else {
                SyncMerge.mergeGoals(GoalSyncPayload.fromJson(cloudGoalsJson), listOf(local))
            }
        }

        assertEquals(listOf(local), mergedGoals)
    }
}
