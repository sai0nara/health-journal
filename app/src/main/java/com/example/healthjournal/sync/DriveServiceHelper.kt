package com.example.healthjournal.sync

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

class DriveServiceHelper(private val driveService: Drive) {

    suspend fun uploadJournalData(content: String): String? = withContext(Dispatchers.IO) {
        val metadata = File()
            .setName("health_journal_data.json")
            .setMimeType("application/json")

        val contentStream = ByteArrayContent.fromString("application/json", content)

        val existingFileId = findDataFile()
        
        val file = if (existingFileId == null) {
            driveService.files().create(metadata, contentStream).execute()
        } else {
            driveService.files().update(existingFileId, null, contentStream).execute()
        }
        
        file.id
    }

    suspend fun downloadJournalData(): String? = withContext(Dispatchers.IO) {
        val fileId = findDataFile() ?: return@withContext null
        
        driveService.files().get(fileId).executeMediaAsInputStream().use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        }
    }

    private suspend fun findDataFile(): String? = withContext(Dispatchers.IO) {
        val result = driveService.files().list()
            .setQ("name = 'health_journal_data.json' and trashed = false")
            .setSpaces("drive")
            .setFields("files(id)")
            .execute()
        
        result.files.firstOrNull()?.id
    }

    companion object {
        fun createDriveService(context: Context, account: android.accounts.Account): Drive {
            val credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singletonList(DriveScopes.DRIVE_FILE)
            ).setSelectedAccount(account)

            return Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Health Journal").build()
        }
    }
}
