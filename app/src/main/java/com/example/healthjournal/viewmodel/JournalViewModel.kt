package com.example.healthjournal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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

class JournalViewModel(application: Application, private val repository: JournalRepository) : AndroidViewModel(application) {

    private val authManager = GoogleAuthManager(application)
    private val sessionManager = SessionManager(application)

    val allEntries: StateFlow<List<JournalEntry>> = repository.allEntries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isUserSignedIn = MutableStateFlow(sessionManager.getUserEmail() != null)
    val isUserSignedIn: StateFlow<Boolean> = _isUserSignedIn.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    fun addEntry(description: String) {
        viewModelScope.launch {
            val newEntry = JournalEntry(description = description)
            repository.insert(newEntry)
            if (_isUserSignedIn.value) {
                SyncManager.enqueueSync(getApplication())
            }
        }
    }

    fun signIn(onResolutionRequired: (android.app.PendingIntent) -> Unit) {
        viewModelScope.launch {
            val credential = authManager.signIn()
            if (credential != null) {
                sessionManager.saveUserEmail(credential.id)
                _isUserSignedIn.value = true
                
                // Request Drive authorization after sign in
                authManager.requestDriveAuthorization(
                    email = credential.id,
                    onResolutionRequired = onResolutionRequired,
                    onSuccess = { accessToken ->
                        _syncStatus.value = "Authenticated & Authorized"
                        syncNow()
                    }
                )
            }
        }
    }

    fun syncNow() {
        val email = sessionManager.getUserEmail() ?: return
        viewModelScope.launch {
            _syncStatus.value = "Syncing..."
            try {
                val account = authManager.getAccount(email)
                val driveService = DriveServiceHelper.createDriveService(getApplication(), account)
                val driveHelper = DriveServiceHelper(driveService)
                
                // 1. Upload local to cloud
                val localEntries = repository.allEntries.first()
                driveHelper.uploadJournalData(Gson().toJson(localEntries))
                
                // 2. Download from cloud and merge (simple merge: cloud wins for conflicts)
                val cloudJson = driveHelper.downloadJournalData()
                if (cloudJson != null) {
                    val type = object : TypeToken<List<JournalEntry>>() {}.type
                    val cloudEntries: List<JournalEntry> = Gson().fromJson(cloudJson, type)
                    repository.importAll(cloudEntries)
                }
                
                _syncStatus.value = "Sync Complete"
            } catch (e: Exception) {
                _syncStatus.value = "Sync Failed: ${e.message}"
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            sessionManager.clearSession()
            _isUserSignedIn.value = false
            _syncStatus.value = null
        }
    }
}

class JournalViewModelFactory(private val application: Application, private val repository: JournalRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JournalViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
