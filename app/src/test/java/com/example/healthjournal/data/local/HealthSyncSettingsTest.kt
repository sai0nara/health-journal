package com.example.healthjournal.data.local

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Privacy-first gate for Health Connect sync: the toggle defaults to OFF
 * and persists via SharedPreferences, mirroring [UnitSettings]. RED until
 * [HealthSyncSettings] exists.
 */
class HealthSyncSettingsTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val store = mutableMapOf<String, Boolean>()

    @Before
    fun setup() {
        store.clear()
        context = mockk()
        prefs = mockk()
        editor = mockk(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns editor
        every { prefs.getBoolean(any(), any()) } answers {
            val key = it.invocation.args[0] as String
            val default = it.invocation.args[1] as Boolean
            store[key] ?: default
        }
        every { editor.putBoolean(any(), any()) } answers {
            store[it.invocation.args[0] as String] = it.invocation.args[1] as Boolean
            editor
        }
    }

    @Test
    fun read_defaultsToDisabledWhenPrefsEmpty() {
        assertFalse(HealthSyncSettings.read(context))
    }

    @Test
    fun writeEnabled_thenReadReturnsEnabled() {
        HealthSyncSettings.write(context, true)

        assertTrue(HealthSyncSettings.read(context))
    }

    @Test
    fun writeDisabled_thenReadReturnsDisabled() {
        HealthSyncSettings.write(context, true)
        HealthSyncSettings.write(context, false)

        assertFalse(HealthSyncSettings.read(context))
    }

    @Test
    fun read_corruptValueFallsBackToDisabled() {
        every { prefs.getBoolean(any(), any()) } throws ClassCastException("corrupt")

        assertFalse(HealthSyncSettings.read(context))
    }

    @Test
    fun write_persistsViaApply() {
        HealthSyncSettings.write(context, true)

        verify { editor.apply() }
    }
}
