package com.example.healthjournal.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import com.example.healthjournal.viewmodel.IJournalViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArchiveScreen(
    viewModel: IJournalViewModel,
    onBack: () -> Unit
) {
    val archivedEntries by viewModel.archivedEntries.collectAsState()
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var isSelectionMode by remember { mutableStateOf(false) }

    val onToggleSelect = { id: String ->
        selectedIds = if (selectedIds.contains(id)) {
            selectedIds - id
        } else {
            selectedIds + id
        }
        isSelectionMode = selectedIds.isNotEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSelectionMode) "${selectedIds.size} Selected" else "Archive") },
                navigationIcon = {
                    IconButton(onClick = if (isSelectionMode) { { 
                        selectedIds = emptySet()
                        isSelectionMode = false 
                    } } else onBack) {
                        Icon(
                            if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { 
                            selectedIds.forEach { viewModel.restoreEntry(it) }
                            selectedIds = emptySet()
                            isSelectionMode = false
                        }) {
                            Icon(Icons.Default.Restore, contentDescription = "Restore Selected")
                        }
                        IconButton(onClick = { 
                            viewModel.deleteEntries(selectedIds.toList())
                            selectedIds = emptySet()
                            isSelectionMode = false
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                        }
                    } else if (archivedEntries.isNotEmpty()) {
                        IconButton(onClick = { viewModel.emptyArchive() }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Empty Archive")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (archivedEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No archived entries.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(archivedEntries, key = { it.entry_id }) { entry ->
                    val isSelected = selectedIds.contains(entry.entry_id)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { 
                                    if (isSelectionMode) onToggleSelect(entry.entry_id)
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        onToggleSelect(entry.entry_id)
                                    }
                                }
                            )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelectionMode) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onToggleSelect(entry.entry_id) }
                                )
                            }
                            JournalEntryItem(
                                entry = entry,
                                onClick = { if (isSelectionMode) onToggleSelect(entry.entry_id) }
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
