package com.example.healthjournal.data

import com.example.healthjournal.data.local.SELF_PROFILE_ID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Test provider that can switch profiles without a Room preference. */
class FakeActiveProfileProvider(initialProfileId: String = SELF_PROFILE_ID) : ActiveProfileProvider {

    private val _activeProfileId = MutableStateFlow(initialProfileId)
    override val activeProfileId: StateFlow<String> = _activeProfileId

    override fun currentProfileId(): String = _activeProfileId.value

    fun switchTo(profileId: String) {
        _activeProfileId.value = profileId
    }
}
