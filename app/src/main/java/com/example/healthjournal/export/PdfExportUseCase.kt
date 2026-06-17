package com.example.healthjournal.export

import android.content.Context
import android.net.Uri
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.JournalEntry
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.properties.TextAlignment
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PdfExportUseCase(
    private val context: Context,
    private val repository: JournalRepository,
    private val exportsDir: File,
    private val imageResizer: ImageResizer
) {
    suspend fun execute(startDate: Long, endDate: Long): File {
        exportsDir.mkdirs()
        val pdfFile = File(exportsDir, "health_journal_report_${System.currentTimeMillis()}.pdf")
        
        // Filter entries by date
        val entries = repository.allEntries.first().filter { 
            it.timestamp in startDate..endDate 
        }.sortedByDescending { it.timestamp }

        val writer = PdfWriter(pdfFile)
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        try {
            // Title
            document.add(Paragraph("Health Journal Report")
                .setBold()
                .setFontSize(24f)
                .setTextAlignment(TextAlignment.CENTER))

            // Date Range
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            document.add(Paragraph("Period: ${sdf.format(Date(startDate))} - ${sdf.format(Date(endDate))}")
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20f))

            entries.forEach { entry ->
                // Entry Date Header
                val entryDate = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
                document.add(Paragraph(entryDate)
                    .setBold()
                    .setFontSize(14f)
                    .setMarginTop(10f))

                // Entry Content
                val plaintext = android.text.Html.fromHtml(entry.description, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
                document.add(Paragraph(plaintext)
                    .setFontSize(11f))

                // Metrics
                if (entry.bp_systolic != null) {
                    val metrics = "BP: ${entry.bp_systolic?.toInt()}/${entry.bp_diastolic?.toInt()} mmHg | HR: ${entry.heart_rate_avg} bpm | Sleep: ${entry.sleep_hours}h"
                    document.add(Paragraph(metrics)
                        .setItalic()
                        .setFontSize(10f)
                        .setFontColor(com.itextpdf.kernel.colors.ColorConstants.DARK_GRAY))
                }

                // Photos
                entry.photo_urls?.forEach { photoUrl ->
                    val imageFile = imageResizer.downsampleImage(Uri.parse(photoUrl), 600)
                    if (imageFile != null) {
                        try {
                            val data = ImageDataFactory.create(imageFile.absolutePath)
                            val img = Image(data)
                            img.setMaxWidth(300f)
                            document.add(img)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            imageFile.delete()
                        }
                    }
                }
                
                // Separator line or spacing
                document.add(Paragraph("\n").setFontSize(5f))
            }
        } finally {
            document.close()
        }
        
        return pdfFile
    }
}
