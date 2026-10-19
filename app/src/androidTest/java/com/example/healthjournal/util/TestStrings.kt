package com.example.healthjournal.util

import androidx.annotation.StringRes
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Returns the app string exactly as the UI under test resolves it for the current
 * device locale. Assertions built on real resource ids stay locale-agnostic, so the
 * same test passes on an English or Russian device instead of hardcoding English text.
 */
fun targetString(@StringRes id: Int): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
