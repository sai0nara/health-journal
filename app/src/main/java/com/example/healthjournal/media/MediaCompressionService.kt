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
        val photosDir = File(context.filesDir, "photos")
        if (!photosDir.exists()) {
            photosDir.mkdirs()
        }
        val ext = originalFileName?.substringAfterLast('.', "jpg") ?: "jpg"
        val uniqueFileName = "media_${UUID.randomUUID()}.$ext"
        val destFile = File(photosDir, uniqueFileName)

        return try {
            val bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap == null) {
                // Decode failed, try raw fallback
                saveRawFallback(inputStream, destFile)
            } else {
                val success = FileOutputStream(destFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream).also {
                        outputStream.flush()
                    }
                }
                bitmap.recycle()
                
                if (success) {
                    "file://${destFile.absolutePath}"
                } else {
                    // Compression failed, save raw fallback
                    saveRawFallback(inputStream, destFile)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Exception caught, try raw fallback as last resort
            try {
                saveRawFallback(inputStream, destFile)
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
        }
    }

    private fun saveRawFallback(inputStream: InputStream, destFile: File): String? {
        return try {
            // Re-open/copy stream to destFile if possible.
            // Note: the original inputStream might have been consumed/read already.
            // But if it supports marking/resetting, we can reset it.
            // If not, we just attempt to copy what remains or catch the failure.
            if (inputStream.markSupported()) {
                inputStream.reset()
            }
            FileOutputStream(destFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            "file://${destFile.absolutePath}"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
