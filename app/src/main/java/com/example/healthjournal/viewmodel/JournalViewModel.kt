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
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface IJournalViewModel {
    val allEntries: StateFlow<List<JournalEntry>>
    val isUserSignedIn: StateFlow<Boolean>
    val syncStatus: StateFlow<String?>
    val searchQuery: StateFlow<String>
    val isAscending: StateFlow<Boolean>
    
    fun addEntry(description: String, timestamp: Long = System.currentTimeMillis(), photoUrl: String? = null)
    fun updateEntry(entry: JournalEntry)
    suspend fun getEntryById(entryId: String): JournalEntry?
    fun signIn(activityContext: Context, onResolutionRequired: (android.app.PendingIntent) -> Unit)
    fun syncNow()
    fun signOut()
    
    fun setSearchQuery(query: String)
    fun setSortOrder(isAsc: Boolean)
}

class JournalViewModel(
    application: Application,
    private val repository: JournalRepository,
    private val authManager: GoogleAuthManager,
    private val sessionManager: SessionManager,
    private val ioContext: kotlin.coroutines.CoroutineContext = Dispatchers.IO
) : AndroidViewModel(application), IJournalViewModel {

    private val _searchQuery = MutableStateFlow("")
    override val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isAscending = MutableStateFlow(false) // Default Descending (newest first)
    override val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val allEntries: StateFlow<List<JournalEntry>> = combine(_searchQuery, _isAscending) { query, isAsc ->
        query to isAsc
    }.flatMapLatest { (query, isAsc) ->
        if (query.isBlank()) {
            repository.getEntriesSortedByDate(isAsc)
        } else {
            repository.searchEntries(query, isAsc)
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
        // Observe WorkManager for "journal_sync"
        viewModelScope.launch {
            WorkManager.getInstance(getApplication())
                .getWorkInfosForUniqueWorkFlow("journal_sync")
                .collect { workInfos ->
                    val info = workInfos.firstOrNull()
                    _syncStatus.value = when (info?.state) {
                        WorkInfo.State.ENQUEUED -> {
                            if (info.runAttemptCount > 0) "Retrying Sync..." else "Sync Queued"
                        }
                        WorkInfo.State.RUNNING -> "Syncing..."
                        WorkInfo.State.SUCCEEDED -> "Synced"
                        WorkInfo.State.FAILED -> "Sync Failed"
                        WorkInfo.State.CANCELLED -> "Sync Cancelled"
                        else -> null
                    }
                }
        }
    }

    override fun addEntry(description: String, timestamp: Long, photoUrl: String?) {
        if (timestamp > System.currentTimeMillis()) {
            Log.w(TAG, "Attempted to add entry in the future. Ignoring.")
            return
        }
        viewModelScope.launch {
            val newEntry = JournalEntry(
                description = description,
                timestamp = timestamp,
                photo_url = photoUrl
            )
            repository.insert(newEntry)
            if (_isUserSignedIn.value) {
                SyncManager.enqueueSync(getApplication())
            }
        }
    }

    override fun updateEntry(entry: JournalEntry) {
        if (entry.timestamp > System.currentTimeMillis()) {
            Log.w(TAG, "Attempted to update entry with future date. Ignoring.")
            return
        }
        viewModelScope.launch {
            repository.insert(entry.copy(isSynced = false)) // Reset sync status on edit
            if (_isUserSignedIn.value) {
                SyncManager.enqueueSync(getApplication())
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
            onSuccess = { accessToken ->
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
        SyncManager.enqueueSync(getApplication())
        _syncStatus.value = "Sync Requested"
    }

    override fun signOut() {
        viewModelScope.launch {
            Log.d(TAG, "Sign out initiated")
            authManager.signOut()
            sessionManager.clearSession()
            _isUserSignedIn.value = false
            _syncStatus.value = null
        }
    }

    override fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    override fun setSortOrder(isAsc: Boolean) {
        _isAscending.value = isAsc
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
            @Suppress("UNCHECKED_CAST")
            return JournalViewModel(application, repository, authManager, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
