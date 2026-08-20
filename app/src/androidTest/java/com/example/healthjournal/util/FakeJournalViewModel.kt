package com.example.healthjournal.util

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.data.local.JournalEntry
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
    override val syncStatus: StateFlow<String?> = MutableStateFlow(null)
    override val searchQuery: MutableStateFlow<String> = MutableStateFlow("")
    override val archiveSearchQuery: MutableStateFlow<String> = MutableStateFlow("")
    override val isAscending: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val selectedTags: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())

    var addEntryCalledWith: AddEntryCall? = null
    var updateEntryCalledWith: Pair<JournalEntry, Set<String>>? = null
    var entryToReturn: JournalEntry? = null
    var restoredEntryId: String? = null
    var deletedEntriesIds: List<String>? = null
    var emptyArchiveCalled = false
    var signInCalled = false
    var syncNowCalled = false
    var healthAvailability = 1

    data class AddEntryCall(
        val description: String,
        val timestamp: Long,
        val photoUrls: List<String>,
        val attachments: List<AttachmentData>,
        val tags: Set<String>
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
        tags: Set<String>
    ) {
        addEntryCalledWith = AddEntryCall(description, timestamp, photoUrls, attachments, tags)
    }

    override fun updateEntry(entry: JournalEntry, tags: Set<String>) {
        updateEntryCalledWith = entry to tags
    }

    override suspend fun getEntryById(entryId: String): JournalEntry? = entryToReturn

    override fun signIn(activityContext: Context, onResolutionRequired: (PendingIntent) -> Unit) {
        signInCalled = true
    }

    override fun syncNow() {
        syncNowCalled = true
    }

    override fun signOut() {}

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

    override val healthPermissions: Set<String> = emptySet()
    override suspend fun hasHealthPermissions(): Boolean = false
    override fun checkHealthAvailability(): Int = healthAvailability
    override suspend fun syncHealthData(timestamp: Long): HealthSyncResult = HealthSyncResult()

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