package com.example.healthjournal.sync

import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.PersonalCard

/**
 * Pure merge logic for bidirectional journal sync.
 * Last-Write-Wins on lastModified per entry, with explicit tag handling:
 * - A cloud entry whose tags are null carries no tag information (legacy
 *   payload from a pre-tagging build) and must NOT wipe local tags.
 * - A cloud entry with a non-null tag array (even empty) wins the tag
 *   merge per last-write-wins when the cloud copy is the winner.
 */
object SyncMerge {
    suspend fun merge(
        cloudEntries: List<JournalEntry>,
        localEntries: List<JournalEntry>,
        localTagsForEntry: suspend (String) -> List<String>
    ): List<JournalEntry> {
        val result = mutableMapOf<String, JournalEntry>()

        cloudEntries.forEach { cloud ->
            result[cloud.entry_id] = cloud
        }

        localEntries.forEach { local ->
            val cloud = result[local.entry_id]
            val cloudWins = cloud != null && cloud.lastModified >= local.lastModified
            val localWins = !cloudWins
            if (localWins) {
                val tags = localTagsForEntry(local.entry_id)
                result[local.entry_id] = local.withTags(tags)
            } else if (cloud != null && cloud.tags == null) {
                // Cloud copy won but carries no tag info (legacy payload):
                // preserve local tags instead of wiping them.
                val tags = localTagsForEntry(local.entry_id)
                result[local.entry_id] = cloud.withTags(tags)
            }
        }

        return result.values.toList()
    }

    /**
     * Pure merge for body measurements: Last-Write-Wins on lastModified,
     * cloud wins ties (same convention as journal merge). Tombstones are
     * filtered by SyncWorker before merging, mirroring journal flow.
     */
    fun mergeMeasurements(
        cloudMeasurements: List<BodyMeasurementEntry>,
        localMeasurements: List<BodyMeasurementEntry>
    ): List<BodyMeasurementEntry> {
        val result = mutableMapOf<String, BodyMeasurementEntry>()

        cloudMeasurements.forEach { cloud ->
            result[cloud.entry_id] = cloud
        }

        localMeasurements.forEach { local ->
            val cloud = result[local.entry_id]
            if (cloud == null || local.lastModified > cloud.lastModified) {
                result[local.entry_id] = local
            }
        }

        return result.values.toList()
    }

    /**
     * Full-snapshot merge for goals: newest [GoalEntity.lastModified] wins
     * per parameter id (cloud wins ties). Local goals absent from the cloud
     * snapshot are pruned — propagating goal clears across devices without
     * needing a tombstone ledger.
     */
    fun mergeGoals(
        cloudGoals: List<GoalEntity>,
        localGoals: List<GoalEntity>
    ): List<GoalEntity> {
        val result = mutableMapOf<String, GoalEntity>()

        cloudGoals.forEach { cloud ->
            result[cloud.parameterId] = cloud
        }

        // Keep local copy only when cloud has the same parameter AND
        // the local edit is strictly newer (cloud wins ties).
        localGoals.forEach { local ->
            val cloud = result[local.parameterId]
            if (cloud != null && local.lastModified > cloud.lastModified) {
                result[local.parameterId] = local
            }
        }

        return result.values.toList()
    }

    /**
     * Merge for personal card: Last-Write-Wins on lastModified, cloud wins ties.
     * Since PersonalCard is a singleton (one row per user), this is a simple
     * LWW merge similar to goals.
     */
    fun mergePersonalCard(
        cloudCards: List<PersonalCard>,
        localCards: List<PersonalCard>
    ): List<PersonalCard> {
        val result = mutableMapOf<String, PersonalCard>()

        cloudCards.forEach { cloud ->
            result[cloud.id] = cloud
        }

        localCards.forEach { local ->
            val cloud = result[local.id]
            if (cloud == null || local.lastModified > cloud.lastModified) {
                result[local.id] = local
            }
        }

        return result.values.toList()
    }
}