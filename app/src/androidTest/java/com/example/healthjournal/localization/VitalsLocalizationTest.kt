package com.example.healthjournal.localization

import android.content.res.Configuration
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthjournal.R
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the Russian copy for the manual-vitals surfaces: field labels,
 * units, action labels, and validation messages resolved by the sheet/editor.
 */
class VitalsLocalizationTest {

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
    fun vitalsLabels_areRussian() {
        assertEquals("Показатели", ruContext.getString(R.string.vitals_sheet_title))
        assertEquals("Систолическое", ruContext.getString(R.string.vitals_field_systolic))
        assertEquals("Диастолическое", ruContext.getString(R.string.vitals_field_diastolic))
        assertEquals("Пульс", ruContext.getString(R.string.vitals_field_hr))
        assertEquals("Глюкоза крови", ruContext.getString(R.string.vitals_field_glucose))
        assertEquals("Температура тела", ruContext.getString(R.string.vitals_field_temperature))
        assertEquals("Показатели здоровья", ruContext.getString(R.string.entry_health_metrics))
        assertEquals("АД", ruContext.getString(R.string.entry_metric_bp))
        assertEquals("Пульс", ruContext.getString(R.string.entry_metric_hr))
        assertEquals("Добавить показатели", ruContext.getString(R.string.entry_vitals_action_add))
        assertEquals("Изменить показатели", ruContext.getString(R.string.entry_vitals_action_edit))
    }

    @Test
    fun vitalsUnitsAndErrors_areRussian() {
        assertEquals("мм рт. ст.", ruContext.getString(R.string.entry_vitals_unit_bp))
        assertEquals("уд/мин", ruContext.getString(R.string.entry_vitals_unit_bpm))
        assertEquals("ммоль/л", ruContext.getString(R.string.entry_vitals_unit_mmol_l))
        assertEquals("ч", ruContext.getString(R.string.entry_vitals_unit_hours))
        assertEquals("Введите число", ruContext.getString(R.string.vitals_error_invalid_format))
        assertEquals("Введите целое число", ruContext.getString(R.string.vitals_error_integer_required))
        assertEquals(
            "Введите систолическое и диастолическое давление.",
            ruContext.getString(R.string.vitals_error_bp_pair)
        )
        assertEquals(
            "Систолическое должно быть выше диастолического.",
            ruContext.getString(R.string.vitals_error_systolic_not_greater)
        )
    }

    @Test
    fun vitalsLabels_fallBackToEnglish() {
        assertEquals("Vitals", enContext.getString(R.string.vitals_sheet_title))
        assertEquals("Blood glucose", enContext.getString(R.string.vitals_field_glucose))
        assertEquals("Add vitals", enContext.getString(R.string.entry_vitals_action_add))
        assertEquals("BP", enContext.getString(R.string.entry_metric_bp))
        assertEquals("HR", enContext.getString(R.string.entry_metric_hr))
    }
}
