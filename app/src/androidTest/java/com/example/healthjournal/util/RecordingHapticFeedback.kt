package com.example.healthjournal.util

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Captures every explicit haptic the UI requests through LocalHapticFeedback,
 * so tests can assert which gestures produced feedback without depending on
 * device vibration hardware (which is not observable from instrumentation).
 */
class RecordingHapticFeedback : HapticFeedback {

    private val recorded = mutableListOf<HapticFeedbackType>()

    val events: List<HapticFeedbackType>
        get() = synchronized(recorded) { recorded.toList() }

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        synchronized(recorded) { recorded += hapticFeedbackType }
    }
}
