package com.example.healthjournal.localization

import com.example.healthjournal.R
import com.example.healthjournal.domain.MeasurementField
import com.example.healthjournal.domain.ScheduledDay
import com.example.healthjournal.domain.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Every localizable taxonomy enum must expose a distinct string resource
 * that exists in BOTH catalogs, so no enum label renders English on ru-RU
 * devices (or crashes on a missing key). The canonical English `label` /
 * `name` stays untouched: those values are persisted (preset days, session
 * types, journal descriptions) and must never change with locale.
 */
class EnumLabelResTest {

    private fun resolveResDir(): File {
        val candidates = listOf(
            File("src/main/res"),
            File("app/src/main/res")
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate app/src/main/res from ${System.getProperty("user.dir")}")
    }

    private fun stringKeys(stringsXml: File): Set<String> {
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder().parse(stringsXml)
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length)
            .map { nodes.item(it).attributes.getNamedItem("name").nodeValue }
            .toSet()
    }

    private fun labelResIds(): List<Int> = buildList {
        WorkoutType.entries.forEach { add(it.labelRes) }
        MeasurementField.entries.forEach { add(it.labelRes) }
        ScheduledDay.entries.forEach { add(it.labelRes) }
    }

    @Test
    fun taxonomyLabelRes_areDistinct() {
        val ids = labelResIds()
        assertEquals(25, ids.size)
        assertEquals(
            "labelRes keys must be distinct across taxonomy enums, duplicates: " +
                ids.groupingBy { it }.eachCount().filter { it.value > 1 },
            ids.size,
            ids.toSet().size
        )
    }

    @Test
    fun taxonomyLabelRes_existInBothCatalogs() {
        val idToName = R.string::class.java.fields.associate { it.getInt(null) to it.name }
        val resDir = resolveResDir()
        val defaultKeys = stringKeys(File(resDir, "values/strings.xml"))
        val russianKeys = stringKeys(File(resDir, "values-ru/strings.xml"))

        val unresolved = labelResIds().filter { it !in idToName }
        assertTrue("labelRes ids with no R.string field: $unresolved", unresolved.isEmpty())

        val names = labelResIds().map { idToName.getValue(it) }
        val missingDefault = names.filter { it !in defaultKeys }
        val missingRussian = names.filter { it !in russianKeys }
        assertTrue(
            "taxonomy keys missing from values/strings.xml: $missingDefault; " +
                "missing from values-ru/strings.xml: $missingRussian",
            missingDefault.isEmpty() && missingRussian.isEmpty()
        )
    }
}
