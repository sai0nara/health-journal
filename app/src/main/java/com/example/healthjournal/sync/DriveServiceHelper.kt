package com.example.healthjournal.sync

import android.content.Context
import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

class DriveServiceHelper(private val context: Context, private val driveService: Drive) {

    suspend fun uploadJournalData(content: String): String? =
        uploadDataFile(JOURNAL_DATA_FILE, content)

    /**
     * Uploads (creates or updates) a JSON data file in the appDataFolder.
     * Returns the Drive file id, or null when creation returned no id.
     */
    suspend fun uploadDataFile(fileName: String, content: String): String? = withContext(Dispatchers.IO) {
        val metadata = File()
            .setName(fileName)
            .setMimeType("application/json")
            .setParents(Collections.singletonList("appDataFolder"))

        val contentStream = ByteArrayContent.fromString("application/json", content)

        val existingFileId = findFileByName(fileName)

        val file = if (existingFileId == null) {
            android.util.Log.d("DriveServiceHelper", "Creating new cloud file...")
            driveService.files().create(metadata, contentStream).execute()
        } else {
            android.util.Log.d("DriveServiceHelper", "Updating existing cloud file: $existingFileId")
            driveService.files().update(existingFileId, null, contentStream).execute()
        }

        file.id
    }

    suspend fun downloadJournalData(): String? =
        downloadDataFile(JOURNAL_DATA_FILE)

    /** Downloads a JSON data file from the appDataFolder; null if absent. */
    suspend fun downloadDataFile(fileName: String): String? = withContext(Dispatchers.IO) {
        val fileId = findFileByName(fileName) ?: return@withContext null

        driveService.files().get(fileId).executeMediaAsInputStream().use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        }
    }

    suspend fun uploadFile(localFile: java.io.File, mimeType: String): String? = withContext(Dispatchers.IO) {
        val metadata = File()
            .setName(localFile.name)
            .setMimeType(mimeType)
            .setParents(Collections.singletonList("appDataFolder"))

        val contentStream = com.google.api.client.http.FileContent(mimeType, localFile)

        val existingFileId = findFileByName(localFile.name)
        
        val file = if (existingFileId == null) {
            android.util.Log.d("DriveServiceHelper", "Uploading new file: ${localFile.name}")
            driveService.files().create(metadata, contentStream).execute()
        } else {
            android.util.Log.d("DriveServiceHelper", "File already exists in cloud: ${localFile.name}")
            return@withContext existingFileId
        }
        
        file.id
    }

    suspend fun downloadFile(fileId: String, targetFile: java.io.File): Boolean = withContext(Dispatchers.IO) {
        try {
            targetFile.parentFile?.let { parent ->
                if (!parent.exists()) {
                    parent.mkdirs()
                }
            }
            driveService.files().get(fileId).executeMediaAsInputStream().use { inputStream ->
                targetFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("DriveServiceHelper", "Error downloading file $fileId: ${e.message}")
            false
        }
    }

    suspend fun findFileByName(name: String): String? = withContext(Dispatchers.IO) {
        val result = driveService.files().list()
            .setQ("name = '$name' and trashed = false")
            .setSpaces("appDataFolder")
            .setFields("files(id)")
            .execute()
        
        result.files.firstOrNull()?.id
    }


    companion object {
        const val JOURNAL_DATA_FILE = "health_journal_data.json"
        const val MEASUREMENTS_DATA_FILE = "body_measurements.json"

        /**
         * Cross-device deletion ledger for the shared deleted_entries table.
         * Kept in a sibling file so legacy clients that only know
         * [MEASUREMENTS_DATA_FILE] continue to parse their payload unchanged.
         */
        const val MEASUREMENTS_TOMBSTONES_FILE = "body_measurements_tombstones.json"
    }
}
