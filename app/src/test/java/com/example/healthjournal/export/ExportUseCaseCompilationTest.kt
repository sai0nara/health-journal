package com.example.healthjournal.export

import android.content.Context
import com.example.healthjournal.data.JournalRepository
import com.google.gson.Gson
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class ExportUseCaseCompilationTest {

    private val context: Context = mockk()
    private val repository: JournalRepository = mockk()
    private val exportsDir = File("build/tmp/exports")
    private val imageResizer: ImageResizer = mockk()
    private val gson = Gson()

    @Test
    fun testPdfExportUseCase_Exists() {
        val useCase = PdfExportUseCase(context, repository, exportsDir, imageResizer)
        assertNotNull(useCase)
    }

    @Test
    fun testZipExportUseCase_Exists() {
        val useCase = ZipExportUseCase(repository, exportsDir, gson)
        assertNotNull(useCase)
    }
}
