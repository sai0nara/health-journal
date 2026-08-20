package com.example.healthjournal.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import java.io.ByteArrayInputStream
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

interface ExifOrientationHandler {
    fun read(bytes: ByteArray): Int
    fun write(file: File, orientation: Int)
}

class AndroidExifOrientationHandler : ExifOrientationHandler {
    override fun read(bytes: ByteArray): Int {
        return try {
            ExifInterface(ByteArrayInputStream(bytes))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    override fun write(file: File, orientation: Int) {
        try {
            val exif = ExifInterface(file)
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            exif.saveAttributes()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class AndroidMediaCompressionService(
    private val context: Context,
    private val exifHandler: ExifOrientationHandler = AndroidExifOrientationHandler()
) : MediaCompressionService {

    override fun compressAndSaveImage(inputStream: InputStream, originalFileName: String?): String? {
        val photosDir = File(context.filesDir, "photos")
        if (!photosDir.exists()) {
            photosDir.mkdirs()
        }
        val ext = originalFileName?.substringAfterLast('.', "jpg") ?: "jpg"
        val uniqueFileName = "media_${UUID.randomUUID()}.$ext"
        val destFile = File(photosDir, uniqueFileName)

        return try {
            // Buffer the entire stream first so fallbacks have the original bytes
            // (a consumed stream cannot be re-read, previously producing 0-byte files)
            val bytes = inputStream.readBytes()
            if (bytes.isEmpty()) return null

            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap == null) {
                saveRawFallback(bytes, destFile)
            } else {
                val orientation = exifHandler.read(bytes)
                val success = FileOutputStream(destFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream).also {
                        outputStream.flush()
                    }
                }
                bitmap.recycle()

                if (success) {
                    if (orientation != ExifInterface.ORIENTATION_NORMAL) {
                        exifHandler.write(destFile, orientation)
                    }
                    "file://${destFile.absolutePath}"
                } else {
                    // Compression failed, save raw fallback
                    saveRawFallback(bytes, destFile)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Exception caught, try raw fallback as last resort
            try {
                saveRawFallback(inputStream.readBytes(), destFile)
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
        }
    }

    private fun saveRawFallback(bytes: ByteArray, destFile: File): String? {
        return try {
            if (bytes.isEmpty()) return null
            FileOutputStream(destFile).use { outputStream ->
                outputStream.write(bytes)
                outputStream.flush()
            }
            "file://${destFile.absolutePath}"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}