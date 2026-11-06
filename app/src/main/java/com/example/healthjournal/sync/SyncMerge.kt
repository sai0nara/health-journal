package com.example.healthjournal.sync

import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.EntryTagCrossRef
import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.PersonalCard
import com.example.healthjournal.data.local.ProfileDeletionEntity
import com.example.healthjournal.data.local.ProfileEntity
import com.example.healthjournal.data.local.ProfileRelationship
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import com.example.healthjournal.domain.TagIdentity

data class ProfileMergeResult(
    val profiles: List<ProfileEntity>,
    val deletions: List<ProfileDeletionEntity>,
    val deletedProfileIds: Set<String>
)

/**
 * Pure merge logic for bidirectional journal sync.
 * Last-Write-Wins on lastModified per entry, with explicit tag handling:
 * - A cloud entry whose tags are null carries no tag information (legacy
 *   payload from a pre-tagging build) and must NOT wipe local tags.
 * - A cloud entry with a non-null tag array (even empty) wins the tag
 *   merge per last-write-wins when the cloud copy is the winner.
 */
object SyncMerge {

    /** Cross-refs for [entryId] in canonical form, one per distinct tag. */
    fun canonicalTagRefs(entryId: String, tags: Iterable<String>): List<EntryTagCrossRef> =
        TagIdentity.canonicalDistinct(tags).map { EntryTagCrossRef(entryId, it) }

    fun canonicalTagRefs(
        @Suppress("UNUSED_PARAMETER") profileId: String,
        entryId: String,
        tags: Iterable<String>
    ): List<EntryTagCrossRef> =
        canonicalTagRefs(entryId, tags)

    fun mergeProfiles(
        cloudProfiles: List<ProfileEntity>,
        localProfiles: List<ProfileEntity>,
        cloudDeletions: List<ProfileDeletionEntity> = emptyList(),
        localDeletions: List<ProfileDeletionEntity> = emptyList()
    ): ProfileMergeResult {
        val mergedDeletions = mutableMapOf<String, ProfileDeletionEntity>()
        (cloudDeletions + localDeletions).forEach { marker ->
            if (marker.profileId == SELF_PROFILE_ID) return@forEach
            val existing = mergedDeletions[marker.profileId]
            if (existing == null || marker.deletedAt > existing.deletedAt) {
                mergedDeletions[marker.profileId] = marker
            }
        }

        val candidates = mutableMapOf<String, ProfileEntity>()
        cloudProfiles.forEach { cloud ->
            candidates[cloud.id] = cloud
        }
        localProfiles.forEach { local ->
            val cloud = candidates[local.id]
            if (cloud == null || local.lastModified > cloud.lastModified) {
                candidates[local.id] = local
            }
        }

        val deletedIds = mutableSetOf<String>()
        val surviving = mutableListOf<ProfileEntity>()
        candidates.values.forEach { profile ->
            val deletion = mergedDeletions[profile.id]
            if (profile.id != SELF_PROFILE_ID &&
                deletion != null &&
                deletion.deletedAt >= profile.lastModified
            ) {
                deletedIds += profile.id
            } else {
                surviving += profile
            }
        }
        mergedDeletions.keys.forEach { id ->
            if (id != SELF_PROFILE_ID && surviving.none { it.id == id }) {
                deletedIds += id
            }
        }

        if (surviving.none { it.id == SELF_PROFILE_ID || it.relationship == ProfileRelationship.SELF.name }) {
            val now = System.currentTimeMillis()
            surviving += ProfileEntity(
                id = SELF_PROFILE_ID,
                name = "Me",
                relationship = ProfileRelationship.SELF.name,
                createdAt = now,
                lastModified = now,
                isSynced = true,
                syncStatus = "SYNCED"
            )
        }

        return ProfileMergeResult(
            profiles = surviving,
            deletions = mergedDeletions.values.toList(),
            deletedProfileIds = deletedIds
        )
    }

    @Suppress("UNNECESSARY_SAFE_CALL", "UNNECESSARY_ELVIS", "USELESS_ELVIS")
    suspend fun merge(
        cloudEntries: List<JournalEntry>,
        localEntries: List<JournalEntry>,
        localTagsForEntry: suspend (String) -> List<String>
    ): List<JournalEntry> {
        val result = mutableMapOf<Pair<String, String>, JournalEntry>()

        cloudEntries.forEach { rawCloud ->
            val profileId = (rawCloud.profileId as String?)?.takeIf { it.isNotBlank() } ?: SELF_PROFILE_ID
            val cloud = if (rawCloud.profileId == profileId) rawCloud else rawCloud.copy(profileId = profileId).also { it.tags = rawCloud.tags }
            result[profileId to cloud.entry_id] = cloud
        }

        localEntries.forEach { rawLocal ->
            val profileId = (rawLocal.profileId as String?)?.takeIf { it.isNotBlank() } ?: SELF_PROFILE_ID
            val local = if (rawLocal.profileId == profileId) rawLocal else rawLocal.copy(profileId = profileId).also { it.tags = rawLocal.tags }
            val key = profileId to local.entry_id
            val cloud = result[key]
            val cloudWins = cloud != null && cloud.lastModified >= local.lastModified
            val localWins = !cloudWins
            if (localWins) {
                val tags = localTagsForEntry(local.entry_id)
                result[key] = local.withTags(tags)
            } else if (cloud != null && cloud.tags == null) {
                // Cloud copy won but carries no tag info (legacy payload):
                // preserve local tags instead of wiping them.
                val tags = localTagsForEntry(local.entry_id)
                result[key] = cloud.withTags(tags)
            }
        }

        return result.values.toList()
    }

    /**
     * Pure merge for body measurements: Last-Write-Wins on lastModified,
     * cloud wins ties (same convention as journal merge). Tombstones are
     * filtered by SyncWorker before merging, mirroring journal flow.
     */
    @Suppress("UNNECESSARY_SAFE_CALL", "UNNECESSARY_ELVIS", "USELESS_ELVIS")
    fun mergeMeasurements(
        cloudMeasurements: List<BodyMeasurementEntry>,
        localMeasurements: List<BodyMeasurementEntry>
    ): List<BodyMeasurementEntry> {
        val result = mutableMapOf<Pair<String, String>, BodyMeasurementEntry>()

        cloudMeasurements.forEach { rawCloud ->
            val profileId = (rawCloud.profileId as String?)?.takeIf { it.isNotBlank() } ?: SELF_PROFILE_ID
            val cloud = if (rawCloud.profileId == profileId) rawCloud else rawCloud.copy(profileId = profileId)
            result[profileId to cloud.entry_id] = cloud
        }

        localMeasurements.forEach { rawLocal ->
            val profileId = (rawLocal.profileId as String?)?.takeIf { it.isNotBlank() } ?: SELF_PROFILE_ID
            val local = if (rawLocal.profileId == profileId) rawLocal else rawLocal.copy(profileId = profileId)
            val key = profileId to local.entry_id
            val cloud = result[key]
            if (cloud == null || local.lastModified > cloud.lastModified) {
                result[key] = local
            }
        }

        return result.values.toList()
    }

    /**
     * Full-snapshot merge for goals: newest [GoalEntity.lastModified] wins
     * per (profileId, parameterId) (cloud wins ties). A local goal absent from
     * the cloud snapshot for that profile is pruned only when that profile's
     * cloud snapshot is at least as fresh — propagating genuine goal clears
     * without letting a stale partial cloud or another profile's snapshot wipe
     * newer local goals.
     */
    @Suppress("UNNECESSARY_SAFE_CALL", "UNNECESSARY_ELVIS", "USELESS_ELVIS")
    fun mergeGoals(
        cloudGoals: List<GoalEntity>,
        localGoals: List<GoalEntity>
    ): List<GoalEntity> {
        val normalizedCloud = cloudGoals.map { raw ->
            val profileId = (raw.profileId as String?)?.takeIf { it.isNotBlank() } ?: SELF_PROFILE_ID
            if (raw.profileId == profileId) raw else raw.copy(profileId = profileId)
        }
        val normalizedLocal = localGoals.map { raw ->
            val profileId = (raw.profileId as String?)?.takeIf { it.isNotBlank() } ?: SELF_PROFILE_ID
            if (raw.profileId == profileId) raw else raw.copy(profileId = profileId)
        }

        val result = mutableMapOf<Pair<String, String>, GoalEntity>()

        normalizedCloud.forEach { cloud ->
            result[cloud.profileId to cloud.parameterId] = cloud
        }

        val cloudFreshByProfile = normalizedCloud
            .groupBy { it.profileId }
            .mapValues { (_, list) -> list.maxOfOrNull { it.lastModified } }
        val localFreshByProfile = normalizedLocal
            .groupBy { it.profileId }
            .mapValues { (_, list) -> list.maxOfOrNull { it.lastModified } }

        normalizedLocal.forEach { local ->
            val key = local.profileId to local.parameterId
            val cloud = result[key]
            val cloudFresh = cloudFreshByProfile[local.profileId]
            val localFresh = localFreshByProfile[local.profileId]
            val pruneAbsent = cloudFresh != null && (localFresh == null || cloudFresh >= localFresh)
            if (cloud != null) {
                // Keep local copy only when the local edit is strictly newer
                // (cloud wins ties).
                if (local.lastModified > cloud.lastModified) {
                    result[key] = local
                }
            } else if (!pruneAbsent) {
                result[key] = local
            }
        }

        return result.values.toList()
    }

    /**
     * Merge for personal card: Last-Write-Wins on lastModified per (profileId, id),
     * cloud wins ties.
     */
    @Suppress("UNNECESSARY_SAFE_CALL", "UNNECESSARY_ELVIS", "USELESS_ELVIS")
    fun mergePersonalCard(
        cloudCards: List<PersonalCard>,
        localCards: List<PersonalCard>
    ): List<PersonalCard> {
        val result = mutableMapOf<Pair<String, String>, PersonalCard>()

        cloudCards.forEach { rawCloud ->
            val profileId = (rawCloud.profileId as String?)?.takeIf { it.isNotBlank() } ?: SELF_PROFILE_ID
            val id = (rawCloud.id as String?)?.takeIf { it.isNotBlank() } ?: "personal_card"
            val cloud = if (rawCloud.profileId == profileId && rawCloud.id == id) rawCloud else rawCloud.copy(profileId = profileId, id = id)
            result[profileId to id] = cloud
        }

        localCards.forEach { rawLocal ->
            val profileId = (rawLocal.profileId as String?)?.takeIf { it.isNotBlank() } ?: SELF_PROFILE_ID
            val id = (rawLocal.id as String?)?.takeIf { it.isNotBlank() } ?: "personal_card"
            val local = if (rawLocal.profileId == profileId && rawLocal.id == id) rawLocal else rawLocal.copy(profileId = profileId, id = id)
            val key = profileId to id
            val cloud = result[key]
            if (cloud == null || local.lastModified > cloud.lastModified) {
                result[key] = local
            }
        }

        return result.values.toList()
    }
}
