package com.example.healthjournal.util

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.ProfileEntity
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import com.example.healthjournal.domain.EntryVitals
import com.example.healthjournal.viewmodel.HealthSyncResult
import com.example.healthjournal.viewmodel.IJournalViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Shared in-memory implementation of [IJournalViewModel] for UI tests.
 * Records calls so tests can assert on them.
 */
open class FakeJournalViewModel : IJournalViewModel {
    override val allEntries: MutableStateFlow<List<JournalEntry>> = MutableStateFlow(emptyList())
    override val archivedEntries: MutableStateFlow<List<JournalEntry>> = MutableStateFlow(emptyList())
    override val reactiveArchivedEntries: MutableStateFlow<List<JournalEntry>> = MutableStateFlow(emptyList())
    override val isUserSignedIn: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val userEmail: MutableStateFlow<String?> = MutableStateFlow(null)
    override val syncStatus: StateFlow<String?> = MutableStateFlow(null)
    override val isManualSyncActive: StateFlow<Boolean> = MutableStateFlow(false)
    override val authRequired: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val searchQuery: MutableStateFlow<String> = MutableStateFlow("")
    override val archiveSearchQuery: MutableStateFlow<String> = MutableStateFlow("")
    override val isAscending: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val selectedTags: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    override val tagsByEntry: MutableStateFlow<Map<String, List<String>>> = MutableStateFlow(emptyMap())
    override val activeProfileId: MutableStateFlow<String> = MutableStateFlow(SELF_PROFILE_ID)
    override val activeProfile: MutableStateFlow<ProfileEntity?> = MutableStateFlow(null)
    override val dateFilter: MutableStateFlow<com.example.healthjournal.domain.HistoryDateFilter> =
        MutableStateFlow(com.example.healthjournal.domain.HistoryDateFilter.All)
    override val visibleMonth: MutableStateFlow<java.time.YearMonth> =
        MutableStateFlow(java.time.YearMonth.now())
    override val daysWithEntriesInVisibleMonth: MutableStateFlow<Set<java.time.LocalDate>> =
        MutableStateFlow(emptySet())

    var addEntryCalledWith: AddEntryCall? = null
    var updateEntryCalledWith: Pair<JournalEntry, Set<String>>? = null
    var updateEntryProfileId: String? = null
    var updateVitalsCalledWith: Pair<String, EntryVitals>? = null
    var entryToReturn: JournalEntry? = null
    var restoredEntryId: String? = null
    var deletedEntriesIds: List<String>? = null
    var emptyArchiveCalled = false
    var signInCalled = false
    var signOutCalled = false
    var syncNowCalled = false
    var requestDriveAuthCalled = false
    var healthAvailability = 1
    var healthPermissionsGranted = false
    var healthSyncResult: HealthSyncResult = HealthSyncResult()
    var syncHealthDataCalled = false
    var syncHealthDataTargetProfileId: String? = null

    data class AddEntryCall(
        val description: String,
        val timestamp: Long,
        val photoUrls: List<String>,
        val attachments: List<AttachmentData>,
        val tags: Set<String>,
        val bpSystolic: Double? = null,
        val bpDiastolic: Double? = null,
        val heartRate: Int? = null,
        val sleepHours: Float? = null,
        val bloodGlucoseMmol: Double? = null,
        val bodyTemperatureC: Double? = null,
        val spo2Percent: Int? = null,
        val profileId: String
    )

    override fun addEntry(
        description: String,
        timestamp: Long,
        photoUrls: List<String>,
        attachments: List<AttachmentData>,
        bpSystolic: Double?,
        bpDiastolic: Double?,
        heartRate: Int?,
        sleepHours: Float?,
        bloodGlucoseMmol: Double?,
        bodyTemperatureC: Double?,
        spo2Percent: Int?,
        tags: Set<String>,
        profileId: String
    ) {
        addEntryCalledWith = AddEntryCall(
            description = description,
            timestamp = timestamp,
            photoUrls = photoUrls,
            attachments = attachments,
            tags = tags,
            bpSystolic = bpSystolic,
            bpDiastolic = bpDiastolic,
            heartRate = heartRate,
            sleepHours = sleepHours,
            bloodGlucoseMmol = bloodGlucoseMmol,
            bodyTemperatureC = bodyTemperatureC,
            spo2Percent = spo2Percent,
            profileId = profileId
        )
    }

    override fun updateEntry(entry: JournalEntry, tags: Set<String>, profileId: String) {
        updateEntryCalledWith = entry to tags
        updateEntryProfileId = profileId
    }

    override fun updateVitals(entryId: String, vitals: EntryVitals) {
        updateVitalsCalledWith = entryId to vitals
        allEntries.value = allEntries.value.map { entry ->
            if (entry.entry_id != entryId) {
                entry
            } else {
                entry.copy(
                    bp_systolic = vitals.bpSystolic,
                    bp_diastolic = vitals.bpDiastolic,
                    heart_rate_avg = vitals.heartRate,
                    blood_glucose_mmol = vitals.bloodGlucoseMmol,
                    body_temperature_c = vitals.bodyTemperatureC,
                    spo2_percent = vitals.spo2Percent
                )
            }
        }
    }

    override suspend fun getEntryById(entryId: String): JournalEntry? = entryToReturn

    override fun signIn(activityContext: Context, onResolutionRequired: (PendingIntent) -> Unit) {
        signInCalled = true
    }

    override fun requestDriveAuth(onResolutionRequired: (PendingIntent) -> Unit) {
        requestDriveAuthCalled = true
    }

    override fun syncNow() {
        syncNowCalled = true
    }

    override fun signOut() {
        signOutCalled = true
    }

    override fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    override fun setArchiveSearchQuery(query: String) {
        archiveSearchQuery.value = query
    }

    override fun setSortOrder(isAsc: Boolean) {
        isAscending.value = isAsc
    }

    override fun toggleTag(tag: String) {
        selectedTags.value = if (tag in selectedTags.value) {
            selectedTags.value - tag
        } else {
            selectedTags.value + tag
        }
    }

    override fun setDateFilter(filter: com.example.healthjournal.domain.HistoryDateFilter) {
        dateFilter.value = filter
    }

    override fun clearDateFilter() {
        dateFilter.value = com.example.healthjournal.domain.HistoryDateFilter.All
    }

    override fun setVisibleMonth(yearMonth: java.time.YearMonth) {
        visibleMonth.value = yearMonth
    }

    override val healthPermissions: Set<String> = emptySet()
    /**
     * Test-controlled opt-in gate. Defaults to enabled so existing UI tests
     * exercise the Sync Health action; tests for the disabled state set this
     * to false explicitly.
     */
    override val isHealthSyncEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override fun refreshHealthSyncSetting() {}
    override fun setHealthSyncEnabled(enabled: Boolean) {
        isHealthSyncEnabled.value = enabled
    }
    override suspend fun hasHealthPermissions(): Boolean = healthPermissionsGranted
    override fun checkHealthAvailability(): Int = healthAvailability
    override suspend fun syncHealthData(timestamp: Long, targetProfileId: String): HealthSyncResult {
        syncHealthDataCalled = true
        syncHealthDataTargetProfileId = targetProfileId
        return healthSyncResult.copy(targetProfileId = targetProfileId)
    }

    override fun archiveEntry(entryId: String) {}

    override fun restoreEntry(entryId: String) {
        restoredEntryId = entryId
    }

    override fun deleteEntries(entryIds: List<String>) {
        deletedEntriesIds = entryIds
    }

    override fun emptyArchive() {
        emptyArchiveCalled = true
    }

    override suspend fun savePersistentFile(uri: Uri, isPhoto: Boolean): String? = null
}