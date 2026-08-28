package com.example.healthjournal.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.healthjournal.export.ExportState
import com.example.healthjournal.export.ExportViewModel
import com.example.healthjournal.export.RestoreViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: ExportViewModel,
    restoreViewModel: RestoreViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val exportState by viewModel.exportState.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    
    var startDate by remember { mutableLongStateOf(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L) } // 30 days ago
    var endDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedFormat by remember { mutableStateOf("PDF") }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    LaunchedEffect(exportState) {
        if (exportState is ExportState.ReadyToShare) {
            val state = exportState as ExportState.ReadyToShare
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = state.mimeType
                putExtra(Intent.EXTRA_STREAM, state.fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Exported Data"))
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Data") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Export") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Restore") }
                )
            }
            if (selectedTab == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = "Choose the date range and format for your export.",
                        style = MaterialTheme.typography.bodyLarge
                    )

            // Date Selection
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Start Date", style = MaterialTheme.typography.labelMedium)
                            Text(sdf.format(Date(startDate)), style = MaterialTheme.typography.bodyLarge)
                        }
                        IconButton(onClick = { showStartDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Start Date")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("End Date", style = MaterialTheme.typography.labelMedium)
                            Text(sdf.format(Date(endDate)), style = MaterialTheme.typography.bodyLarge)
                        }
                        IconButton(onClick = { showEndDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select End Date")
                        }
                    }
                }
            }

            // Format Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Export Format", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedFormat == "PDF",
                        onClick = { selectedFormat = "PDF" }
                    )
                    Text("PDF (Medical Report)", modifier = Modifier.padding(start = 8.dp))
                    Spacer(modifier = Modifier.width(24.dp))
                    RadioButton(
                        selected = selectedFormat == "ZIP",
                        onClick = { selectedFormat = "ZIP" }
                    )
                    Text("ZIP (Raw Data & Media)", modifier = Modifier.padding(start = 8.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (exportState is ExportState.Generating) {
                val state = exportState as ExportState.Generating
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator()
                    Text(state.message)
                }
            } else {
                Button(
                    onClick = { viewModel.exportData(startDate, endDate, selectedFormat) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = exportState !is ExportState.Generating
                ) {
                    Text("Generate Export")
                }
            }

            if (exportState is ExportState.Error) {
                Text(
                    text = (exportState as ExportState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                }
                }
            } else {
                RestoreScreen(restoreViewModel)
            }
        }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { startDate = it }
                    showStartDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { endDate = it }
                    showEndDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
