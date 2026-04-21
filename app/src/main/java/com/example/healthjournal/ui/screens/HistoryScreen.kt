package com.example.healthjournal.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.viewmodel.IJournalViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: IJournalViewModel, onAddEntryClick: () -> Unit) {
    val entries by viewModel.allEntries.collectAsState()
    val isSignedIn by viewModel.isUserSignedIn.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val authorizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.syncNow()
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Journal") },
                actions = {
                    if (isSignedIn) {
                        IconButton(onClick = { viewModel.syncNow() }) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync Now")
                        }
                    } else {
                        TextButton(onClick = {
                            viewModel.signIn(context) { pendingIntent ->
                                authorizationLauncher.launch(
                                    IntentSenderRequest.Builder(pendingIntent).build()
                                )
                            }
                        }) {
                            Text("Sign In")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEntryClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            syncStatus?.let {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        viewModel.syncNow()
                        // Simulate some delay for visual feedback
                        kotlinx.coroutines.delay(1000)
                        isRefreshing = false
                    }
                },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                if (entries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No entries yet. Start by adding one!", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(entries) { entry ->
                            JournalEntryItem(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JournalEntryItem(entry: JournalEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(entry.timestamp)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = entry.description, fontSize = 16.sp)
            }
            
            if (entry.isSynced) {
                Icon(
                    Icons.Default.CloudDone,
                    contentDescription = "Cloud Synced",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.Default.CloudSync,
                    contentDescription = "Local Only",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
