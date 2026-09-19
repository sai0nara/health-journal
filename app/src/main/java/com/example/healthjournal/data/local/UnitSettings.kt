package com.example.healthjournal.data.local

import android.content.Context

/**
 * Persisted app-wide unit-system preference (metric/imperial), backed by a
 * private SharedPreferences file. The same choice the Personal Card exposes
 * is read from here by the workout quick pad, so the units stay in sync
 * across screens without plumbing a shared store through every ViewModel.
 */
object UnitSettings {

    private const val PREFS_NAME = "unit_settings"
    private const val KEY_UNIT_SYSTEM = "unit_system"

    fun read(context: Context): UnitSystem =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_UNIT_SYSTEM, null)
            ?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() }
            ?: UnitSystem.METRIC

    fun write(context: Context, unitSystem: UnitSystem) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_UNIT_SYSTEM, unitSystem.name)
            .apply()
    }
}