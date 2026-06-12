package com.example.healthjournal.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for JournalTypeConverters, specifically verifying
 * correct serialization/deserialization of AttachmentData with
 * the new isLocalOnly field and syncStatus handling.
 */
class JournalTypeConvertersTest {

    private val converters = JournalTypeConverters()

    @Test
    fun fromAttachmentList_serializesCorrectly() {
        val attachments = listOf(
            AttachmentData(
                name = "photo.jpg",
                uri = "/data/files/photo.jpg",
                mimeType = "image/jpeg",
                isLocalOnly = true
            ),
            AttachmentData(
                name = "report.pdf",
                uri = "https://cloud.example.com/report.pdf",
                mimeType = "application/pdf",
                isLocalOnly = false
            )
        )

        val json = converters.fromAttachmentList(attachments)
        val deserialized = converters.toAttachmentList(json)

        assertEquals(2, deserialized.size)
        assertEquals("photo.jpg", deserialized[0].name)
        assertTrue(deserialized[0].isLocalOnly == true)
        assertEquals("report.pdf", deserialized[1].name)
        assertFalse(deserialized[1].isLocalOnly == true)
    }

    @Test
    fun toAttachmentList_returnsEmptyListForNull() {
        val result = converters.toAttachmentList(null)
        assertTrue(result.isEmpty())
    }

    @Test
    fun toAttachmentList_returnsEmptyListForInvalidJson() {
        val result = converters.toAttachmentList("{invalid-json")
        assertTrue(result.isEmpty())
    }

    @Test
    fun roundTrip_preservesIsLocalOnlyField() {
        val original = listOf(
            AttachmentData("test.jpg", "/local/test.jpg", "image/jpeg", true),
            AttachmentData("cloud.jpg", "https://cdn.com/cloud.jpg", "image/jpeg", false)
        )

        val json = converters.fromAttachmentList(original)
        val restored = converters.toAttachmentList(json)

        assertEquals(original.size, restored.size)
        for (i in original.indices) {
            assertEquals(original[i].name, restored[i].name)
            assertEquals(original[i].uri, restored[i].uri)
            assertEquals(original[i].mimeType, restored[i].mimeType)
            assertEquals(original[i].isLocalOnly, restored[i].isLocalOnly)
        }
    }

    @Test
    fun emptyAttachmentList_serializesAndDeserializes() {
        val emptyList = emptyList<AttachmentData>()

        val json = converters.fromAttachmentList(emptyList)
        val result = converters.toAttachmentList(json)

        assertTrue(result.isEmpty())
    }
}
