package com.example.healthjournal.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.healthjournal.R
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.domain.ValidateVitals
import com.example.healthjournal.domain.VitalField
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalEntryItem(
    entry: JournalEntry,
    onClick: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onLongClick: (() -> Unit)? = null,
    tags: List<String> = emptyList(),
    onTagClick: (String) -> Unit = {},
    unitSystem: UnitSystem = UnitSystem.METRIC,
    onVitalsClick: (JournalEntry) -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(entry.timestamp)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light
                    )
                    if (entry.lastModified > entry.timestamp + 60000) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.entry_edited),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraLight,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    val richTextState = rememberRichTextState()
                    LaunchedEffect(entry.description) {
                        richTextState.setHtml(entry.description)
                    }
                    RichText(
                        state = richTextState,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 3
                    )
                }

                // Show More / Show Less interaction point
                // Note: In a real app, we'd detect if truncation actually happened.
                // For this track, we'll provide the interaction point as requested.
                if (entry.description.length > 100) { // Simple heuristic
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(32.dp).testTag("show_more_button")
                    ) {
                        Text(
                            text = if (isExpanded) stringResource(R.string.entry_show_less) else stringResource(R.string.entry_show_more),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                if (tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("entry_tag_row")
                    ) {
                        tags.forEach { tag ->
                            FilterChip(
                                selected = false,
                                onClick = { onTagClick(tag) },
                                label = {
                                    Text(
                                        "#${tagDisplayName(tag)}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.testTag("entry_tag_$tag")
                            )
                        }
                    }
                }

                if (!entry.photo_urls.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth().testTag("photo_thumbnails")
                    ) {
                        items(entry.photo_urls!!.size) { index ->
                            val photoUrl = entry.photo_urls!![index]
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .clickable { onPhotoClick(photoUrl) },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                if (entry.attachments?.isNotEmpty() == true) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Description, 
                            contentDescription = null, 
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = pluralStringResource(R.plurals.entry_attachments_format, entry.attachments?.size ?: 0, entry.attachments?.size ?: 0),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                if (entry.hasAnyVitals()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    VitalsChipRow(entry = entry, unitSystem = unitSystem)
                }

                if (entry.isArchived != true) {
                    TextButton(
                        onClick = { onVitalsClick(entry) },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("entry_vitals_action")
                    ) {
                        Icon(
                            imageVector = if (entry.hasEditableVitals()) {
                                Icons.Default.Edit
                            } else {
                                Icons.Default.Add
                            },
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                if (entry.hasEditableVitals()) {
                                    R.string.entry_vitals_action_edit
                                } else {
                                    R.string.entry_vitals_action_add
                                }
                            ),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            
            if (entry.isSynced == true) {
                Icon(
                    Icons.Default.CloudDone,
                    contentDescription = stringResource(R.string.common_cd_synced),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.Default.CloudSync,
                    contentDescription = stringResource(R.string.common_cd_local),
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VitalsChipRow(entry: JournalEntry, unitSystem: UnitSystem) {
    val display = ValidateVitals.formatVitals(entry.toVitals(), unitSystem)

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("entry_vitals_row"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (entry.bp_systolic != null && entry.bp_diastolic != null) {
            val text = stringResource(
                R.string.entry_vitals_format_bp,
                display.systolic,
                display.diastolic,
                stringResource(R.string.entry_vitals_unit_bp)
            )
            VitalsChip(
                tag = "entry_vitals_bp",
                description = stringResource(R.string.entry_vitals_cd_bp, text),
                text = text
            )
        }
        entry.heart_rate_avg?.let { heartRate ->
            val text = stringResource(
                R.string.entry_vitals_format_value_unit,
                heartRate.toString(),
                stringResource(R.string.entry_vitals_unit_bpm)
            )
            VitalsChip(
                tag = "entry_vitals_hr",
                description = stringResource(R.string.entry_vitals_cd_hr, text),
                text = text
            )
        }
        entry.blood_glucose_mmol?.let {
            val text = stringResource(
                R.string.entry_vitals_format_value_unit,
                display.glucose,
                unitFor(VitalField.GLUCOSE, unitSystem)
            )
            VitalsChip(
                tag = "entry_vitals_glucose",
                description = stringResource(R.string.entry_vitals_cd_glucose, text),
                text = text
            )
        }
        entry.body_temperature_c?.let {
            val text = stringResource(
                R.string.entry_vitals_format_value_unit,
                display.temperature,
                unitFor(VitalField.TEMPERATURE, unitSystem)
            )
            VitalsChip(
                tag = "entry_vitals_temp",
                description = stringResource(R.string.entry_vitals_cd_temperature, text),
                text = text
            )
        }
        entry.spo2_percent?.let { spo2 ->
            val text = stringResource(
                R.string.entry_vitals_format_value_unit,
                spo2.toString(),
                stringResource(R.string.entry_vitals_unit_percent)
            )
            VitalsChip(
                tag = "entry_vitals_spo2",
                description = stringResource(R.string.entry_vitals_cd_spo2, text),
                text = text
            )
        }
        entry.sleep_hours?.let { hours ->
            val text = stringResource(
                R.string.entry_vitals_format_value_unit,
                formatSleepHours(hours),
                stringResource(R.string.entry_vitals_unit_hours)
            )
            VitalsChip(
                tag = "entry_vitals_sleep",
                description = stringResource(R.string.entry_vitals_cd_sleep, text),
                text = text
            )
        }
    }
}

@Composable
private fun VitalsChip(tag: String, description: String, text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .testTag(tag)
            .semantics(mergeDescendants = true) {
                contentDescription = description
            }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
