package com.example.healthjournal.ui.format

import android.content.Context
import com.example.healthjournal.R
import com.example.healthjournal.domain.WarmupHistoryFormatter
import java.util.Locale

/**
 * Resource-backed [WarmupHistoryFormatter] for the running app. Resolves the
 * persisted history line in the context's locale at finish time; the stored
 * journal text does not change afterward.
 */
class AndroidWarmupHistoryFormatter(private val context: Context) : WarmupHistoryFormatter {

    override fun format(name: String, durationSeconds: Int, completed: Boolean): String {
        val minutesText = if (durationSeconds % 60 == 0) {
            context.getString(R.string.workout_minutes_whole, durationSeconds / 60)
        } else {
            context.getString(
                R.string.workout_minutes_fractional,
                String.format(locale(), "%.1f", durationSeconds / 60.0)
            )
        }
        val outcome = context.getString(
            if (completed) R.string.warmup_outcome_completed
            else R.string.warmup_outcome_skipped
        )
        return context.getString(R.string.workout_warmup_history, name, minutesText, outcome)
    }

    private fun locale(): Locale = context.resources.configuration.locales[0]
}
