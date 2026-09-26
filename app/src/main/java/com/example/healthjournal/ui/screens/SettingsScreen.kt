package com.example.healthjournal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.healthjournal.R
import com.example.healthjournal.data.local.UnitSettings
import com.example.healthjournal.data.local.UnitSystem

/**
 * General app settings: exposes the app-wide unit system (metric/imperial)
 * persisted to [UnitSettings] — the same choice the Personal Card and the
 * workout quick pad share, surfaced here as the primary point of control.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var unitSystem by remember { mutableStateOf(UnitSettings.read(context)) }
    var unitSystemExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back_label))
                    }
                },
                title = { Text(stringResource(R.string.settings_title)) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_section_general),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            ExposedDropdownMenuBox(
                expanded = unitSystemExpanded,
                onExpandedChange = { unitSystemExpanded = it }
            ) {
                OutlinedTextField(
                    value = stringResource(
                        if (unitSystem == UnitSystem.METRIC) R.string.unit_system_metric
                        else R.string.unit_system_imperial
                    ),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_unit_system)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitSystemExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .testTag("settings_unit_system")
                )
                ExposedDropdownMenu(
                    expanded = unitSystemExpanded,
                    onDismissRequest = { unitSystemExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.unit_system_metric)) },
                        onClick = {
                            unitSystem = UnitSystem.METRIC
                            UnitSettings.write(context, UnitSystem.METRIC)
                            unitSystemExpanded = false
                        },
                        modifier = Modifier.testTag("settings_unit_system_metric")
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.unit_system_imperial)) },
                        onClick = {
                            unitSystem = UnitSystem.IMPERIAL
                            UnitSettings.write(context, UnitSystem.IMPERIAL)
                            unitSystemExpanded = false
                        },
                        modifier = Modifier.testTag("settings_unit_system_imperial")
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_units_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}