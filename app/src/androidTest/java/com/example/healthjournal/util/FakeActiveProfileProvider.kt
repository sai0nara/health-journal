package com.example.healthjournal.util

import com.example.healthjournal.data.ActiveProfileProvider
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Test provider for instrumented UI tests. */
class FakeActiveProfileProvider(initialProfileId: String = SELF_PROFILE_ID) : ActiveProfileProvider {

    private val _activeProfileId = MutableStateFlow(initialProfileId)
    override val activeProfileId: StateFlow<String> = _activeProfileId

    override fun currentProfileId(): String = _activeProfileId.value

    fun switchTo(profileId: String) {
        _activeProfileId.value = profileId
    }
}
