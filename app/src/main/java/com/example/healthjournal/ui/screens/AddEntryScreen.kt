package com.example.healthjournal.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.material.icons.filled.Description
import coil.compose.AsyncImage
import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.ui.components.EnrichmentPanel
import com.example.healthjournal.viewmodel.IJournalViewModel
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
    var description by remember { mutableStateOf("") }
    var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var attachedPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var attachedFiles by remember { mutableStateOf<List<AttachmentData>>(emptyList()) }
    var existingEntry by remember { mutableStateOf<JournalEntry?>(null) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedTimestamp,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }
        }
    )
    val timePickerState = rememberTimePickerState(
        initialHour = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }.get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }.get(Calendar.MINUTE)
    )

    // Load existing entry if editing
    LaunchedEffect(entryId) {
        if (entryId != null) {
            val entry = viewModel.getEntryById(entryId)
            if (entry != null) {
                existingEntry = entry
                description = entry.description
                selectedTimestamp = entry.timestamp
                attachedPhotoUris = entry.photo_urls.map { Uri.parse(it) }
                attachedFiles = entry.attachments
            }
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        // No action needed here, the URI is already added to state in launchCamera
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
        // This is a bit simplified, ideally we'd re-launch the camera if granted
    }

    fun launchCamera() {
        val file = File(context.cacheDir, "images/temp_photo_${System.currentTimeMillis()}.jpg")
        file.parentFile?.mkdirs()
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        attachedPhotoUris = attachedPhotoUris + uri

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun savePersistentFile(uri: Uri, isPhoto: Boolean): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val fileName = if (isPhoto) "photo_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg" 
                           else "doc_${System.currentTimeMillis()}_${UUID.randomUUID()}"
            val dir = File(context.filesDir, if (isPhoto) "photos" else "attachments")
            dir.mkdirs()
            val persistentFile = File(dir, fileName)
            
            inputStream?.use { input ->
                persistentFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(persistentFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val calendar = Calendar.getInstance()
                        val currentCalendar = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                        calendar.timeInMillis = it
                        calendar.set(Calendar.HOUR_OF_DAY, currentCalendar.get(Calendar.HOUR_OF_DAY))
                        calendar.set(Calendar.MINUTE, currentCalendar.get(Calendar.MINUTE))
                        
                        // Re-validate against current time
                        if (calendar.timeInMillis <= System.currentTimeMillis()) {
                            selectedTimestamp = calendar.timeInMillis
                        }
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
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
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = selectedTimestamp
                    calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    calendar.set(Calendar.MINUTE, timePickerState.minute)
                    
                    // Re-validate against current time
                    if (calendar.timeInMillis <= System.currentTimeMillis()) {
                        selectedTimestamp = calendar.timeInMillis
                    }
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Select Time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (entryId == null) "New Entry" else "Edit Entry") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedTimestamp)))
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(selectedTimestamp)))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("How are you feeling today?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5
            )
            
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
                                modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.small),
                                contentScale = ContentScale.Crop
                            )
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
                                    Text(file.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
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

            Spacer(modifier = Modifier.height(16.dp))
            
            EnrichmentPanel(
                onAttachPhotoClick = { photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onAttachFileClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                onSyncHealthClick = { /* TODO: Implement Health Connect */ }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (description.isNotBlank()) {
                        val finalTimestamp = if (selectedTimestamp > System.currentTimeMillis()) {
                            System.currentTimeMillis()
                        } else {
                            selectedTimestamp
                        }
                        
                        // Save all files persistently
                        val persistentPhotoUrls = attachedPhotoUris.map { uri ->
                            if (uri.toString().startsWith("file:///")) uri.toString()
                            else savePersistentFile(uri, true) ?: ""
                        }.filter { it.isNotBlank() }

                        val persistentAttachments = attachedFiles.map { file ->
                            if (file.uri.startsWith("file:///")) file
                            else {
                                val persistentUri = savePersistentFile(Uri.parse(file.uri), false)
                                file.copy(uri = persistentUri ?: "")
                            }
                        }.filter { it.uri.isNotBlank() }

                        if (entryId == null) {
                            viewModel.addEntry(
                                description = description,
                                timestamp = finalTimestamp,
                                photoUrls = persistentPhotoUrls,
                                attachments = persistentAttachments
                            )
                        } else {
                            existingEntry?.let {
                                viewModel.updateEntry(
                                    it.copy(
                                        description = description,
                                        timestamp = finalTimestamp,
                                        photo_urls = persistentPhotoUrls,
                                        attachments = persistentAttachments
                                    )
                                )
                            }
                        }
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (entryId == null) "Save Entry" else "Update Entry")
            }
        }
    }
}
