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
import com.example.healthjournal.R
import com.example.healthjournal.data.JournalTag
import com.example.healthjournal.domain.EntryKindTag

/**
 * Display name for a stored tag: system auto-tags resolve through resources
 * (Russian parity); user tags render verbatim.
 */
@Composable
fun tagDisplayName(tag: String): String = when (tag) {
    EntryKindTag.FITNESS -> stringResource(R.string.auto_tag_fitness)
    EntryKindTag.HEALTH -> stringResource(R.string.auto_tag_health)
    EntryKindTag.MEDICATION -> stringResource(R.string.auto_tag_medication)
    else -> tag.lowercase().replaceFirstChar { it.uppercase() }
}

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
