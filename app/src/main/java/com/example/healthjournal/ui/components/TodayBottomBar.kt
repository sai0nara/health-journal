package com.example.healthjournal.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.healthjournal.R

enum class TopLevelTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String
) {
    TODAY("main", R.string.nav_today, Icons.Default.Home, "bottom_nav_today"),
    HISTORY("history", R.string.nav_history, Icons.Default.History, "bottom_nav_history"),
    TRENDS("vitals", R.string.nav_trends, Icons.Default.BarChart, "bottom_nav_trends"),
    MORE("more", R.string.nav_more, Icons.Default.MoreHoriz, "bottom_nav_more")
}

/**
 * Persistent 4-tab bottom navigation bar for thumb-zone switching across
 * Today, History, Trends, and More destinations.
 */
@Composable
fun TodayBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier.testTag("today_bottom_bar")) {
        TopLevelTab.entries.forEach { tab ->
            val selected = currentRoute == tab.route
            val label = stringResource(tab.labelRes)
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        onNavigate(tab.route)
                    }
                },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.testTag(tab.testTag)
            )
        }
    }
}
