package com.example.healthjournal.export

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.ProfileRepository
import com.example.healthjournal.data.local.ProfileEntity
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed class ExportState {
    object Idle : ExportState()
    data class Generating(val progress: Float, val message: String) : ExportState()
    data class ReadyToShare(val fileUri: Uri, val mimeType: String, val fileName: String) : ExportState()
    data class Error(val message: String) : ExportState()
}

class ExportViewModel(
    application: Application,
    private val repository: JournalRepository,
    fullBackupUseCase: FullBackupUseCase,
    private val profileRepository: ProfileRepository? = null
) : AndroidViewModel(application) {

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState

    val activeProfiles: StateFlow<List<ProfileEntity>> =
        (profileRepository?.activeProfiles ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedProfileIds = MutableStateFlow(
        setOf(repository.currentProfileId().ifBlank { SELF_PROFILE_ID })
    )
    val selectedProfileIds: StateFlow<Set<String>> = _selectedProfileIds.asStateFlow()

    private var lastActiveProfileId: String = repository.currentProfileId().ifBlank { SELF_PROFILE_ID }

    private val exportsDir = File(application.cacheDir, "exports")
    private val imageResizer = ImageResizer(application)
    private val pdfExportUseCase = PdfExportUseCase(application, repository, exportsDir, imageResizer)
    private val fullBackupUseCase = fullBackupUseCase
    private val exportService = ExportServiceImpl(application.cacheDir)

    init {
        viewModelScope.launch {
            repository.activeProfileId.collect { activeId ->
                val cleanId = activeId.ifBlank { SELF_PROFILE_ID }
                if (cleanId != lastActiveProfileId) {
                    lastActiveProfileId = cleanId
                    _selectedProfileIds.value = setOf(cleanId)
                }
            }
        }
        viewModelScope.launch {
            activeProfiles.collect { profiles ->
                if (profiles.isNotEmpty()) {
                    val activeSet = profiles.map { it.id }.toSet()
                    val retained = _selectedProfileIds.value.intersect(activeSet)
                    if (retained.isEmpty()) {
                        val fallback = repository.currentProfileId().takeIf { it in activeSet }
                            ?: profiles.first().id
                        _selectedProfileIds.value = setOf(fallback)
                    } else if (retained.size != _selectedProfileIds.value.size) {
                        _selectedProfileIds.value = retained
                    }
                }
            }
        }
    }

    fun toggleProfileSelection(profileId: String) {
        val current = _selectedProfileIds.value
        if (profileId in current) {
            if (current.size > 1) {
                _selectedProfileIds.value = current - profileId
            }
        } else {
            _selectedProfileIds.value = current + profileId
        }
    }

    fun exportData(
        startDate: Long,
        endDate: Long,
        format: String,
        passphrase: String? = null,
        profileIds: List<String>? = null
    ) {
        viewModelScope.launch {
            _exportState.value = ExportState.Generating(0f, "Preparing export...")
            exportService.cleanupCache()

            try {
                val file = when (format.uppercase()) {
                    "PDF" -> {
                        val selectedSet = profileIds?.toSet() ?: _selectedProfileIds.value
                        val orderedProfiles = activeProfiles.value
                            .filter { it.id in selectedSet }
                            .map { it.id }
                            .ifEmpty {
                                selectedSet.toList().ifEmpty { listOf(repository.currentProfileId()) }
                            }
                        val profileNames = activeProfiles.value.associate { it.id to it.name }
                        pdfExportUseCase.execute(
                            profileIds = orderedProfiles,
                            startDate = startDate,
                            endDate = endDate,
                            profileNames = profileNames
                        )
                    }
                    "ZIP" -> fullBackupUseCase.execute(passphrase)
                    else -> throw IllegalArgumentException("Unsupported format")
                }

                val authority = "${getApplication<Application>().packageName}.fileprovider"
                val uri = FileProvider.getUriForFile(getApplication(), authority, file)
                val mimeType = if (format.uppercase() == "PDF") "application/pdf" else "application/zip"

                _exportState.value = ExportState.ReadyToShare(uri, mimeType, file.name)
            } catch (e: Exception) {
                e.printStackTrace()
                _exportState.value = ExportState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun resetState() {
        _exportState.value = ExportState.Idle
    }
}
