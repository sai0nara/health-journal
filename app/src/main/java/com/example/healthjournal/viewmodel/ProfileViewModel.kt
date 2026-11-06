package com.example.healthjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthjournal.data.ProfileError
import com.example.healthjournal.data.ProfileRepository
import com.example.healthjournal.data.local.ProfileEntity
import com.example.healthjournal.data.local.ProfileRelationship
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for profile management and the active-profile switcher.
 */
data class ProfileUiState(
    val profiles: List<ProfileEntity> = emptyList(),
    val activeProfile: ProfileEntity? = null,
    val activeCount: Int = 0,
    val isBusy: Boolean = false,
    val error: ProfileError? = null
) {
    val activeProfiles: List<ProfileEntity>
        get() = profiles.filterNot { it.isArchived }

    val archivedProfiles: List<ProfileEntity>
        get() = profiles.filter { it.isArchived }
}

/**
 * Exposes profile CRUD as suspend actions returning [Result], so callers can
 * branch on [ProfileError] without parsing display strings. The repository's
 * flows stay the source of truth for the rendered list and active profile.
 */
class ProfileViewModel(
    private val repository: ProfileRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(dispatcher) {
            combine(repository.profiles, repository.activeProfile) { profiles, active ->
                ProfileUiState(
                    profiles = profiles,
                    activeProfile = active,
                    activeCount = profiles.count { !it.isArchived }
                )
            }.collect { _uiState.value = it }
        }
    }

    suspend fun create(
        name: String,
        relationship: ProfileRelationship,
        accentColor: Int?
    ): Result<ProfileEntity> = perform { repository.create(name, relationship, accentColor) }

    suspend fun update(
        id: String,
        name: String,
        relationship: ProfileRelationship,
        accentColor: Int?
    ): Result<Unit> = perform { repository.update(id, name, relationship, accentColor) }

    suspend fun archive(id: String): Result<Unit> = perform { repository.archive(id) }

    suspend fun restore(id: String): Result<Unit> = perform { repository.restore(id) }

    suspend fun deletePermanently(id: String, typedName: String): Result<Unit> =
        perform { repository.deletePermanently(id, typedName) }

    suspend fun switchTo(id: String): Result<Unit> = perform { repository.switchTo(id) }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private suspend fun <T> perform(block: suspend () -> Result<T>): Result<T> {
        _uiState.update { it.copy(isBusy = true, error = null) }
        val result = runCatching { block() }
            .getOrElse { Result.failure(it) }
        _uiState.update {
            it.copy(
                isBusy = false,
                error = result.exceptionOrNull() as? ProfileError
            )
        }
        return result
    }
}

class ProfileViewModelFactory(
    private val repository: ProfileRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
