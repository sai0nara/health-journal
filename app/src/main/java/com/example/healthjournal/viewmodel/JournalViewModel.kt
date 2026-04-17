package com.example.healthjournal.viewmodel

import android.app.Application
import android.content.Context
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
import com.example.healthjournal.sync.DriveServiceHelper
import com.example.healthjournal.sync.SyncManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface IJournalViewModel {
    val allEntries: StateFlow<List<JournalEntry>>
    val isUserSignedIn: StateFlow<Boolean>
    val syncStatus: StateFlow<String?>
    fun addEntry(description: String, timestamp: Long = System.currentTimeMillis())
    fun signIn(activityContext: Context, onResolutionRequired: (android.app.PendingIntent) -> Unit)
    fun syncNow()
    fun signOut()
}

class JournalViewModel(
    application: Application,
    private val repository: JournalRepository,
    private val authManager: GoogleAuthManager,
    private val sessionManager: SessionManager
) : AndroidViewModel(application), IJournalViewModel {

    override val allEntries: StateFlow<List<JournalEntry>> = repository.allEntries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isUserSignedIn = MutableStateFlow(sessionManager.getUserEmail() != null)
    override val isUserSignedIn: StateFlow<Boolean> = _isUserSignedIn.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    override val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

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

    override fun addEntry(description: String, timestamp: Long) {
        viewModelScope.launch {
            val newEntry = JournalEntry(description = description, timestamp = timestamp)
            repository.insert(newEntry)
            if (_isUserSignedIn.value) {
                SyncManager.enqueueSync(getApplication())
            }
        }
    }

    override fun signIn(activityContext: Context, onResolutionRequired: (android.app.PendingIntent) -> Unit) {
        viewModelScope.launch {
            val credential = authManager.signIn(activityContext)
            if (credential != null) {
                sessionManager.saveUserEmail(credential.id)
                _isUserSignedIn.value = true
                
                // Request Drive authorization after sign in
                authManager.requestDriveAuthorization(
                    email = credential.id,
                    onResolutionRequired = onResolutionRequired,
                    onSuccess = { _ ->
                        _syncStatus.value = "Authenticated & Authorized"
                        syncNow()
                    }
                )
            }
        }
    }

    override fun syncNow() {
        val email = sessionManager.getUserEmail() ?: return
        SyncManager.enqueueSync(getApplication())
        _syncStatus.value = "Sync Requested"
    }

    override fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            sessionManager.clearSession()
            _isUserSignedIn.value = false
            _syncStatus.value = null
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
            @Suppress("UNCHECKED_CAST")
            return JournalViewModel(application, repository, authManager, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
