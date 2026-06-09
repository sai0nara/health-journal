package com.example.healthjournal.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.InputStream

import android.net.Uri

class MediaCompressionServiceTest {

    private lateinit var context: Context
    private lateinit var compressionService: MediaCompressionService
    private lateinit var mockFilesDir: File

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        mockFilesDir = java.nio.file.Files.createTempDirectory("test_files_dir").toFile()
        every { context.filesDir } returns mockFilesDir
        
        compressionService = AndroidMediaCompressionService(context)
        
        mockkStatic(BitmapFactory::class)
    }

    @Test
    fun `compressAndSaveImage returns valid URI string on success`() {
        val mockInputStream = mockk<InputStream>(relaxed = true)
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { BitmapFactory.decodeStream(any()) } returns mockBitmap
        every { mockBitmap.compress(any(), any(), any()) } returns true
        
        val uri = compressionService.compressAndSaveImage(mockInputStream, "test_image.jpg")
        
        assertNotNull(uri)
        assert(uri!!.startsWith("file://${mockFilesDir.absolutePath}/media_"))
        
        verify { mockBitmap.compress(Bitmap.CompressFormat.JPEG, 80, any()) }
    }

    @Test
    fun `compressAndSaveImage returns null on decode failure`() {
        val mockInputStream = mockk<InputStream>(relaxed = true)
        every { BitmapFactory.decodeStream(any()) } returns null
        
        val uri = compressionService.compressAndSaveImage(mockInputStream, "test_image.jpg")
        
        assertNull(uri)
    }

    @Test
    fun `compressAndSaveImage returns null on compress failure`() {
        val mockInputStream = mockk<InputStream>(relaxed = true)
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { BitmapFactory.decodeStream(any()) } returns mockBitmap
        every { mockBitmap.compress(any(), any(), any()) } returns false
        
        val uri = compressionService.compressAndSaveImage(mockInputStream, "test_image.jpg")
        
        assertNull(uri)
    }
}
