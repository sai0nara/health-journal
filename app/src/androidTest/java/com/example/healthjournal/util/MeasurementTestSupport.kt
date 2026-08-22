package com.example.healthjournal.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.healthjournal.data.BodyMeasurementRepository
import com.example.healthjournal.viewmodel.BodyMeasurementViewModel
import io.mockk.mockk

/** Shared androidTest helper: supplies a measurement ViewModel backed by a relaxed mock repository. */
object MeasurementTestSupport {

    val factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BodyMeasurementViewModel(mockk<BodyMeasurementRepository>(relaxed = true)) as T
    }
}
