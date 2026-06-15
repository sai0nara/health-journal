package com.example.healthjournal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun RichTextToolbar(
    modifier: Modifier = Modifier,
    activeBold: Boolean = false,
    activeItalic: Boolean = false,
    activeUnderline: Boolean = false,
    activeHeader: Int = 0, // 0: None, 1: H1, 2: H2
    onBoldClick: () -> Unit = {},
    onItalicClick: () -> Unit = {},
    onUnderlineClick: () -> Unit = {},
    onHeaderClick: () -> Unit = {},
    onListNumberedClick: () -> Unit = {},
    onListBulletClick: () -> Unit = {},
    onAttachClick: () -> Unit = {},
    onLinkClick: () -> Unit = {},
    onClearClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        color = Color(0xFF1A1C1E), // Dark charcoal
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header Group
            ToolbarButton(
                icon = if (activeHeader == 1) Icons.Default.FormatSize else Icons.Default.Title,
                isActive = activeHeader > 0,
                onClick = onHeaderClick,
                label = if (activeHeader > 0) "H$activeHeader" else "H"
            )

            ToolbarDivider()

            // Inline Style Group
            ToolbarButton(
                icon = Icons.Default.FormatBold,
                isActive = activeBold,
                onClick = onBoldClick
            )
            ToolbarButton(
                icon = Icons.Default.FormatItalic,
                isActive = activeItalic,
                onClick = onItalicClick
            )
            ToolbarButton(
                icon = Icons.Default.FormatUnderlined,
                isActive = activeUnderline,
                onClick = onUnderlineClick
            )

            ToolbarDivider()

            // List Group
            ToolbarButton(
                icon = Icons.Default.FormatListNumbered,
                isActive = false,
                onClick = onListNumberedClick
            )
            ToolbarButton(
                icon = Icons.Default.FormatListBulleted,
                isActive = false,
                onClick = onListBulletClick
            )

            ToolbarDivider()

            // Media & Links Group
            ToolbarButton(
                icon = Icons.Default.AttachFile,
                isActive = false,
                onClick = onAttachClick
            )
            ToolbarButton(
                icon = Icons.Default.Link,
                isActive = false,
                onClick = onLinkClick
            )

            Spacer(modifier = Modifier.weight(1f))

            // Clear Group
            ToolbarButton(
                icon = Icons.Default.FormatClear,
                isActive = false,
                onClick = onClearClick
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    label: String? = null
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isActive) Color.White.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (label != null && !isActive) {
           Text(
               text = label,
               style = MaterialTheme.typography.labelLarge,
               color = if (isActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
           )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ToolbarDivider() {
    VerticalDivider(
        modifier = Modifier
            .height(24.dp)
            .padding(horizontal = 4.dp),
        thickness = 1.dp,
        color = Color.White.copy(alpha = 0.1f)
    )
}

// Extension to make Box clickable without material ripple constraints if needed
@Composable
private fun Modifier.clickable(onClick: () -> Unit): Modifier = this.then(
    androidx.compose.foundation.clickable(
        interactionSource = androidx.compose.foundation.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = androidx.compose.material.ripple.rememberRipple(bounded = true),
        onClick = onClick
    )
)
