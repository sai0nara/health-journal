package com.example.healthjournal.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.healthjournal.R
import com.example.healthjournal.data.local.ProfileEntity

/**
 * Compact top-bar profile chip. The dropdown lists every profile, marks the
 * active one, and always offers Manage Profiles.
 */
@Composable
fun ProfileSwitcher(
    profiles: List<ProfileEntity>,
    activeProfileId: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onProfileSelected: (String) -> Unit,
    onManageProfiles: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val active = profiles.firstOrNull { it.id == activeProfileId } ?: profiles.firstOrNull()
    val description = stringResource(R.string.profile_switcher_cd)

    Surface(
        onClick = { expanded = true },
        modifier = modifier
            .testTag("profile_switcher")
            .semantics { contentDescription = description },
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .height(40.dp)
                .padding(start = 4.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(
                name = active?.name.orEmpty(),
                accentColor = active?.accentColor,
                size = 28.dp
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = active?.name.orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        profiles.forEach { profile ->
            val isActive = profile.id == activeProfileId
            DropdownMenuItem(
                text = {
                    Text(
                        text = profile.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    ProfileAvatar(
                        name = profile.name,
                        accentColor = profile.accentColor,
                        size = 24.dp
                    )
                },
                trailingIcon = if (isActive) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.profile_active),
                            modifier = Modifier.testTag("profile_switcher_check_${profile.id}")
                        )
                    }
                } else {
                    null
                },
                onClick = {
                    expanded = false
                    onProfileSelected(profile.id)
                },
                modifier = Modifier.testTag("profile_switcher_item_${profile.id}")
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.profile_switcher_manage)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null
                )
            },
            onClick = {
                expanded = false
                onManageProfiles()
            },
            modifier = Modifier.testTag("profile_switcher_manage")
        )
    }
}
