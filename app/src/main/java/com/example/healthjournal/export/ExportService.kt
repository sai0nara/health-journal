package com.example.healthjournal.export

import java.io.File

interface ExportService {
    fun cleanupCache()
}

class ExportServiceImpl(private val baseDir: File) : ExportService {
    override fun cleanupCache() {
        val exportsDir = File(baseDir, "exports")
        if (exportsDir.exists()) {
            exportsDir.listFiles()?.forEach { it.delete() }
        }
    }
}
