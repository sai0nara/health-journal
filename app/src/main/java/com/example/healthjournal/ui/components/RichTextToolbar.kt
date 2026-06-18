package com.example.healthjournal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextState

@Composable
fun RichTextToolbar(
    state: RichTextState,
    modifier: Modifier = Modifier,
    onLinkClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ToolbarButton(
                icon = Icons.Default.Title,
                isActive = state.isH1 || state.isH2,
                onClick = {
                    if (state.isH1) {
                        state.toggleSpanStyle(SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal))
                    } else {
                        state.toggleSpanStyle(SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold))
                    }
                },
                label = "H",
                testTag = "header_button"
            )

            ToolbarDivider()

            ToolbarButton(
                icon = Icons.Default.FormatBold,
                isActive = state.currentSpanStyle.fontWeight == FontWeight.Bold,
                onClick = { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
                testTag = "bold_button"
            )
            ToolbarButton(
                icon = Icons.Default.FormatItalic,
                isActive = state.currentSpanStyle.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic,
                onClick = { state.toggleSpanStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) },
                testTag = "italic_button"
            )
            ToolbarButton(
                icon = Icons.Default.FormatUnderlined,
                isActive = state.currentSpanStyle.textDecoration == TextDecoration.Underline,
                onClick = { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) },
                testTag = "underline_button"
            )

            ToolbarDivider()

            ToolbarButton(
                icon = Icons.Default.FormatListNumbered,
                isActive = false, 
                onClick = { state.toggleOrderedList() }
            )
            ToolbarButton(
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                isActive = false, 
                onClick = { state.toggleUnorderedList() }
            )

            ToolbarDivider()

            ToolbarButton(
                icon = Icons.Default.Link,
                isActive = false, 
                onClick = onLinkClick
            )

            Spacer(modifier = Modifier.weight(1f))

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
    label: String? = null,
    testTag: String? = null
) {
    val tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (label != null && !isActive) {
           Text(
               text = label,
               style = MaterialTheme.typography.labelLarge,
               color = tint
           )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = tint
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
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

private val RichTextState.isH1 get() = currentSpanStyle.fontSize == 24.sp
private val RichTextState.isH2 get() = currentSpanStyle.fontSize == 20.sp
