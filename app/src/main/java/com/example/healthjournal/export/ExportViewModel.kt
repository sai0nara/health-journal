package com.example.healthjournal.export

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthjournal.data.JournalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    fullBackupUseCase: FullBackupUseCase
) : AndroidViewModel(application) {

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState

    private val exportsDir = File(application.cacheDir, "exports")
    private val imageResizer = ImageResizer(application)
    private val pdfExportUseCase = PdfExportUseCase(application, repository, exportsDir, imageResizer)
    private val fullBackupUseCase = fullBackupUseCase
    private val exportService = ExportServiceImpl(application.cacheDir)

    fun exportData(startDate: Long, endDate: Long, format: String) {
        viewModelScope.launch {
            _exportState.value = ExportState.Generating(0f, "Preparing export...")
            exportService.cleanupCache()
            
            try {
                val file = when (format.uppercase()) {
                    "PDF" -> pdfExportUseCase.execute(startDate, endDate)
                    "ZIP" -> fullBackupUseCase.execute()
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
