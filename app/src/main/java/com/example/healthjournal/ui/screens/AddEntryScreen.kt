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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import coil.compose.AsyncImage
import com.example.healthjournal.R
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.UnitConverter
import com.example.healthjournal.data.local.UnitSettings
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.domain.HashtagParser
import com.example.healthjournal.domain.ValidateVitals
import com.example.healthjournal.domain.VitalField
import com.example.healthjournal.domain.VitalsInput
import com.example.healthjournal.util.HtmlEntities
import com.example.healthjournal.ui.components.EnrichmentPanel
import com.example.healthjournal.ui.components.RichTextToolbar
import com.example.healthjournal.ui.components.TagSelectionRow
import com.example.healthjournal.ui.components.VitalsFieldsEditor
import com.example.healthjournal.ui.components.formatSleepHours
import com.example.healthjournal.ui.components.hasAnyVitals
import com.example.healthjournal.ui.components.toVitals
import com.example.healthjournal.ui.components.unitFor
import com.example.healthjournal.viewmodel.HealthSyncResult
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
    var entryTags by remember { mutableStateOf(setOf<String>()) }
    
    // Health Metrics State
    val unitSystem = UnitSettings.read(context)
    var vitalsInput by remember { mutableStateOf(VitalsInput()) }
    val vitalsErrors = ValidateVitals.validate(vitalsInput, unitSystem)
    var sleepHours by remember { mutableStateOf<Float?>(null) }
    var isHealthSyncing by remember { mutableStateOf(false) }

    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var expandedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkText by remember { mutableStateOf("") }
    var linkUrl by remember { mutableStateOf("") }

    // Health Connect opt-in gate: hidden Sync Health action means no
    // availability check, permission request, or fetch can be triggered.
    val healthSyncEnabled by viewModel.isHealthSyncEnabled.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.refreshHealthSyncSetting()
    }

    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text(stringResource(R.string.entry_link_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = linkText,
                        onValueChange = { linkText = it },
                        label = { Text(stringResource(R.string.entry_link_text_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = linkUrl,
                        onValueChange = { linkUrl = it },
                        label = { Text(stringResource(R.string.entry_link_url_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (linkUrl.isNotBlank()) {
                        richTextState.addLink(
                            text = linkText.ifBlank { linkUrl },
                            url = linkUrl
                        )
                    }
                    showLinkDialog = false
                    linkText = ""
                    linkUrl = ""
                }) { Text(stringResource(R.string.entry_link_insert)) }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedTimestamp
    )
    val calendar = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE)
    )

    // Synced values replace BP/HR but never clear manually entered glucose,
    // temperature, or SpO2; a missing Health Connect value leaves the draft alone.
    fun applyHealthSyncResult(result: HealthSyncResult) {
        vitalsInput = vitalsInput.copy(
            systolic = result.bpSystolic?.let { UnitConverter.formatDouble(it) } ?: vitalsInput.systolic,
            diastolic = result.bpDiastolic?.let { UnitConverter.formatDouble(it) } ?: vitalsInput.diastolic,
            heartRate = result.heartRate?.toString() ?: vitalsInput.heartRate
        )
        sleepHours = result.sleepHours ?: sleepHours
    }

    // Health Connect Permission Launcher
    val healthPermissionsLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        android.util.Log.d("AddEntryScreen", "Health permissions result: $granted")
        if (!viewModel.isHealthSyncEnabled.value) return@rememberLauncherForActivityResult
        if (granted.containsAll(viewModel.healthPermissions)) {
            scope.launch {
                isHealthSyncing = true
                applyHealthSyncResult(viewModel.syncHealthData(selectedTimestamp))
                isHealthSyncing = false
            }
        } else {
            android.util.Log.w("AddEntryScreen", "Not all health permissions granted. Required: ${viewModel.healthPermissions}, Granted: $granted")
            android.widget.Toast.makeText(context, context.getString(R.string.entry_toast_health_perm), android.widget.Toast.LENGTH_SHORT).show()
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
                vitalsInput = ValidateVitals.formatVitals(entry.toVitals(), unitSystem)
                sleepHours = entry.sleep_hours
                entryTags = entry.tags?.toSet() ?: emptySet()
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
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
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
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (entryId == null) stringResource(R.string.entry_title_add) else stringResource(R.string.entry_title_edit)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back_label))
                    }
                },
                actions = {
                    if (existingEntry?.isArchived == true) {
                        IconButton(onClick = {
                            viewModel.restoreEntry(existingEntry!!.entry_id)
                            existingEntry = existingEntry?.copy(isArchived = false)
                        }) {
                            Icon(Icons.Default.Restore, contentDescription = stringResource(R.string.entry_cd_unarchive))
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
                    onLinkClick = {
                        linkText = richTextState.selection.let { 
                            richTextState.annotatedString.substring(it.start, it.end)
                        }
                        showLinkDialog = true
                    }
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

                if (!isReadOnly) {
                    Text(stringResource(R.string.entry_category), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    TagSelectionRow(
                        selectedTags = entryTags,
                        onTagToggle = { tag ->
                            if (entryTags.contains(tag)) {
                                entryTags = entryTags - tag
                            } else {
                                entryTags = entryTags + tag
                            }
                        },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                if (!isReadOnly) {
                    RichTextEditor(
                        state = richTextState,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                        label = { Text(stringResource(R.string.entry_hint_feeling)) }
                    )
                } else {
                    RichText(
                        state = richTextState,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            
            // Health Metrics Section
            if (isReadOnly) {
                existingEntry
                    ?.takeIf { it.hasAnyVitals() }
                    ?.let { ReadOnlyVitalsSummary(entry = it, unitSystem = unitSystem) }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.entry_health_metrics),
                            style = MaterialTheme.typography.titleSmall
                        )
                        VitalsFieldsEditor(
                            input = vitalsInput,
                            onInputChange = { vitalsInput = it },
                            errors = vitalsErrors,
                            unitSystem = unitSystem
                        )
                        sleepHours?.let { hours ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.entry_metric_sleep),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = stringResource(
                                        R.string.entry_vitals_format_value_unit,
                                        formatSleepHours(hours),
                                        stringResource(R.string.entry_vitals_unit_hours)
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (attachedPhotoUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.entry_photos), style = MaterialTheme.typography.titleSmall)
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
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.entry_cd_remove), modifier = Modifier.size(16.dp))
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
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            if (attachedFiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.entry_attachments), style = MaterialTheme.typography.titleSmall)
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
                                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.entry_cd_remove), modifier = Modifier.size(16.dp))
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
                    showSyncHealth = healthSyncEnabled,
                    onSyncHealthClick = {
                        if (!viewModel.isHealthSyncEnabled.value) return@EnrichmentPanel
                        scope.launch {
                            val availability = viewModel.checkHealthAvailability()
                            if (availability != HealthConnectClient.SDK_AVAILABLE) {
                                android.widget.Toast.makeText(context, context.getString(R.string.entry_toast_health_unavailable), android.widget.Toast.LENGTH_LONG).show()
                                return@launch
                            }

                            if (viewModel.hasHealthPermissions()) {
                                isHealthSyncing = true
                                applyHealthSyncResult(viewModel.syncHealthData(selectedTimestamp))
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
                    val descriptionHtml = HtmlEntities.decodeNonAsciiNamedEntities(richTextState.toHtml())
                    val plainText = richTextState.annotatedString.text
                    if (plainText.isNotBlank()) {
                        val vitals = ValidateVitals.toEntryVitals(vitalsInput, unitSystem)
                        if (vitals != null) {
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
                                        android.widget.Toast.makeText(context, context.getString(R.string.entry_toast_attachment_failed_format, file.name), android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    file.copy(uri = persistentUri ?: "")
                                }
                            }.filter { it.uri.isNotBlank() }

                            // Hashtags parse from plain text: the editor serializes
                            // '#' as '&num;' in HTML, so HTML parsing would miss them.
                            val tagsWithHashtags = entryTags +
                                HashtagParser.extractHashtags(plainText)
                            if (entryId == null) {
                                viewModel.addEntry(
                                    description = descriptionHtml,
                                    timestamp = finalTimestamp,
                                    photoUrls = persistentPhotoUrls,
                                    attachments = persistentAttachments,
                                    bpSystolic = vitals.bpSystolic,
                                    bpDiastolic = vitals.bpDiastolic,
                                    heartRate = vitals.heartRate,
                                    sleepHours = sleepHours,
                                    bloodGlucoseMmol = vitals.bloodGlucoseMmol,
                                    bodyTemperatureC = vitals.bodyTemperatureC,
                                    spo2Percent = vitals.spo2Percent,
                                    tags = tagsWithHashtags
                                )
                            } else {
                                existingEntry?.let {
                                    viewModel.updateEntry(
                                        it.copy(
                                            description = descriptionHtml,
                                            timestamp = finalTimestamp,
                                            photo_urls = persistentPhotoUrls,
                                            attachments = persistentAttachments,
                                            bp_systolic = vitals.bpSystolic,
                                            bp_diastolic = vitals.bpDiastolic,
                                            heart_rate_avg = vitals.heartRate,
                                            sleep_hours = sleepHours,
                                            blood_glucose_mmol = vitals.bloodGlucoseMmol,
                                            body_temperature_c = vitals.bodyTemperatureC,
                                            spo2_percent = vitals.spo2Percent
                                        ),
                                        tags = tagsWithHashtags
                                    )
                                }
                            }
                            onBack()
                        }
                        }
                    }
                },
                enabled = vitalsErrors.isEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (entryId == null) stringResource(R.string.entry_save) else stringResource(R.string.entry_update))
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReadOnlyVitalsSummary(entry: JournalEntry, unitSystem: UnitSystem) {
    val vitals = entry.toVitals()
    val display = ValidateVitals.formatVitals(vitals, unitSystem)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.entry_health_metrics),
                style = MaterialTheme.typography.titleSmall
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (vitals.bpSystolic != null && vitals.bpDiastolic != null) {
                    HealthMetricItem(
                        icon = Icons.Default.MonitorHeart,
                        value = stringResource(
                            R.string.entry_vitals_format_bp,
                            display.systolic,
                            display.diastolic,
                            unitFor(VitalField.SYSTOLIC, unitSystem)
                        ),
                        label = stringResource(R.string.entry_metric_bp)
                    )
                }
                vitals.heartRate?.let { heartRate ->
                    HealthMetricItem(
                        icon = Icons.Default.Favorite,
                        value = stringResource(
                            R.string.entry_vitals_format_value_unit,
                            heartRate.toString(),
                            unitFor(VitalField.HEART_RATE, unitSystem)
                        ),
                        label = stringResource(R.string.entry_metric_hr)
                    )
                }
                vitals.bloodGlucoseMmol?.let {
                    HealthMetricItem(
                        icon = Icons.Default.WaterDrop,
                        value = stringResource(
                            R.string.entry_vitals_format_value_unit,
                            display.glucose,
                            unitFor(VitalField.GLUCOSE, unitSystem)
                        ),
                        label = stringResource(R.string.vitals_field_glucose)
                    )
                }
                vitals.bodyTemperatureC?.let {
                    HealthMetricItem(
                        icon = Icons.Default.Thermostat,
                        value = stringResource(
                            R.string.entry_vitals_format_value_unit,
                            display.temperature,
                            unitFor(VitalField.TEMPERATURE, unitSystem)
                        ),
                        label = stringResource(R.string.vitals_field_temperature)
                    )
                }
                vitals.spo2Percent?.let { spo2 ->
                    HealthMetricItem(
                        icon = Icons.Default.Air,
                        value = stringResource(
                            R.string.entry_vitals_format_value_unit,
                            spo2.toString(),
                            unitFor(VitalField.SPO2, unitSystem)
                        ),
                        label = stringResource(R.string.vitals_field_spo2)
                    )
                }
                entry.sleep_hours?.let { hours ->
                    HealthMetricItem(
                        icon = Icons.Default.Bedtime,
                        value = stringResource(
                            R.string.entry_vitals_format_value_unit,
                            formatSleepHours(hours),
                            stringResource(R.string.entry_vitals_unit_hours)
                        ),
                        label = stringResource(R.string.entry_metric_sleep)
                    )
                }
            }
        }
    }
}
