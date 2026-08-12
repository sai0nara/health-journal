package com.example.healthjournal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.healthjournal.data.JournalTag

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
                label = { Text(tag.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}
