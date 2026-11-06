package com.example.healthjournal.data

import android.content.Context
import com.example.healthjournal.data.local.SELF_PROFILE_ID

interface ActiveProfileStore {
    fun read(): String?
    fun write(profileId: String)
}

class SharedPreferencesActiveProfileStore(context: Context) : ActiveProfileStore {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun read(): String? = prefs.getString(KEY_ACTIVE_PROFILE_ID, null)

    override fun write(profileId: String) {
        prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, profileId).apply()
    }

    companion object {
        private const val PREFS_NAME = "profile_settings"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
    }
}

interface ActiveProfileProvider {
    val activeProfileId: kotlinx.coroutines.flow.StateFlow<String>
    fun currentProfileId(): String
}

val ActiveProfileProvider.fallbackProfileId: String
    get() = activeProfileId.value.ifBlank { SELF_PROFILE_ID }
