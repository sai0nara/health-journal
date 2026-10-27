package com.example.healthjournal.domain

/**
 * Formats the one-line warm-up summary persisted into a finished workout's
 * journal description. Production injects a resource-backed implementation so
 * the line follows the device locale; [English] preserves the canonical format
 * for JVM tests and callers without a resource context.
 */
fun interface WarmupHistoryFormatter {
    fun format(name: String, durationSeconds: Int, completed: Boolean): String

    companion object {
        val English: WarmupHistoryFormatter = WarmupHistoryFormatter { name, seconds, completed ->
            val minutesText = if (seconds % 60 == 0) {
                "${seconds / 60} min"
            } else {
                "%.1f min".format(seconds / 60.0)
            }
            val outcome = if (completed) "Completed" else "Skipped"
            "Warm-up: $name · $minutesText · $outcome"
        }
    }
}
