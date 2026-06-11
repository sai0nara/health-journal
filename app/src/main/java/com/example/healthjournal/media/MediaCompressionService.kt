package com.example.healthjournal.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

interface MediaCompressionService {
    /**
     * Compresses the provided image stream and saves it to local internal storage.
     * @return String URI representation of the saved file, or null if failed.
     */
    fun compressAndSaveImage(inputStream: InputStream, originalFileName: String?): String?
}

class AndroidMediaCompressionService(private val context: Context) : MediaCompressionService {

    override fun compressAndSaveImage(inputStream: InputStream, originalFileName: String?): String? {
        return try {
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null

            // Generate unique file name
            val ext = originalFileName?.substringAfterLast('.', "jpg") ?: "jpg"
            val uniqueFileName = "media_${UUID.randomUUID()}.$ext"
            
            // Destination file in context.filesDir
            val destFile = File(context.filesDir, uniqueFileName)
            
            val outputStream = FileOutputStream(destFile)
            val success = bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            
            outputStream.flush()
            outputStream.close()
            
            if (success) {
                "file://${destFile.absolutePath}"
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
