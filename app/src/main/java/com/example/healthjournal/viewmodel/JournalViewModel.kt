package com.example.healthjournal.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.healthjournal.auth.GoogleAuthManager
import com.example.healthjournal.auth.SessionManager
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.health.HealthConnectManager
import com.example.healthjournal.media.AndroidMediaCompressionService
import com.example.healthjournal.media.MediaCompressionService
import com.example.healthjournal.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HealthSyncResult(
    val bpSystolic: Double? = null,
    val bpDiastolic: Double? = null,
    val heartRate: Int? = null,
    val sleepHours: Float? = null
)

interface IJournalViewModel {
    val allEntries: StateFlow<List<JournalEntry>>
    val archivedEntries: StateFlow<List<JournalEntry>>
    val reactiveArchivedEntries: StateFlow<List<JournalEntry>>
    val isUserSignedIn: StateFlow<Boolean>
    val syncStatus: StateFlow<String?>
    val searchQuery: StateFlow<String>
    val archiveSearchQuery: StateFlow<String>
    val isAscending: StateFlow<Boolean>
    val selectedTags: StateFlow<Set<String>>

    fun addEntry(
        description: String,
        timestamp: Long = System.currentTimeMillis(),
        photoUrls: List<String> = emptyList(),
        attachments: List<AttachmentData> = emptyList(),
        bpSystolic: Double? = null,
        bpDiastolic: Double? = null,
        heartRate: Int? = null,
        sleepHours: Float? = null,
        tags: Set<String> = emptySet()
    )
    fun updateEntry(entry: JournalEntry, tags: Set<String> = emptySet())
    suspend fun getEntryById(entryId: String): JournalEntry?
    fun signIn(activityContext: Context, onResolutionRequired: (android.app.PendingIntent) -> Unit)
    fun syncNow()
    fun signOut()

    fun setSearchQuery(query: String)
    fun setArchiveSearchQuery(query: String)
    fun setSortOrder(isAsc: Boolean)
    fun toggleTag(tag: String)

    // Health Connect
    val healthPermissions: Set<String>
    suspend fun hasHealthPermissions(): Boolean
    fun checkHealthAvailability(): Int
    suspend fun syncHealthData(timestamp: Long): HealthSyncResult

    // Archive & Delete
    fun archiveEntry(entryId: String)
    fun restoreEntry(entryId: String)
    fun deleteEntries(entryIds: List<String>)
    fun emptyArchive()
    suspend fun savePersistentFile(uri: android.net.Uri, isPhoto: Boolean): String?
}

class JournalViewModel(
    application: Application,
    private val repository: JournalRepository,
    private val authManager: GoogleAuthManager,
    private val sessionManager: SessionManager,
    private val healthManager: HealthConnectManager,
    private val mediaService: MediaCompressionService,
    private val ioContext: kotlin.coroutines.CoroutineContext = Dispatchers.IO
) : AndroidViewModel(application), IJournalViewModel {

    private val _searchQuery = MutableStateFlow("")
    override val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isAscending = MutableStateFlow(false) // Default Descending (newest first)
    override val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    private val _selectedTags = MutableStateFlow(setOf<String>())
    override val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val allEntries: StateFlow<List<JournalEntry>> = combine(
        _searchQuery, _isAscending, _selectedTags
    ) { query, isAsc, tags ->
        Triple(query, isAsc, tags)
    }.flatMapLatest { (query, isAsc, tags) ->
        if (tags.isEmpty()) {
            if (query.isBlank()) {
                repository.getEntriesSortedByDate(isAsc)
            } else {
                repository.searchEntries(query, isAsc)
            }
        } else {
            repository.searchEntriesWithTags(query, tags.toList(), isAsc)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    override val archivedEntries: StateFlow<List<JournalEntry>> = repository.archivedEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _archiveSearchQuery = MutableStateFlow("")
    override val archiveSearchQuery: StateFlow<String> = _archiveSearchQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val reactiveArchivedEntries: StateFlow<List<JournalEntry>> = combine(_archiveSearchQuery, _selectedTags) { query, tags ->
        query to tags
    }.flatMapLatest { (query, tags) ->
        if (tags.isEmpty()) {
            if (query.isBlank()) {
                repository.archivedEntries
            } else {
                repository.searchArchivedEntries(query)
            }
        } else {
            repository.searchArchivedEntriesWithTags(query, tags.toList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isUserSignedIn = MutableStateFlow(sessionManager.getUserEmail() != null)
    override val isUserSignedIn: StateFlow<Boolean> = _isUserSignedIn.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    override val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    private val TAG = "JournalViewModel"

    init {
        // Observe WorkManager for the sync work names actually used by SyncManager
        viewModelScope.launch {
            val workManager = WorkManager.getInstance(getApplication())
            merge(
                workManager.getWorkInfosForUniqueWorkFlow(SyncManager.PERIODIC_WORK_NAME),
                workManager.getWorkInfosForUniqueWorkFlow(SyncManager.MANUAL_WORK_NAME)
            ).collect { workInfos ->
                val info = workInfos.firstOrNull()
                _syncStatus.value = when (info?.state) {
                    WorkInfo.State.ENQUEUED -> {
                        if (info.runAttemptCount > 0) "Retrying Sync..." else "Sync Queued"
                    }
                    WorkInfo.State.RUNNING -> "Syncing..."
                    WorkInfo.State.SUCCEEDED -> "Synced"
                    WorkInfo.State.FAILED -> {
                        val errorMsg = info.outputData.getString("error_message") ?: "Sync Failed"
                        viewModelScope.launch(Dispatchers.Main) {
                            android.widget.Toast.makeText(getApplication(), "Sync failed: $errorMsg", android.widget.Toast.LENGTH_LONG).show()
                        }
                        "Sync Failed"
                    }
                    WorkInfo.State.CANCELLED -> "Sync Cancelled"
                    else -> null
                }
            }
        }
    }

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
        if (timestamp > System.currentTimeMillis()) {
            Log.w(TAG, "Attempted to add entry in the future. Ignoring.")
            return
        }
        viewModelScope.launch {
            val newEntry = JournalEntry(
                description = description,
                timestamp = timestamp,
                photo_urls = photoUrls,
                attachments = attachments,
                bp_systolic = bpSystolic,
                bp_diastolic = bpDiastolic,
                heart_rate_avg = heartRate,
                sleep_hours = sleepHours,
                lastModified = System.currentTimeMillis()
            )
            repository.insert(newEntry)
            
            tags.forEach { tag ->
                repository.addTag(newEntry.entry_id, tag)
            }

            if (_isUserSignedIn.value) {
                SyncManager.enqueuePeriodicSync(getApplication())
            }
        }
    }

    override fun updateEntry(entry: JournalEntry, tags: Set<String>) {
        viewModelScope.launch {
            // Update lastModified to ensure local edits "win" in sync conflict resolution
            // Keep the original timestamp (creation date)
            repository.insert(entry.copy(syncStatus = "PENDING_SYNC", lastModified = System.currentTimeMillis()))
            
            // Refresh tags: remove all existing and add current selection
            val existingTags = repository.getTagsForEntry(entry.entry_id)
            existingTags.forEach { tag ->
                repository.removeTag(entry.entry_id, tag)
            }
            tags.forEach { tag ->
                repository.addTag(entry.entry_id, tag)
            }

            if (_isUserSignedIn.value) {
                SyncManager.enqueuePeriodicSync(getApplication())
            }
        }
    }

    override suspend fun getEntryById(entryId: String): JournalEntry? {
        return repository.getEntryById(entryId)
    }

    override fun signIn(activityContext: Context, onResolutionRequired: (android.app.PendingIntent) -> Unit) {
        viewModelScope.launch(ioContext) {
            try {
                Log.d(TAG, "Sign in initiated")
                val credential = authManager.signIn(activityContext)
                Log.d(TAG, "Sign in successful for ${credential.id}")
                sessionManager.saveUserEmail(credential.id)
                _isUserSignedIn.value = true
                
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication(), "Signed in as ${credential.id}", android.widget.Toast.LENGTH_SHORT).show()
                }

                requestDriveAuth(onResolutionRequired)
            } catch (e: Exception) {
                Log.e(TAG, "Sign in failed", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication(), "Sign-in failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun requestDriveAuth(onResolutionRequired: (android.app.PendingIntent) -> Unit) {
        val email = sessionManager.getUserEmail() ?: return
        
        Log.d(TAG, "Requesting Drive authorization for $email")
        authManager.requestDriveAuthorization(
            onResolutionRequired = { pendingIntent ->
                Log.d(TAG, "Resolution required for Drive access")
                onResolutionRequired(pendingIntent)
            },
            onSuccess = { _ ->
                Log.d(TAG, "Drive authorization successful")
                _syncStatus.value = "Authenticated & Authorized"
                viewModelScope.launch(Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication(), "Drive Authorization Successful!", android.widget.Toast.LENGTH_SHORT).show()
                }
                syncNow()
            }
        )
    }

    override fun syncNow() {
        val email = sessionManager.getUserEmail() ?: return
        Log.d(TAG, "Sync now triggered for $email")
        SyncManager.triggerManualSync(getApplication())
        _syncStatus.value = "Sync Requested"
        android.widget.Toast.makeText(getApplication(), "Syncing with Google Drive...", android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun signOut() {
        viewModelScope.launch {
            Log.d(TAG, "Sign out initiated")
            authManager.signOut()
            sessionManager.clearSession()
            _isUserSignedIn.value = false
            _syncStatus.value = null
            android.widget.Toast.makeText(getApplication(), "Signed out successfully", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    override fun setArchiveSearchQuery(query: String) {
        _archiveSearchQuery.value = query
    }

    override fun setSortOrder(isAsc: Boolean) {
        _isAscending.value = isAsc
    }

    override fun toggleTag(tag: String) {
        val current = _selectedTags.value
        if (current.contains(tag)) {
            _selectedTags.value = current - tag
        } else {
            _selectedTags.value = current + tag
        }
    }

    fun toggleEntryTag(entryId: String, tag: String) {
        viewModelScope.launch {
            val tags = repository.getTagsForEntry(entryId)
            if (tags.contains(tag)) {
                repository.removeTag(entryId, tag)
            } else {
                repository.addTag(entryId, tag)
            }
            // Spec §4: tag changes must set PENDING_SYNC and bump lastModified
            repository.markEntryDirty(entryId)
            if (_isUserSignedIn.value) {
                SyncManager.enqueuePeriodicSync(getApplication())
            }
        }
    }

    suspend fun getTagsForEntry(entryId: String): List<String> {
        return repository.getTagsForEntry(entryId)
    }

    // Health Connect Implementation

    override val healthPermissions: Set<String>
        get() = healthManager.requiredPermissions.toSet()

    override suspend fun hasHealthPermissions(): Boolean {
        return healthManager.hasAllPermissions()
    }

    override fun checkHealthAvailability(): Int {
        return healthManager.checkAvailability()
    }

    override suspend fun syncHealthData(timestamp: Long): HealthSyncResult = withContext(ioContext) {
        val instant = Instant.ofEpochMilli(timestamp)
        val zoneId = ZoneId.systemDefault()
        val startOfDay = instant.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()
        val endOfDay = startOfDay.plus(1, java.time.temporal.ChronoUnit.DAYS)

        val bp = healthManager.getLatestBloodPressure(startOfDay, endOfDay)
        val hr = healthManager.getAverageHeartRate(startOfDay, endOfDay)
        val sleep = healthManager.getSleepDurationHours(startOfDay.minus(12, java.time.temporal.ChronoUnit.HOURS), endOfDay)

        HealthSyncResult(
            bpSystolic = bp?.first,
            bpDiastolic = bp?.second,
            heartRate = hr?.toInt(),
            sleepHours = sleep
        )
    }

    // Archive & Delete
    override fun archiveEntry(entryId: String) {
        viewModelScope.launch {
            repository.archiveEntry(entryId)
            if (_isUserSignedIn.value) {
                SyncManager.enqueuePeriodicSync(getApplication())
            }
        }
    }

    override fun restoreEntry(entryId: String) {
        Log.d(TAG, "Restoring entry: $entryId")
        viewModelScope.launch {
            repository.restoreEntry(entryId)
            if (_isUserSignedIn.value) {
                SyncManager.enqueuePeriodicSync(getApplication())
            }
        }
    }

    override fun deleteEntries(entryIds: List<String>) {
        viewModelScope.launch {
            repository.deleteEntries(entryIds)
            if (_isUserSignedIn.value) {
                SyncManager.enqueuePeriodicSync(getApplication())
            }
        }
    }

    override fun emptyArchive() {
        viewModelScope.launch {
            repository.deleteAllArchived()
            if (_isUserSignedIn.value) {
                SyncManager.enqueuePeriodicSync(getApplication())
            }
        }
    }

    override suspend fun savePersistentFile(uri: android.net.Uri, isPhoto: Boolean): String? = withContext(ioContext) {
        val context = getApplication<Application>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            if (isPhoto) {
                mediaService.compressAndSaveImage(inputStream, uri.lastPathSegment)
            } else {
                val fileName = "doc_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}"
                val dir = java.io.File(context.filesDir, "attachments")
                dir.mkdirs()
                val persistentFile = java.io.File(dir, fileName)
                inputStream.use { input ->
                    persistentFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                "file://${persistentFile.absolutePath}"
            }
        } catch (e: Exception) {
            Log.e("JournalViewModel", "Error saving persistent file for URI: $uri", e)
            null
        }
    }
}

class JournalViewModelFactory(
    private val application: Application,
    private val repository: JournalRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
            val authManager = GoogleAuthManager(application)
            val sessionManager = SessionManager(application)
            val healthManager = HealthConnectManager(application)
            val mediaService = AndroidMediaCompressionService(application)
            @Suppress("UNCHECKED_CAST")
            return JournalViewModel(application, repository, authManager, sessionManager, healthManager, mediaService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
