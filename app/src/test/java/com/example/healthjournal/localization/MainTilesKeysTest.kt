package com.example.healthjournal.localization

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Enforces the main-page-tiles requirement: every dashboard tile label
 * exists in the default catalog and has a Russian translation.
 * RED until the main_tile_* keys are authored in both catalogs.
 */
class MainTilesKeysTest {

    private val tileKeys = listOf(
        "main_tile_history",
        "main_tile_workout",
        "main_tile_measurements",
        "main_tile_presets",
        "main_tile_archive",
        "main_tile_export",
        "main_tile_personal_card",
        "main_tile_settings"
    )

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
        val names = mutableSetOf<String>()
        doc.getElementsByTagName("string").let { nodes ->
            for (i in 0 until nodes.length) {
                names += nodes.item(i).attributes.getNamedItem("name").nodeValue
            }
        }
        return names
    }

    @Test
    fun mainTileKeys_existInBothCatalogs() {
        val resDir = resolveResDir()
        val defaultKeys = stringKeys(File(resDir, "values/strings.xml"))
        val russianKeys = stringKeys(File(resDir, "values-ru/strings.xml"))

        val missingDefault = tileKeys - defaultKeys
        assertTrue(
            "Default catalog is missing tile keys:\n" + missingDefault.joinToString("\n"),
            missingDefault.isEmpty()
        )
        val missingRussian = tileKeys - russianKeys
        assertTrue(
            "Russian catalog is missing tile keys:\n" + missingRussian.joinToString("\n"),
            missingRussian.isEmpty()
        )
    }
}
