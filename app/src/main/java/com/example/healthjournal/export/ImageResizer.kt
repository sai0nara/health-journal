package com.example.healthjournal.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

class ImageResizer(private val context: Context) {

    /**
     * Downsamples an image from a URI and saves it as a temporary JPEG.
     * Max dimension is 1000px to keep PDF size manageable.
     */
    fun downsampleImage(uri: Uri, maxWidth: Int = 1000): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            var inSampleSize = 1
            if (options.outHeight > maxWidth || options.outWidth > maxWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= maxWidth || halfWidth / inSampleSize >= maxWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            
            val secondStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(secondStream, null, decodeOptions)
            secondStream.close()

            if (bitmap == null) return null

            // Final resize if still too large
            val finalBitmap = if (bitmap.width > maxWidth || bitmap.height > maxWidth) {
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val targetWidth = if (ratio > 1) maxWidth else (maxWidth * ratio).toInt()
                val targetHeight = if (ratio > 1) (maxWidth / ratio).toInt() else maxWidth
                Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            } else {
                bitmap
            }

            val tempFile = File(context.cacheDir, "export_tmp_img_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            
            if (finalBitmap != bitmap) bitmap.recycle()
            finalBitmap.recycle()

            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
