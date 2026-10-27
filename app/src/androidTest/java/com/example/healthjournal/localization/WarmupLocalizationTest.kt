package com.example.healthjournal.localization

import android.content.res.Configuration
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthjournal.R
import com.example.healthjournal.ui.format.AndroidWarmupHistoryFormatter
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the warm-up surfaces that are not plain Compose literals: validation
 * errors are resource ids resolved by the editor, and the persisted history line
 * is formatted through the injected resource-backed formatter.
 */
class WarmupLocalizationTest {

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val ruContext = localizedContext("ru", "RU")
    private val enContext = localizedContext("en", "US")

    private fun localizedContext(language: String, country: String) =
        targetContext.createConfigurationContext(
            Configuration(targetContext.resources.configuration).apply {
                setLocale(Locale(language, country))
            }
        )

    @Test
    fun warmupValidationMessages_areRussian() {
        assertEquals(
            "Введите название разминки",
            ruContext.getString(R.string.preset_warmup_error_name_required)
        )
        assertEquals(
            "Название разминки должно быть не длиннее 60 символов",
            ruContext.getString(R.string.preset_warmup_error_name_too_long)
        )
        assertEquals(
            "Длительность разминки должна быть от 1 до 60 минут",
            ruContext.getString(R.string.preset_warmup_error_duration_range)
        )
    }

    @Test
    fun warmupHistoryLine_isRussian() {
        val formatter = AndroidWarmupHistoryFormatter(ruContext)

        assertEquals(
            "Разминка: Rowing · 1 мин · Завершена",
            formatter.format(name = "Rowing", durationSeconds = 60, completed = true)
        )
        assertEquals(
            "Разминка: Rowing · 5 мин · Пропущена",
            formatter.format(name = "Rowing", durationSeconds = 300, completed = false)
        )
        assertEquals(
            "Разминка: Rowing · 1,5 мин · Завершена",
            formatter.format(name = "Rowing", durationSeconds = 90, completed = true)
        )
    }

    @Test
    fun warmupHistoryLine_fallsBackToEnglish() {
        val formatter = AndroidWarmupHistoryFormatter(enContext)

        assertEquals(
            "Warm-up: Rowing · 1 min · Completed",
            formatter.format(name = "Rowing", durationSeconds = 60, completed = true)
        )
    }
}
