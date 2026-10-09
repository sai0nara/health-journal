package com.example.healthjournal.data.local

import android.content.Context

/**
 * Persisted opt-in gate for Health Connect sync (journal-entry Sync Health
 * plus workout read/write), backed by a private SharedPreferences file.
 * Privacy-first: absent or unreadable values resolve to disabled, so fresh
 * installs and existing users alike must explicitly enable sync in Settings
 * before the app touches Health Connect. Existing synced metric values in
 * the database are left untouched by the toggle.
 */
object HealthSyncSettings {

    private const val PREFS_NAME = "health_sync_settings"
    private const val KEY_ENABLED = "health_sync_enabled"

    fun read(context: Context): Boolean =
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false)
        }.getOrDefault(false)

    fun write(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }
}
