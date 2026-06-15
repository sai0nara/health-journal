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

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.richeditor.model.RichTextState

@Composable
fun RichTextToolbar(
    state: RichTextState,
    modifier: Modifier = Modifier,
    onAttachClick: () -> Unit = {},
    onLinkClick: () -> Unit = {}
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
                icon = Icons.Default.Title,
                isActive = false, // state.isHeader
                onClick = { /* state.toggleHeader() */ },
                label = "H"
            )

            ToolbarDivider()

            // Inline Style Group
            ToolbarButton(
                icon = Icons.Default.FormatBold,
                isActive = state.currentSpanStyle.fontWeight == FontWeight.Bold,
                onClick = { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }
            )
            ToolbarButton(
                icon = Icons.Default.FormatItalic,
                isActive = state.currentSpanStyle.fontStyle == FontStyle.Italic,
                onClick = { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) }
            )
            ToolbarButton(
                icon = Icons.Default.FormatUnderlined,
                isActive = state.currentSpanStyle.textDecoration == TextDecoration.Underline,
                onClick = { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) }
            )

            ToolbarDivider()

            // List Group
            ToolbarButton(
                icon = Icons.Default.FormatListNumbered,
                isActive = false, // state.isOrderedList
                onClick = { state.toggleOrderedList() }
            )
            ToolbarButton(
                icon = Icons.Default.FormatListBulleted,
                isActive = false, // state.isUnorderedList
                onClick = { state.toggleUnorderedList() }
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
                onClick = { 
                    state.removeParagraphStyle(state.currentParagraphStyle)
                    state.removeSpanStyle(state.currentSpanStyle)
                }
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
