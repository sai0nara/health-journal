package com.example.healthjournal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.healthjournal.R
import com.example.healthjournal.ui.theme.contentColorForAccent

/** Circular profile initials with an optional per-profile accent color. */
@Composable
fun ProfileAvatar(
    name: String,
    accentColor: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    val background = accentColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primaryContainer
    val foreground = if (accentColor == null) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        contentColorForAccent(background)
    }
    val description = stringResource(R.string.profile_avatar_cd, name.ifBlank { "?" })

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .testTag("profile_avatar")
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = profileInitials(name),
            color = foreground,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/** First letters of the first two words, uppercased; blank names use "?". */
internal fun profileInitials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return "?"
    return words.take(2).joinToString("") { it.first().uppercase() }
}
