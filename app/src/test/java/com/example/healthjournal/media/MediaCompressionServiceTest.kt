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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

class MediaCompressionServiceTest {

    private lateinit var context: Context
    private lateinit var compressionService: MediaCompressionService
    private lateinit var exifHandler: ExifOrientationHandler
    private lateinit var mockFilesDir: File

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        mockFilesDir = java.nio.file.Files.createTempDirectory("test_files_dir").toFile()
        every { context.filesDir } returns mockFilesDir

        exifHandler = mockk(relaxed = true)
        every { exifHandler.read(any()) } returns android.media.ExifInterface.ORIENTATION_NORMAL
        compressionService = AndroidMediaCompressionService(context, exifHandler)

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

    private fun firstSavedFile(): File {
        val photosDir = File(mockFilesDir, "photos")
        return photosDir.listFiles()!!.first()
    }

    @Test
    fun `compressAndSaveImage returns valid URI string on success`() {
        val realStream = ByteArrayInputStream("valid-image-bytes".toByteArray())
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { BitmapFactory.decodeByteArray(any(), any(), any()) } returns mockBitmap
        every { mockBitmap.compress(any(), any(), any()) } returns true

        val uri = compressionService.compressAndSaveImage(realStream, "test_image.jpg")

        assertNotNull(uri)
        assert(uri!!.startsWith("file://${mockFilesDir.absolutePath}/photos/media_"))

        verify { mockBitmap.compress(Bitmap.CompressFormat.JPEG, 80, any()) }
    }

    @Test
    fun `compressAndSaveImage falls back to raw copy on decode failure`() {
        every { BitmapFactory.decodeByteArray(any(), any(), any()) } returns null
        val rawBytes = "fake-image-bytes".toByteArray()
        val realStream = ByteArrayInputStream(rawBytes)

        val uri = compressionService.compressAndSaveImage(realStream, "test_image.jpg")

        assertNotNull(uri)
        val savedFile = firstSavedFile()
        assertTrue("Fallback file must not be empty", savedFile.length() > 0)
        assertEquals("Fallback must contain the original bytes", "fake-image-bytes", savedFile.readText())
    }

    @Test
    fun `compressAndSaveImage returns null on decode failure and copy failure`() {
        val mockInputStream = mockk<InputStream>()
        every { BitmapFactory.decodeByteArray(any(), any(), any()) } returns null
        // Make stream operations throw to fail the fallback copy
        every { mockInputStream.read(any<ByteArray>()) } throws IOException("Read failed")
        every { mockInputStream.read(any<ByteArray>(), any(), any()) } throws IOException("Read failed")
        every { mockInputStream.read() } throws IOException("Read failed")

        val uri = compressionService.compressAndSaveImage(mockInputStream, "test_image.jpg")

        assertNull(uri)
    }

    @Test
    fun `compressAndSaveImage falls back to raw copy on compress failure`() {
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { BitmapFactory.decodeByteArray(any(), any(), any()) } returns mockBitmap
        every { mockBitmap.compress(any(), any(), any()) } returns false
        val rawBytes = "raw-bytes-for-compress-fallback".toByteArray()
        val realStream = ByteArrayInputStream(rawBytes)

        val uri = compressionService.compressAndSaveImage(realStream, "test_image.jpg")

        assertNotNull(uri)
        val savedFile = firstSavedFile()
        assertTrue("Fallback file must not be empty", savedFile.length() > 0)
        assertEquals("raw-bytes-for-compress-fallback", savedFile.readText())
    }

    @Test
    fun `compressAndSaveImage returns null for empty input`() {
        every { BitmapFactory.decodeByteArray(any(), any(), any()) } returns null
        val emptyStream = ByteArrayInputStream(ByteArray(0))

        val uri = compressionService.compressAndSaveImage(emptyStream, "test_image.jpg")

        assertNull("Empty input must not produce a 0-byte file", uri)
        val photosDir = File(mockFilesDir, "photos")
        assertTrue(photosDir.listFiles()?.isEmpty() != false)
    }

    @Test
    fun `compressAndSaveImage preserves EXIF orientation`() {
        val realStream = ByteArrayInputStream("orientable-image-bytes".toByteArray())
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { BitmapFactory.decodeByteArray(any(), any(), any()) } returns mockBitmap
        every { mockBitmap.compress(any(), any(), any()) } returns true
        every { exifHandler.read(any()) } returns android.media.ExifInterface.ORIENTATION_ROTATE_90

        compressionService.compressAndSaveImage(realStream, "test_image.jpg")

        verify { exifHandler.write(any(), android.media.ExifInterface.ORIENTATION_ROTATE_90) }
    }

    @Test
    fun `compressAndSaveImage does not write orientation when absent`() {
        val mockInputStream = createMockInputStream()
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { BitmapFactory.decodeByteArray(any(), any(), any()) } returns mockBitmap
        every { mockBitmap.compress(any(), any(), any()) } returns true

        compressionService.compressAndSaveImage(mockInputStream, "test_image.jpg")

        verify(exactly = 0) { exifHandler.write(any(), any()) }
    }
}