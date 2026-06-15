package com.example.healthjournal.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import coil.compose.AsyncImage
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.ui.components.EnrichmentPanel
import com.example.healthjournal.ui.components.RichTextToolbar
import com.example.healthjournal.viewmodel.IJournalViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    viewModel: IJournalViewModel,
    onBack: () -> Unit,
    entryId: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val richTextState = rememberRichTextState()
    var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var attachedPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var attachedFiles by remember { mutableStateOf<List<AttachmentData>>(emptyList()) }
    var existingEntry by remember { mutableStateOf<JournalEntry?>(null) }
    
    // Health Metrics State
    var bpSystolic by remember { mutableStateOf<Double?>(null) }
    var bpDiastolic by remember { mutableStateOf<Double?>(null) }
    var heartRate by remember { mutableStateOf<Int?>(null) }
    var sleepHours by remember { mutableStateOf<Float?>(null) }
    var isHealthSyncing by remember { mutableStateOf(false) }

    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var expandedImageUri by remember { mutableStateOf<Uri?>(null) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedTimestamp
    )
    val calendar = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE)
    )

    // Health Connect Permission Launcher
    val healthPermissionsLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        android.util.Log.d("AddEntryScreen", "Health permissions result: $granted")
        if (granted.containsAll(viewModel.healthPermissions)) {
            scope.launch {
                isHealthSyncing = true
                val result = viewModel.syncHealthData(selectedTimestamp)
                bpSystolic = result.bpSystolic
                bpDiastolic = result.bpDiastolic
                heartRate = result.heartRate
                sleepHours = result.sleepHours
                isHealthSyncing = false
            }
        } else {
            android.util.Log.w("AddEntryScreen", "Not all health permissions granted. Required: ${viewModel.healthPermissions}, Granted: $granted")
            android.widget.Toast.makeText(context, "Health permissions required for sync", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Load existing entry if editing
    LaunchedEffect(entryId) {
        if (entryId != null) {
            val entry = viewModel.getEntryById(entryId)
            if (entry != null) {
                existingEntry = entry
                richTextState.setHtml(entry.description)
                selectedTimestamp = entry.timestamp
                attachedPhotoUris = entry.photo_urls?.map { Uri.parse(it) } ?: emptyList()
                attachedFiles = entry.attachments ?: emptyList()
                bpSystolic = entry.bp_systolic
                bpDiastolic = entry.bp_diastolic
                heartRate = entry.heart_rate_avg
                sleepHours = entry.sleep_hours
            }
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraTempUri?.let { uri ->
                attachedPhotoUris = attachedPhotoUris + uri
            }
        }
        cameraTempUri = null
    }

    // Multi-Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            attachedPhotoUris = attachedPhotoUris + uris
        }
    }

    // File Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val name = c.getString(c.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
                    val mimeType = context.contentResolver.getType(it) ?: "application/octet-stream"
                    attachedFiles = attachedFiles + AttachmentData(name, it.toString(), mimeType)
                }
            }
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraTempUri?.let { cameraLauncher.launch(it) }
        }
    }

    fun launchCamera() {
        val file = File(context.cacheDir, "images/temp_photo_${System.currentTimeMillis()}.jpg")
        file.parentFile?.mkdirs()
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        cameraTempUri = uri

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val newCal = Calendar.getInstance().apply { timeInMillis = it }
                        calendar.set(Calendar.YEAR, newCal.get(Calendar.YEAR))
                        calendar.set(Calendar.MONTH, newCal.get(Calendar.MONTH))
                        calendar.set(Calendar.DAY_OF_MONTH, newCal.get(Calendar.DAY_OF_MONTH))
                        selectedTimestamp = calendar.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    calendar.set(Calendar.MINUTE, timePickerState.minute)
                    selectedTimestamp = calendar.timeInMillis
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (entryId == null) "Add Entry" else "Edit Entry") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (existingEntry?.isArchived == true) {
                        IconButton(onClick = {
                            viewModel.restoreEntry(existingEntry!!.entry_id)
                            existingEntry = existingEntry?.copy(isArchived = false)
                        }) {
                            Icon(Icons.Default.Restore, contentDescription = "Unarchive")
                        }
                    }
                }
            )
        }
    ) { padding ->
        val isReadOnly = existingEntry?.isArchived == true

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (!isReadOnly) {
                RichTextToolbar(
                    state = richTextState,
                    onAttachClick = { launchCamera() } // Or other media action
                )
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Date and Time Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (!isReadOnly) showDatePicker = true },
                        modifier = Modifier.weight(1f),
                        enabled = !isReadOnly
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedTimestamp)))
                    }
                    
                    OutlinedButton(
                        onClick = { if (!isReadOnly) showTimePicker = true },
                        modifier = Modifier.weight(1f),
                        enabled = !isReadOnly
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(selectedTimestamp)))
                    }
                }

                if (isReadOnly) {
                    Text(
                        text = richTextState.annotatedString,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    RichTextEditor(
                        state = richTextState,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                        label = { Text("How are you feeling today?") }
                    )
                }
            
            // Health Metrics Section
            if (bpSystolic != null || heartRate != null || sleepHours != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (bpSystolic != null && bpDiastolic != null) {
                            HealthMetricItem(
                                icon = Icons.Default.MonitorHeart, 
                                value = "${bpSystolic?.toInt()}/${bpDiastolic?.toInt()}", 
                                label = "BP (mmHg)"
                            )
                        }
                        heartRate?.let {
                            HealthMetricItem(icon = Icons.Default.Favorite, value = "$it", label = "Avg HR")
                        }
                        sleepHours?.let {
                            HealthMetricItem(icon = Icons.Default.Bedtime, value = "%.1fh".format(it), label = "Sleep")
                        }
                    }
                }
            }

            if (attachedPhotoUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Photos", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(attachedPhotoUris.size) { index ->
                        val uri = attachedPhotoUris[index]
                        Box(modifier = Modifier.size(100.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable { expandedImageUri = uri },
                                contentScale = ContentScale.Crop
                            )
                            if (!isReadOnly) {
                                IconButton(
                                    onClick = { attachedPhotoUris = attachedPhotoUris.filterIndexed { i, _ -> i != index } },
                                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp),
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
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
                            .background(Color.Black)
                            .clickable { expandedImageUri = null },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = expandedImageUri,
                            contentDescription = "Expanded Image",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            if (attachedFiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Attachments", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    attachedFiles.forEachIndexed { index, file ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    if (file.mimeType.startsWith("image/")) {
                                        AsyncImage(
                                            model = file.uri,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.small),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Text(file.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    if (!isReadOnly) {
                                        IconButton(
                                            onClick = { attachedFiles = attachedFiles.filterIndexed { i, _ -> i != index } },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                        }
                                    }
                            }
                        }
                    }
                }
            }

            if (!isReadOnly) {
                Spacer(modifier = Modifier.height(16.dp))
                
                EnrichmentPanel(
                    onCameraClick = { launchCamera() },
                    onGalleryClick = { photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onAttachFileClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                    onSyncHealthClick = { 
                        scope.launch {
                            val availability = viewModel.checkHealthAvailability()
                            if (availability != HealthConnectClient.SDK_AVAILABLE) {
                                android.widget.Toast.makeText(context, "Health Connect is not available on this device", android.widget.Toast.LENGTH_LONG).show()
                                return@launch
                            }

                            if (viewModel.hasHealthPermissions()) {
                                isHealthSyncing = true
                                val result = viewModel.syncHealthData(selectedTimestamp)
                                bpSystolic = result.bpSystolic
                                bpDiastolic = result.bpDiastolic
                                heartRate = result.heartRate
                                sleepHours = result.sleepHours
                                isHealthSyncing = false
                            } else {
                                android.util.Log.d("AddEntryScreen", "Launching Health permissions for: ${viewModel.healthPermissions}")
                                healthPermissionsLauncher.launch(viewModel.healthPermissions)
                            }
                        }
                    }
                )
                
                if (isHealthSyncing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                    val descriptionHtml = richTextState.toHtml()
                    if (descriptionHtml.isNotBlank()) {
                        scope.launch {
                            val finalTimestamp = if (selectedTimestamp > System.currentTimeMillis()) {
                                System.currentTimeMillis()
                            } else {
                                selectedTimestamp
                            }
                            
                            // Save all files persistently
                            val persistentPhotoUrls = attachedPhotoUris.map { uri ->
                                if (uri.scheme == "file") uri.toString()
                                else viewModel.savePersistentFile(uri, true) ?: ""
                            }.filter { it.isNotBlank() }

                            val persistentAttachments = attachedFiles.map { file ->
                                val fileUri = Uri.parse(file.uri)
                                if (fileUri.scheme == "file") file
                                else {
                                    val persistentUri = viewModel.savePersistentFile(fileUri, false)
                                    if (persistentUri == null) {
                                        android.widget.Toast.makeText(context, "Failed to save attachment: ${file.name}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    file.copy(uri = persistentUri ?: "")
                                }
                            }.filter { it.uri.isNotBlank() }

                            if (entryId == null) {
                                viewModel.addEntry(
                                    description = descriptionHtml,
                                    timestamp = finalTimestamp,
                                    photoUrls = persistentPhotoUrls,
                                    attachments = persistentAttachments,
                                    bpSystolic = bpSystolic,
                                    bpDiastolic = bpDiastolic,
                                    heartRate = heartRate,
                                    sleepHours = sleepHours
                                )
                            } else {
                                existingEntry?.let {
                                    viewModel.updateEntry(
                                        it.copy(
                                            description = descriptionHtml,
                                            timestamp = finalTimestamp,
                                            photo_urls = persistentPhotoUrls,
                                            attachments = persistentAttachments,
                                            bp_systolic = bpSystolic,
                                            bp_diastolic = bpDiastolic,
                                            heart_rate_avg = heartRate,
                                            sleep_hours = sleepHours
                                        )
                                    )
                                }
                            }
                            onBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (entryId == null) "Save Entry" else "Update Entry")
            }
        }
    }
}
}
}

@Composable
fun HealthMetricItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}
