package com.example.healthjournal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.healthjournal.R
import com.example.healthjournal.ui.components.EnrichmentPanel
import com.example.healthjournal.ui.components.RichTextToolbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentPreviewScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preview_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back_label))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                text = stringResource(R.string.preview_enrichment),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            EnrichmentPanel(
                onCameraClick = { /* Preview Click */ },
                onGalleryClick = { /* Preview Click */ },
                onAttachFileClick = { /* Preview Click */ },
                onSyncHealthClick = { /* Preview Click */ }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.preview_toolbar),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            val richTextState = com.mohamedrejeb.richeditor.model.rememberRichTextState()
            RichTextToolbar(
                state = richTextState
            )
        }
    }
}
