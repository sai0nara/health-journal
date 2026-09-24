package com.example.healthjournal.localization

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Enforces the Russian-localization requirement: every string key in the
 * default catalog must have a Russian translation, so no English text leaks
 * through on ru-RU devices. RED until values-ru/strings.xml is authored.
 */
class LocalizationParityTest {

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

    @Test
    fun russianCatalog_coversEveryDefaultKey() {
        val resDir = resolveResDir()
        val defaultKeys = stringKeys(File(resDir, "values/strings.xml"))

        val russianXml = File(resDir, "values-ru/strings.xml")
        assertTrue(
            "values-ru/strings.xml does not exist yet - author the Russian catalog",
            russianXml.isFile
        )

        val missing = defaultKeys - stringKeys(russianXml)
        assertTrue(
            "Russian catalog is missing ${missing.size} keys:\n" +
                missing.sorted().joinToString("\n"),
            missing.isEmpty()
        )
    }
}
