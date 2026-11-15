package com.example.healthjournal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.healthjournal.R
import com.example.healthjournal.ui.components.AboutAppDialog

/**
 * Secondary tools hub reached via the `More` bottom navigation tab.
 * Houses Settings, Export Data, Presets management, and About App.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onOpenSettings: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenPresets: () -> Unit,
    profileSwitcher: @Composable (enabled: Boolean, onSwitch: (String) -> Unit) -> Unit = { _, _ -> },
    onSwitchProfile: (String) -> Unit = {},
    bottomBar: @Composable () -> Unit = {}
) {
    var showAboutDialog by remember { mutableStateOf(false) }
    if (showAboutDialog) {
        AboutAppDialog(onDismiss = { showAboutDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.today_more_title)) },
                actions = {
                    profileSwitcher(true, onSwitchProfile)
                }
            )
        },
        bottomBar = bottomBar
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MoreDestinationRow(
                title = stringResource(R.string.main_tile_settings),
                icon = Icons.Default.Settings,
                testTag = "more_item_settings",
                onClick = onOpenSettings
            )
            HorizontalDivider()
            MoreDestinationRow(
                title = stringResource(R.string.main_tile_export),
                icon = Icons.Default.Share,
                testTag = "more_item_export",
                onClick = onOpenExport
            )
            HorizontalDivider()
            MoreDestinationRow(
                title = stringResource(R.string.main_tile_presets),
                icon = Icons.Default.FitnessCenter,
                testTag = "more_item_presets",
                onClick = onOpenPresets
            )
            HorizontalDivider()
            MoreDestinationRow(
                title = stringResource(R.string.history_menu_about),
                icon = Icons.Default.Info,
                testTag = "more_item_about",
                onClick = { showAboutDialog = true }
            )
        }
    }
}

@Composable
private fun MoreDestinationRow(
    title: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier
            .testTag(testTag)
            .clickable(onClick = onClick)
    )
}
