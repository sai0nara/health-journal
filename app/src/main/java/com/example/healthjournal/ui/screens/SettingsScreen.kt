package com.example.healthjournal.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.healthjournal.R
import com.example.healthjournal.data.local.HealthSyncSettings
import com.example.healthjournal.data.local.UnitSettings
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.viewmodel.IJournalViewModel

/**
 * General app settings: exposes the app-wide unit system (metric/imperial)
 * persisted to [UnitSettings] — the same choice the Personal Card and the
 * workout quick pad share, surfaced here as the primary point of control —
 * alongside the Google account section (sign in/out for Drive sync).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: IJournalViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var unitSystem by remember { mutableStateOf(UnitSettings.read(context)) }
    var unitSystemExpanded by remember { mutableStateOf(false) }
    var healthSyncEnabled by remember { mutableStateOf(HealthSyncSettings.read(context)) }
    val isUserSignedIn by viewModel.isUserSignedIn.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    val authorizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.syncNow()
        }
    }

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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_section_health_sync),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val healthSyncLabel = stringResource(R.string.settings_health_sync_label)
                Text(
                    text = healthSyncLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = healthSyncEnabled,
                    onCheckedChange = {
                        healthSyncEnabled = it
                        HealthSyncSettings.write(context, it)
                    },
                    modifier = Modifier
                        .testTag("settings_health_sync_toggle")
                        .semantics { contentDescription = healthSyncLabel }
                )
            }
            Text(
                text = stringResource(R.string.settings_health_sync_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_section_account),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            if (isUserSignedIn) {
                userEmail?.let { email ->
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag("settings_account_email")
                    )
                }
                syncStatus?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Button(
                    onClick = { viewModel.signOut() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_sign_out")
                ) {
                    Text(stringResource(R.string.settings_sign_out))
                }
            } else {
                Button(
                    onClick = {
                        viewModel.signIn(context) { pendingIntent ->
                            authorizationLauncher.launch(
                                IntentSenderRequest.Builder(pendingIntent).build()
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_sign_in")
                ) {
                    Text(stringResource(R.string.settings_sign_in))
                }
            }
        }
    }
}
