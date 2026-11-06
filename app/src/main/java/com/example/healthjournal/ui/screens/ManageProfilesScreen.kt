package com.example.healthjournal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.healthjournal.R
import com.example.healthjournal.data.ProfileError
import com.example.healthjournal.data.ProfileRepository
import com.example.healthjournal.data.local.ProfileEntity
import com.example.healthjournal.data.local.ProfileRelationship
import com.example.healthjournal.ui.components.ProfileAvatar
import com.example.healthjournal.viewmodel.ProfileUiState
import com.example.healthjournal.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

private data class EditorDraft(
    val id: String?,
    val name: String,
    val relationship: ProfileRelationship,
    val accentColor: Int?
)

private data class ColorChoice(
    val value: Int?,
    val label: Int,
    val tag: String
)

private val profileColors = listOf(
    ColorChoice(null, R.string.profile_color_default, "default"),
    ColorChoice(0xFF2E7D32.toInt(), R.string.profile_color_green, "green"),
    ColorChoice(0xFF1565C0.toInt(), R.string.profile_color_blue, "blue"),
    ColorChoice(0xFF6A1B9A.toInt(), R.string.profile_color_purple, "purple"),
    ColorChoice(0xFFE65100.toInt(), R.string.profile_color_orange, "orange")
)

/**
 * Manage Profiles: create/edit active profiles, archive/restore them, and
 * permanently delete an archived profile after typing its exact name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageProfilesScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf<EditorDraft?>(null) }
    var deleting by remember { mutableStateOf<ProfileEntity?>(null) }
    var typedDeleteName by remember { mutableStateOf("") }

    fun openEditor(profile: ProfileEntity?) {
        viewModel.clearError()
        draft = EditorDraft(
            id = profile?.id,
            name = profile?.name.orEmpty(),
            relationship = profile?.relationship?.toRelationship() ?: ProfileRelationship.OTHER,
            accentColor = profile?.accentColor
        )
    }

    fun closeDialogs() {
        if (state.isBusy) return
        draft = null
        deleting = null
        typedDeleteName = ""
        viewModel.clearError()
    }

    draft?.let { current ->
        ProfileEditorDialog(
            draft = current,
            error = state.error,
            isBusy = state.isBusy,
            onDismiss = ::closeDialogs,
            onClearError = viewModel::clearError,
            onSave = { name, relationship, color ->
                scope.launch {
                    val editingId = current.id
                    val result = if (editingId == null) {
                        viewModel.create(name, relationship, color)
                    } else {
                        viewModel.update(editingId, name, relationship, color)
                    }
                    if (result.isSuccess) {
                        draft = null
                        viewModel.clearError()
                    }
                }
            }
        )
    }

    deleting?.let { profile ->
        AlertDialog(
            onDismissRequest = ::closeDialogs,
            modifier = Modifier.testTag("profile_delete_dialog"),
            title = { Text(stringResource(R.string.profile_delete_title, profile.name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.profile_delete_body))
                    OutlinedTextField(
                        value = typedDeleteName,
                        onValueChange = {
                            typedDeleteName = it
                            viewModel.clearError()
                        },
                        label = { Text(stringResource(R.string.profile_delete_name_label)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_delete_name_input")
                    )
                    state.error?.let {
                        Text(
                            text = it.displayMessage(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val result = viewModel.deletePermanently(profile.id, typedDeleteName)
                            if (result.isSuccess) {
                                deleting = null
                                typedDeleteName = ""
                                viewModel.clearError()
                            }
                        }
                    },
                    enabled = typedDeleteName == profile.name && !state.isBusy,
                    modifier = Modifier.testTag("profile_delete_confirm")
                ) {
                    Text(stringResource(R.string.profile_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = ::closeDialogs, enabled = !state.isBusy) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_profiles_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (state.profiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.profiles_empty))
            }
        } else {
            ProfileList(
                state = state,
                padding = padding,
                showError = draft == null && deleting == null,
                onAdd = { openEditor(null) },
                onEdit = { openEditor(it) },
                onArchive = { profile -> scope.launch { viewModel.archive(profile.id) } },
                onRestore = { profile -> scope.launch { viewModel.restore(profile.id) } },
                onDelete = { profile ->
                    viewModel.clearError()
                    typedDeleteName = ""
                    deleting = profile
                }
            )
        }
    }
}

@Composable
private fun ProfileList(
    state: ProfileUiState,
    padding: PaddingValues,
    showError: Boolean,
    onAdd: () -> Unit,
    onEdit: (ProfileEntity) -> Unit,
    onArchive: (ProfileEntity) -> Unit,
    onRestore: (ProfileEntity) -> Unit,
    onDelete: (ProfileEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .testTag("manage_profiles_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Text(
                    text = stringResource(R.string.profiles_privacy_note),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (showError) {
            state.error?.let { error ->
                item {
                    Text(
                        text = error.displayMessage(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.profiles_active_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .testTag("manage_profiles_active_section")
                    .padding(top = 8.dp)
            )
        }
        items(state.activeProfiles, key = { it.id }) { profile ->
            ProfileRow(
                profile = profile,
                isActive = profile.id == state.activeProfile?.id,
                canArchive = state.activeCount > 1,
                isBusy = state.isBusy,
                onEdit = { onEdit(profile) },
                onArchive = { onArchive(profile) },
                onRestore = null,
                onDelete = null
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = onAdd,
                    enabled = !state.isBusy && state.activeCount < ProfileRepository.MAX_ACTIVE_PROFILES,
                    modifier = Modifier.testTag("manage_profiles_add")
                ) {
                    Text(stringResource(R.string.profiles_add))
                }
                if (state.activeCount >= ProfileRepository.MAX_ACTIVE_PROFILES) {
                    Text(
                        text = stringResource(R.string.profiles_limit_reached),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (state.archivedProfiles.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.profiles_archived_section),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .testTag("manage_profiles_archived_section")
                        .padding(top = 12.dp)
                )
            }
            items(state.archivedProfiles, key = { it.id }) { profile ->
                ProfileRow(
                    profile = profile,
                    isActive = false,
                    canArchive = false,
                    isBusy = state.isBusy,
                    onEdit = { onEdit(profile) },
                    onArchive = null,
                    onRestore = { onRestore(profile) },
                    onDelete = { onDelete(profile) }
                )
            }
        }
    }
}

@Composable
private fun ProfileRow(
    profile: ProfileEntity,
    isActive: Boolean,
    canArchive: Boolean,
    isBusy: Boolean,
    onEdit: () -> Unit,
    onArchive: (() -> Unit)?,
    onRestore: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Card(modifier = Modifier.testTag("manage_profile_row_${profile.id}")) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileAvatar(
                    name = profile.name,
                    accentColor = profile.accentColor,
                    size = 40.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(profile.relationship.toRelationship().toLabel()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isActive) {
                    Text(
                        text = stringResource(R.string.profile_active_badge),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onEdit,
                    enabled = !isBusy,
                    modifier = Modifier.testTag("manage_profile_edit_${profile.id}")
                ) {
                    Text(stringResource(R.string.profile_action_edit))
                }
                onArchive?.let { archive ->
                    TextButton(
                        onClick = archive,
                        enabled = !isBusy && canArchive,
                        modifier = Modifier.testTag("manage_profile_archive_${profile.id}")
                    ) {
                        Text(stringResource(R.string.profile_action_archive))
                    }
                }
                onRestore?.let { restore ->
                    TextButton(
                        onClick = restore,
                        enabled = !isBusy,
                        modifier = Modifier.testTag("manage_profile_restore_${profile.id}")
                    ) {
                        Text(stringResource(R.string.profile_action_restore))
                    }
                }
                onDelete?.let { delete ->
                    TextButton(
                        onClick = delete,
                        enabled = !isBusy,
                        modifier = Modifier.testTag("manage_profile_delete_${profile.id}")
                    ) {
                        Text(stringResource(R.string.profile_action_delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileEditorDialog(
    draft: EditorDraft,
    error: ProfileError?,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
    onSave: (String, ProfileRelationship, Int?) -> Unit
) {
    var name by remember(draft) { mutableStateOf(draft.name) }
    var relationship by remember(draft) { mutableStateOf(draft.relationship) }
    var color by remember(draft) { mutableStateOf(draft.accentColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("profile_editor_dialog"),
        title = {
            Text(
                stringResource(
                    if (draft.id == null) {
                        R.string.profile_editor_create_title
                    } else {
                        R.string.profile_editor_edit_title
                    }
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onClearError()
                    },
                    label = { Text(stringResource(R.string.profile_name_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )
                Text(
                    text = stringResource(R.string.profile_relationship_label),
                    style = MaterialTheme.typography.labelLarge
                )
                ProfileRelationship.entries.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { option ->
                            FilterChip(
                                selected = relationship == option,
                                onClick = {
                                    relationship = option
                                    onClearError()
                                },
                                label = { Text(stringResource(option.toLabel())) },
                                modifier = Modifier.testTag(
                                    "profile_relationship_${option.name.lowercase()}"
                                )
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.profile_color_label),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    profileColors.forEach { choice ->
                        val selected = color == choice.value
                        val description = stringResource(choice.label)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    choice.value?.let { Color(it) }
                                        ?: MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    shape = CircleShape
                                )
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        color = choice.value
                                        onClearError()
                                    }
                                )
                                .testTag("profile_color_${choice.tag}")
                                .semantics { contentDescription = description }
                        )
                    }
                }
                error?.let {
                    Text(
                        text = it.displayMessage(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, relationship, color) },
                enabled = !isBusy,
                modifier = Modifier.testTag("profile_editor_save")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isBusy,
                modifier = Modifier.testTag("profile_editor_cancel")
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun ProfileError.displayMessage(): String = stringResource(
    when (this) {
        ProfileError.BlankName -> R.string.profile_error_blank_name
        ProfileError.ActiveLimit -> R.string.profile_error_active_limit
        ProfileError.SelfExists -> R.string.profile_error_self_exists
        ProfileError.LastActive -> R.string.profile_error_last_active
        ProfileError.NotArchived -> R.string.profile_error_not_archived
        ProfileError.NameMismatch -> R.string.profile_error_name_mismatch
        ProfileError.Archived -> R.string.profile_error_archived
        ProfileError.NotFound -> R.string.profile_error_not_found
    }
)

private fun String.toRelationship(): ProfileRelationship =
    ProfileRelationship.entries.firstOrNull { it.name == this } ?: ProfileRelationship.OTHER

private fun ProfileRelationship.toLabel(): Int = when (this) {
    ProfileRelationship.SELF -> R.string.profile_relationship_self
    ProfileRelationship.SIBLING -> R.string.profile_relationship_sibling
    ProfileRelationship.CHILD -> R.string.profile_relationship_child
    ProfileRelationship.PARENT -> R.string.profile_relationship_parent
    ProfileRelationship.OTHER -> R.string.profile_relationship_other
}
