package com.example.healthjournal.export

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ExportServiceTest {

    @Test
    fun testCleanupCache_RemovesOldFiles() {
        val tempDir = Files.createTempDirectory("export_test").toFile()
        val exportsDir = File(tempDir, "exports")
        exportsDir.mkdirs()
        
        val oldFile = File(exportsDir, "old_export.pdf")
        oldFile.createNewFile()
        
        val exportService = ExportServiceImpl(tempDir)
        exportService.cleanupCache()
        
        assertFalse("Old export file should be deleted", oldFile.exists())
    }
}
