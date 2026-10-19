package com.example.healthjournal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import com.example.healthjournal.R
import com.example.healthjournal.data.JournalTag

/**
 * Resource for a stored tag value: journal-chip enum names ("HEALTH") and
 * capitalized system auto-tags ("Health") resolve case-insensitively through
 * the shared taxonomy; free-form user tags have no resource.
 */
@StringRes
fun tagLabelRes(tag: String): Int? =
    JournalTag.entries.firstOrNull { it.name.equals(tag, ignoreCase = true) }?.labelRes

/**
 * Display name for a stored tag: known tags resolve through resources
 * (Russian parity); user tags render verbatim, capitalized.
 */
@Composable
fun tagDisplayName(tag: String): String =
    tagLabelRes(tag)?.let { stringResource(it) }
        ?: tag.lowercase().replaceFirstChar { it.uppercase() }

@Composable
fun TagSelectionRow(
    selectedTags: Set<String>,
    onTagToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        JournalTag.entries.forEach { tag ->
            FilterChip(
                selected = selectedTags.contains(tag.name),
                onClick = { onTagToggle(tag.name) },
                label = { Text(tagDisplayName(tag.name)) }
            )
        }
    }
}
