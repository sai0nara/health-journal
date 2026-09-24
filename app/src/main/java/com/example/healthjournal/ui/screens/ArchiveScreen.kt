package com.example.healthjournal.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.healthjournal.R
import com.example.healthjournal.ui.components.JournalEntryItem
import com.example.healthjournal.ui.components.SharedSearchBar
import com.example.healthjournal.ui.components.TagSelectionRow
import com.example.healthjournal.viewmodel.IJournalViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArchiveScreen(
    viewModel: IJournalViewModel,
    onBack: () -> Unit,
    onEntryClick: (String) -> Unit
) {
    val archivedEntries by viewModel.reactiveArchivedEntries.collectAsState()
    val searchQuery by viewModel.archiveSearchQuery.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var expandedImageUri by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEmptyArchiveSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val onToggleSelect = { id: String ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        selectedIds = if (selectedIds.contains(id)) {
            selectedIds - id
        } else {
            selectedIds + id
        }
        isSelectionMode = selectedIds.isNotEmpty()
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.archive_delete_title)) },
            text = { Text(stringResource(R.string.archive_delete_message, selectedIds.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEntries(selectedIds.toList())
                        selectedIds = emptySet()
                        isSelectionMode = false
                        showDeleteConfirmDialog = false
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.archive_snackbar_deleted)) }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.archive_delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showEmptyArchiveSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEmptyArchiveSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.archive_empty_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.archive_empty_body),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        viewModel.emptyArchive()
                        showEmptyArchiveSheet = false
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.archive_snackbar_emptied)) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.archive_empty_confirm))
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { showEmptyArchiveSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.archive_keep_entries))
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    if (isSelectionMode) {
                        Text(stringResource(R.string.archive_selected_format, selectedIds.size))
                    } else {
                        Text(stringResource(R.string.archive_title))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectionMode) {
                            selectedIds = emptySet()
                            isSelectionMode = false
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.action_back_label)
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { 
                            selectedIds.forEach { viewModel.restoreEntry(it) }
                            selectedIds = emptySet()
                            isSelectionMode = false
                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.archive_snackbar_restored)) }
                        }) {
                            Icon(Icons.Default.Restore, contentDescription = stringResource(R.string.archive_cd_restore))
                        }
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.archive_cd_delete))
                        }
                    } else if (archivedEntries.isNotEmpty()) {
                        IconButton(onClick = { showEmptyArchiveSheet = true }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = stringResource(R.string.archive_cd_empty))
                        }
                    }
                },
                colors = if (isSelectionMode) {
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                } else {
                    TopAppBarDefaults.topAppBarColors()
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SharedSearchBar(
                query = searchQuery,
                onQueryChanged = { viewModel.setArchiveSearchQuery(it) },
                placeholder = stringResource(R.string.archive_search_placeholder)
            )

            TagSelectionRow(
                selectedTags = selectedTags,
                onTagToggle = { tag -> viewModel.toggleTag(tag) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (archivedEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory2, 
                            contentDescription = null, 
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.archive_empty_state),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(archivedEntries, key = { it.entry_id }) { entry ->
                        val isSelected = selectedIds.contains(entry.entry_id)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .testTag("archive_entry_${entry.entry_id}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelectionMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { onToggleSelect(entry.entry_id) },
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                                JournalEntryItem(
                                    entry = entry,
                                    onClick = { if (isSelectionMode) onToggleSelect(entry.entry_id) else onEntryClick(entry.entry_id) },
                                    onPhotoClick = { expandedImageUri = it },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            isSelectionMode = true
                                            onToggleSelect(entry.entry_id)
                                        }
                                    }
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (expandedImageUri != null) {
        Dialog(
            onDismissRequest = { expandedImageUri = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim)
                    .clickable { expandedImageUri = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = expandedImageUri,
                    contentDescription = stringResource(R.string.history_cd_expanded_image),
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
        }
    }
}
