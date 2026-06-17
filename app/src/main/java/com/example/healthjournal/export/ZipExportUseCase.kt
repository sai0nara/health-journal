package com.example.healthjournal.export

import com.example.healthjournal.data.JournalRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipExportUseCase(
    private val repository: JournalRepository,
    private val exportsDir: File,
    private val gson: Gson
) {
    suspend fun execute(): File {
        exportsDir.mkdirs()
        val zipFile = File(exportsDir, "health_journal_export_${System.currentTimeMillis()}.zip")
        val entries = repository.allEntries.first()
        
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // 1. Write data.json
            val dataJson = gson.toJson(entries)
            val dataEntry = ZipEntry("data.json")
            zos.putNextEntry(dataEntry)
            zos.write(dataJson.toByteArray())
            zos.closeEntry()
            
            // 2. Copy media files
            entries.forEach { entry ->
                entry.attachments?.forEach { attachment ->
                    val file = File(attachment.uri.replace("file://", ""))
                    if (file.exists() && file.isFile) {
                        val entryName = "media/${file.name}"
                        // Avoid duplicate entries in ZIP if the same file is attached multiple times
                        try {
                            zos.putNextEntry(ZipEntry(entryName))
                            file.inputStream().use { input ->
                                input.copyTo(zos, bufferSize = 8192)
                            }
                            zos.closeEntry()
                        } catch (e: Exception) {
                            // Entry might already exist or other IO error
                        }
                    }
                }
            }
        }
        
        return zipFile
    }
}
