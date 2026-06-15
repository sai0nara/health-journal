package com.example.healthjournal.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.io.InputStream

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

    @After
    fun teardown() {
        unmockkStatic(BitmapFactory::class)
        mockFilesDir.deleteRecursively()
    }

    private fun createMockInputStream(): InputStream {
        val mockInputStream = mockk<InputStream>(relaxed = true)
        // Prevent infinite loops in InputStream.copyTo by returning -1 (EOF)
        every { mockInputStream.read(any<ByteArray>()) } returns -1
        every { mockInputStream.read(any<ByteArray>(), any(), any()) } returns -1
        every { mockInputStream.read() } returns -1
        return mockInputStream
    }

    @Test
    fun `compressAndSaveImage returns valid URI string on success`() {
        val mockInputStream = createMockInputStream()
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { BitmapFactory.decodeStream(any()) } returns mockBitmap
        every { mockBitmap.compress(any(), any(), any()) } returns true
        
        val uri = compressionService.compressAndSaveImage(mockInputStream, "test_image.jpg")
        
        assertNotNull(uri)
        assert(uri!!.startsWith("file://${mockFilesDir.absolutePath}/photos/media_"))
        
        verify { mockBitmap.compress(Bitmap.CompressFormat.JPEG, 80, any()) }
    }

    @Test
    fun `compressAndSaveImage falls back to raw copy on decode failure`() {
        val mockInputStream = createMockInputStream()
        every { BitmapFactory.decodeStream(any()) } returns null
        
        val uri = compressionService.compressAndSaveImage(mockInputStream, "test_image.jpg")
        
        assertNotNull(uri)
        assert(uri!!.startsWith("file://${mockFilesDir.absolutePath}/photos/media_"))
    }

    @Test
    fun `compressAndSaveImage returns null on decode failure and copy failure`() {
        val mockInputStream = mockk<InputStream>()
        every { BitmapFactory.decodeStream(any()) } returns null
        // Make stream operations throw to fail the fallback copy
        every { mockInputStream.read(any<ByteArray>()) } throws IOException("Read failed")
        every { mockInputStream.read(any<ByteArray>(), any(), any()) } throws IOException("Read failed")
        every { mockInputStream.read() } throws IOException("Read failed")
        every { mockInputStream.markSupported() } returns false

        val uri = compressionService.compressAndSaveImage(mockInputStream, "test_image.jpg")
        
        assertNull(uri)
    }

    @Test
    fun `compressAndSaveImage falls back to raw copy on compress failure`() {
        val mockInputStream = createMockInputStream()
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { BitmapFactory.decodeStream(any()) } returns mockBitmap
        every { mockBitmap.compress(any(), any(), any()) } returns false
        
        val uri = compressionService.compressAndSaveImage(mockInputStream, "test_image.jpg")
        
        assertNotNull(uri)
        assert(uri!!.startsWith("file://${mockFilesDir.absolutePath}/photos/media_"))
    }
}
