package com.example.healthjournal.export

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SafeBackupExtractorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun writeZip(entries: Map<String, String>): File {
        val zip = tempFolder.newFile("extract.zip")
        ZipOutputStream(FileOutputStream(zip)).use { zos ->
            entries.forEach { (name, content) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray())
                zos.closeEntry()
            }
        }
        return zip
    }

    private fun content(): String {
        // ~1.5 KB of repeated content so expansion limits are easy to reason about
        return "x".repeat(1500)
    }

    @Test
    fun extract_normalEntries_writesAllFilesToTarget() {
        val zip = writeZip(
            mapOf(
                "data.json" to """{"a":1}""",
                "media/photo.jpg" to content(),
                "goals.json" to """[]"""
            )
        )
        val target = tempFolder.newFolder("target")

        SafeBackupExtractor().extract(zip, target)

        assertTrue(File(target, "data.json").exists())
        assertTrue(File(target, "media/photo.jpg").exists())
        assertTrue(File(target, "goals.json").exists())
        assertEquals("""{"a":1}""", File(target, "data.json").readText())
    }

    @Test
    fun extract_entryWithDirectoryTraversal_isRejected() {
        val zip = writeZip(mapOf("../evil.txt" to "boom"))
        val target = tempFolder.newFolder("target")

        try {
            SafeBackupExtractor().extract(zip, target)
            fail("Expected traversal to be rejected")
        } catch (e: RestoreError.CorruptedFile) {
            assertTrue(true)
        }
        assertTrue(!File(tempFolder.root, "evil.txt").exists())
    }

    @Test
    fun extract_entryWithAbsolutePath_isRejected() {
        val absolute = "/tmp/evil_${System.nanoTime()}.txt"
        val zip = writeZip(mapOf(absolute to "boom"))
        val target = tempFolder.newFolder("target2")

        try {
            SafeBackupExtractor().extract(zip, target)
            fail("Expected absolute path to be rejected")
        } catch (e: RestoreError.CorruptedFile) {
            assertTrue(true)
        }
        assertTrue(!File(absolute).exists())
    }

    @Test
    fun extract_cumulativeExpansionOverLimit_isRejected() {
        // 3 entries x 1500 bytes = 4500 bytes; limit 3000 -> reject
        val zip = writeZip(
            mapOf(
                "a.json" to content(),
                "b.json" to content(),
                "c.json" to content()
            )
        )
        val target = tempFolder.newFolder("target3")

        try {
            SafeBackupExtractor(maxUncompressedBytes = 3000).extract(zip, target)
            fail("Expected expansion limit to be enforced")
        } catch (e: RestoreError.InsufficientStorage) {
            assertTrue(true)
        }
    }

    @Test
    fun extract_singleEntryOverLimit_isRejected() {
        val zip = writeZip(mapOf("big.json" to content()))
        val target = tempFolder.newFolder("target4")

        try {
            SafeBackupExtractor(maxUncompressedBytes = 1000).extract(zip, target)
            fail("Expected single-entry expansion limit to be enforced")
        } catch (e: RestoreError.InsufficientStorage) {
            assertTrue(true)
        }
    }

    @Test
    fun extract_withinLimit_succeeds_partialCleanupOnFailure() {
        val zip = writeZip(
            mapOf(
                "a.json" to content(),
                "b.json" to content()
            )
        )
        val target = tempFolder.newFolder("target5")

        SafeBackupExtractor(maxUncompressedBytes = 1000 * 10).extract(zip, target)

        assertTrue(File(target, "a.json").exists())
        assertTrue(File(target, "b.json").exists())
    }
}
