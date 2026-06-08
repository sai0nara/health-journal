package com.example.healthjournal.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    placeholder: String = "Search..."
) {
    SearchBar(
        query = query,
        onQueryChange = onQueryChanged,
        onSearch = { },
        active = false,
        onActiveChange = { },
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) { }
}
