package com.example.healthjournal.export

import android.net.Uri
import android.webkit.MimeTypeMap
import com.example.healthjournal.data.JournalRepository
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.UUID

class ZipExportUseCase(
    private val repository: JournalRepository,
    private val exportsDir: File,
    private val gson: Gson
) {
    suspend fun execute(startDate: Long = 0L, endDate: Long = Long.MAX_VALUE): File {
        exportsDir.mkdirs()
        val zipFile = File(exportsDir, "health_journal_export_${System.currentTimeMillis()}.zip")
        val entries = repository.getAllEntriesInDateRange(startDate, endDate)
        
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // 1. Write data.json
            val dataJson = gson.toJson(entries)
            val dataEntry = ZipEntry("data.json")
            zos.putNextEntry(dataEntry)
            zos.write(dataJson.toByteArray())
            zos.closeEntry()
            
            // 2. Copy media files
            entries.forEach { entry ->
                // Process attachments
                entry.attachments?.forEach { attachment ->
                    processFileForZip(zos, attachment.uri, attachment.name, attachment.mimeType)
                }
                
                // Process photos
                entry.photo_urls?.forEach { photoUrl ->
                    // Photos are just raw URLs/file paths. Let's assume generic naming for now
                    processFileForZip(zos, photoUrl, "photo_${System.currentTimeMillis()}_${UUID.randomUUID()}", "image/jpeg")
                }
            }
        }
        
        return zipFile
    }

    private fun processFileForZip(zos: ZipOutputStream, fileUri: String, originalName: String, mimeType: String) {
        val uri = Uri.parse(fileUri)
        val file = if (uri.scheme == "file") {
            File(uri.path ?: "")
        } else {
            File(fileUri)
        }
        
        android.util.Log.d("ZipExport", "Checking file: $originalName, URI: $fileUri, File: ${file.absolutePath}, Exists: ${file.exists()}")
        if (file.exists() && file.isFile) {
            val mime = MimeTypeMap.getSingleton()
            val extension = mime.getExtensionFromMimeType(mimeType) ?: "jpg"
            var fileName = originalName
            if (!fileName.endsWith(".$extension", ignoreCase = true)) {
                fileName = "$fileName.$extension"
            }
            val entryName = "media/$fileName"

            try {
                zos.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { input ->
                    input.copyTo(zos, bufferSize = 8192)
                }
                zos.closeEntry()
            } catch (e: Exception) {
                android.util.Log.e("ZipExport", "Failed to add to ZIP: $entryName", e)
            }
        }
    }
}
