package com.example.healthjournal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.healthjournal.R
import com.example.healthjournal.ui.components.AboutAppDialog

private data class MainTile(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
)

private val mainTiles = listOf(
    MainTile("history", R.string.main_tile_history, Icons.Default.History),
    MainTile("workout", R.string.main_tile_workout, Icons.Default.FitnessCenter),
    MainTile("measurements", R.string.main_tile_measurements, Icons.Default.Straighten),
    MainTile("presets", R.string.main_tile_presets, Icons.Default.List),
    MainTile("archive", R.string.main_tile_archive, Icons.Default.Archive),
    MainTile("export", R.string.main_tile_export, Icons.Default.Share),
    MainTile("personal_card", R.string.main_tile_personal_card, Icons.Default.Person),
    MainTile("settings", R.string.main_tile_settings, Icons.Default.Settings)
)

/**
 * Dashboard launch screen: a static 2-column tile grid, one tile per
 * section. Stateless by design — no ViewModel, no I/O — so cold launch
 * stays instant and offline. Each tile navigates to its existing
 * destination; system Back from any section returns here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onTileClick: (String) -> Unit) {
    var showAboutDialog by remember { mutableStateOf(false) }
    if (showAboutDialog) {
        AboutAppDialog(onDismiss = { showAboutDialog = false })
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.main_title)) },
                actions = {
                    IconButton(
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.testTag("main_about")
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = stringResource(R.string.history_menu_about)
                        )
                    }
                }
            )
        }
    ) { padding ->
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        items(mainTiles) { tile ->
            val label = stringResource(tile.labelRes)
            Card(
                modifier = Modifier
                    .testTag("main_tile_${tile.route}")
                    .semantics { contentDescription = label }
                    .clickable(role = Role.Button, onClickLabel = label) {
                        onTileClick(tile.route)
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(tile.icon, contentDescription = null)
                    Text(label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
    }
}
