package com.example.healthjournal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.healthjournal.data.local.JournalEntry
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun JournalEntryItem(
    entry: JournalEntry, 
    onClick: () -> Unit, 
    onPhotoClick: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                            text = "(Edited)",
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
                            text = if (isExpanded) "Show Less" else "Show More",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
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
                            text = "${entry.attachments?.size ?: 0} attachment(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                if (entry.bp_systolic != null || entry.heart_rate_avg != null || entry.sleep_hours != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (entry.bp_systolic != null && entry.bp_diastolic != null) {
                            CompactMetricItem(
                                icon = Icons.Default.MonitorHeart, 
                                value = "${entry.bp_systolic?.toInt()}/${entry.bp_diastolic?.toInt()}"
                            )
                        }
                        entry.heart_rate_avg?.let {
                            CompactMetricItem(icon = Icons.Default.Favorite, value = "$it")
                        }
                        entry.sleep_hours?.let {
                            CompactMetricItem(icon = Icons.Default.Bedtime, value = "%.1fh".format(it))
                        }
                    }
                }
            }
            
            if (entry.isSynced == true) {
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

@Composable
fun CompactMetricItem(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.width(2.dp))
        Text(text = value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}
