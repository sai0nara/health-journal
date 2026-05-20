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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
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
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
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
                if (entry.photo_url != null) {
                    capturedImageUri = Uri.parse(entry.photo_url)
                }
            }
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) {
            // Only clear if it was a new attempt
            if (existingEntry?.photo_url == null) {
                capturedImageUri = null
            } else {
                capturedImageUri = Uri.parse(existingEntry?.photo_url)
            }
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            capturedImageUri?.let { cameraLauncher.launch(it) }
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
        capturedImageUri = uri

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun savePersistentPhoto(tempUri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(tempUri)
            val fileName = "photo_${System.currentTimeMillis()}.jpg"
            val photoDir = File(context.filesDir, "photos")
            photoDir.mkdirs()
            val persistentFile = File(photoDir, fileName)
            
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
            
            capturedImageUri?.let { uri ->
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Attached photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { capturedImageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove photo")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            EnrichmentPanel(
                onAttachPhotoClick = { launchCamera() },
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
                        
                        if (entryId == null) {
                            val persistentUri = capturedImageUri?.let { savePersistentPhoto(it) }
                            viewModel.addEntry(
                                description = description,
                                timestamp = finalTimestamp,
                                photoUrl = persistentUri
                            )
                        } else {
                            existingEntry?.let {
                                val persistentUri = if (capturedImageUri?.toString() == it.photo_url) {
                                    it.photo_url
                                } else {
                                    capturedImageUri?.let { uri -> savePersistentPhoto(uri) }
                                }
                                viewModel.updateEntry(
                                    it.copy(
                                        description = description,
                                        timestamp = finalTimestamp,
                                        photo_url = persistentUri
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
