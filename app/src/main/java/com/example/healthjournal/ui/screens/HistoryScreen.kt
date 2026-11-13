package com.example.healthjournal.ui.screens

import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.UnitSettings
import com.example.healthjournal.domain.HistoryDateFilter
import com.example.healthjournal.ui.components.AboutAppDialog
import com.example.healthjournal.R
import com.example.healthjournal.domain.GestureAxis
import com.example.healthjournal.domain.SwipeArchivePolicy
import com.example.healthjournal.domain.SwipeAxisLockTracker
import com.example.healthjournal.ui.components.JournalEntryItem
import com.example.healthjournal.ui.components.SharedSearchBar
import com.example.healthjournal.ui.components.TagSelectionRow
import com.example.healthjournal.ui.components.VitalsEntrySheet
import com.example.healthjournal.util.HtmlParser
import com.example.healthjournal.viewmodel.IJournalViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: IJournalViewModel,
    onAddEntryClick: () -> Unit,
    onEntryClick: (String) -> Unit,
    onBack: () -> Unit = {},
    profileSwitcher: @Composable (enabled: Boolean, onSwitch: (String) -> Unit) -> Unit =
        { _, _ -> },
    onSwitchProfile: (String) -> Unit = {}
) {
    val entries by viewModel.allEntries.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val isAscending by viewModel.isAscending.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val tagsByEntry by viewModel.tagsByEntry.collectAsState()
    var expandedImageUri by remember { mutableStateOf<String?>(null) }
    var vitalsEntry by remember { mutableStateOf<JournalEntry?>(null) }
    val unitSystem = UnitSettings.read(LocalContext.current)
    val isUserSignedIn by viewModel.isUserSignedIn.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val entryArchivedMessage = stringResource(R.string.history_entry_archived)
    val undoActionLabel = stringResource(R.string.history_undo)

    val accessibilityManager = remember(context) {
        context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    }
    val snackbarDuration = if (accessibilityManager?.isEnabled == true && accessibilityManager.isTouchExplorationEnabled) {
        SnackbarDuration.Indefinite
    } else {
        SnackbarDuration.Long
    }

    val archiveEntryWithUndo: (String) -> Unit = { entryId ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.archiveEntry(entryId)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = entryArchivedMessage,
                actionLabel = undoActionLabel,
                duration = snackbarDuration
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreEntry(entryId)
            }
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }

    // Stop the pull-to-refresh indicator as soon as the user-initiated sync is
    // no longer in flight (finished, failed, or blocked on re-authorization).
    val isManualSyncActive by viewModel.isManualSyncActive.collectAsState()
    LaunchedEffect(isManualSyncActive) {
        if (!isManualSyncActive) {
            isRefreshing = false
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }
    var isCalendarExpanded by remember { mutableStateOf(false) }

    val dateFilter by viewModel.dateFilter.collectAsState()
    val visibleMonth by viewModel.visibleMonth.collectAsState()
    val daysWithEntries by viewModel.daysWithEntriesInVisibleMonth.collectAsState()

    if (showAboutDialog) {
        AboutAppDialog(onDismiss = { showAboutDialog = false })
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back_label)
                        )
                    }
                },
                title = {
                    Text(
                        stringResource(R.string.history_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    profileSwitcher(true, onSwitchProfile)
                    IconButton(
                        onClick = { isCalendarExpanded = !isCalendarExpanded },
                        modifier = Modifier.testTag("history_calendar_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = stringResource(R.string.history_calendar_toggle_cd),
                            tint = if (isCalendarExpanded || dateFilter !is HistoryDateFilter.All) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    IconButton(
                        onClick = { showOverflow = true },
                        modifier = Modifier.testTag("overflow_menu")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.history_cd_more))
                    }
                    DropdownMenu(
                        expanded = showOverflow,
                        onDismissRequest = { showOverflow = false }
                    ) {
                        if (isUserSignedIn) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.history_menu_sync)) },
                                onClick = { showOverflow = false; viewModel.syncNow() },
                                leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null) }
                            )
                        }
                        DropdownMenuItem(
                                text = { Text(stringResource(R.string.history_menu_sort)) },
                            onClick = { showOverflow = false; viewModel.setSortOrder(!isAscending) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) }
                        )
                        DropdownMenuItem(
                                text = { Text(stringResource(R.string.history_menu_about)) },
                            onClick = { showOverflow = false; showAboutDialog = true },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEntryClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.history_cd_add_entry))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search Bar
            SharedSearchBar(
                query = searchQuery,
                onQueryChanged = { viewModel.setSearchQuery(it) },
                placeholder = stringResource(R.string.history_search_placeholder)
            )

            // Collapsible Calendar Panel
            AnimatedVisibility(
                visible = isCalendarExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                com.example.healthjournal.ui.components.HistoryCalendarPanel(
                    visibleMonth = visibleMonth,
                    dateFilter = dateFilter,
                    daysWithEntries = daysWithEntries,
                    onPreviousMonth = { viewModel.setVisibleMonth(visibleMonth.minusMonths(1)) },
                    onNextMonth = { viewModel.setVisibleMonth(visibleMonth.plusMonths(1)) },
                    onDateSelected = { date ->
                        when (val current = dateFilter) {
                            is HistoryDateFilter.SingleDay -> {
                                if (current.date == date) {
                                    viewModel.clearDateFilter()
                                } else {
                                    viewModel.setDateFilter(
                                        HistoryDateFilter.periodOf(current.date, date)
                                    )
                                }
                            }
                            is HistoryDateFilter.All -> {
                                viewModel.setDateFilter(HistoryDateFilter.SingleDay(date))
                            }
                            is HistoryDateFilter.Period -> {
                                viewModel.setDateFilter(HistoryDateFilter.SingleDay(date))
                            }
                        }
                    },
                    onPresetSelected = { preset ->
                        viewModel.setDateFilter(HistoryDateFilter.preset(preset, java.time.LocalDate.now()))
                    },
                    onClearDateFilter = { viewModel.clearDateFilter() },
                    onApply = { isCalendarExpanded = false },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Standalone Active Date Filter Chip (when calendar is collapsed but filter is active)
            if (!isCalendarExpanded && dateFilter !is HistoryDateFilter.All) {
                com.example.healthjournal.ui.components.ActiveDateFilterChip(
                    dateFilter = dateFilter,
                    onClear = { viewModel.clearDateFilter() },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .align(Alignment.Start)
                )
            }

            TagSelectionRow(
                selectedTags = selectedTags,
                onTagToggle = { tag -> viewModel.toggleTag(tag) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Sync Status
            if (isUserSignedIn) {
                syncStatus?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    viewModel.syncNow()
                },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                if (entries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (dateFilter !is HistoryDateFilter.All) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(24.dp).testTag("history_date_empty_state")
                            ) {
                                Text(
                                    text = if (dateFilter is HistoryDateFilter.SingleDay) {
                                        stringResource(R.string.history_empty_date_title)
                                    } else {
                                        stringResource(R.string.history_empty_period_title)
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Button(
                                    onClick = { viewModel.clearDateFilter() },
                                    modifier = Modifier.testTag("clear_date_filter_cta")
                                ) {
                                    Text(stringResource(R.string.history_empty_date_cta))
                                }
                            }
                        } else {
                            Text(
                                activeProfile?.let {
                                    stringResource(R.string.history_empty_profile, it.name)
                                } ?: stringResource(R.string.history_empty)
                            )
                        }
                    }
                } else {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(entries, key = { it.entry_id }) { entry ->
                            SwipeToArchiveWrapper(
                                isListScrollInProgress = listState.isScrollInProgress,
                                onArchive = { archiveEntryWithUndo(entry.entry_id) }
                            ) {
                                JournalEntryItem(
                                    entry = entry,
                                    onClick = { onEntryClick(entry.entry_id) },
                                    onPhotoClick = { expandedImageUri = it },
                                    onArchiveClick = { archiveEntryWithUndo(entry.entry_id) },
                                    tags = tagsByEntry[entry.entry_id] ?: emptyList(),
                                    onTagClick = { tag -> viewModel.toggleTag(tag) },
                                    unitSystem = unitSystem,
                                    onVitalsClick = { vitalsEntry = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    vitalsEntry?.let { entry ->
        VitalsEntrySheet(
            entry = entry,
            unitSystem = unitSystem,
            onDismiss = { vitalsEntry = null },
            onSave = { vitals ->
                viewModel.updateVitals(entry.entry_id, vitals)
                vitalsEntry = null
            }
        )
    }

    if (expandedImageUri != null) {
        Dialog(
            onDismissRequest = { expandedImageUri = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim)
                    .clickable { expandedImageUri = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = expandedImageUri,
                    contentDescription = stringResource(R.string.history_cd_expanded_image),
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun SwipeToArchiveWrapper(
    isListScrollInProgress: Boolean = false,
    onArchive: () -> Unit,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val viewConfig = LocalViewConfiguration.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val coroutineScope = rememberCoroutineScope()
    val currentIsListScrollInProgress by rememberUpdatedState(isListScrollInProgress)

    var cardWidthPx by remember { mutableFloatStateOf(0f) }
    val offsetX = remember { Animatable(0f) }
    val policy = remember(cardWidthPx) { SwipeArchivePolicy(cardWidthPx = cardWidthPx) }
    val isArmed = remember(offsetX.value, cardWidthPx, isRtl) {
        policy.isArmed(offsetX.value, isRtl)
    }

    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isArmed) 1f else 0.7f,
        label = "swipe_bg_alpha"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isArmed) 1.2f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swipe_icon_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { cardWidthPx = it.width.toFloat() }
    ) {
        // Background reveal (only composed while card is displaced from rest)
        if (offsetX.value != 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = backgroundAlpha))
                    .padding(horizontal = 20.dp),
                contentAlignment = if (isRtl) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.scale(iconScale)
                ) {
                    if (isRtl) {
                        Icon(
                            Icons.Default.Archive,
                            contentDescription = stringResource(R.string.history_cd_archive),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = stringResource(R.string.history_archive_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = if (isArmed) FontWeight.Bold else FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.history_archive_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = if (isArmed) FontWeight.Bold else FontWeight.Medium
                        )
                        Icon(
                            Icons.Default.Archive,
                            contentDescription = stringResource(R.string.history_cd_archive),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Foreground card with custom pointer gestures
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(cardWidthPx, isRtl) {
                    val tracker = SwipeAxisLockTracker(touchSlopPx = viewConfig.touchSlop)
                    val velocityTracker = VelocityTracker()

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        tracker.onDown(currentIsListScrollInProgress)
                        velocityTracker.resetTracking()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)

                        var cumulativeDx = 0f
                        var cumulativeDy = 0f

                        while (true) {
                            val event = awaitPointerEvent()
                            val dragChange = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (!dragChange.pressed) {
                                // Pointer lifted - handle release
                                val velocity = velocityTracker.calculateVelocity().x
                                val releaseResult = policy.onRelease(
                                    dragOffsetPx = offsetX.value,
                                    velocityPxPerSec = velocity,
                                    isRtl = isRtl
                                )

                                if (releaseResult.shouldCommit) {
                                    coroutineScope.launch {
                                        offsetX.animateTo(
                                            targetValue = releaseResult.targetOffsetPx,
                                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                        )
                                        onArchive()
                                        offsetX.snapTo(0f)
                                    }
                                } else {
                                    coroutineScope.launch {
                                        offsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                        )
                                    }
                                }
                                tracker.reset()
                                dragChange.consume()
                                break
                            }

                            val positionChange = dragChange.positionChange()
                            cumulativeDx += positionChange.x
                            cumulativeDy += positionChange.y
                            velocityTracker.addPosition(dragChange.uptimeMillis, dragChange.position)

                            val axis = tracker.update(cumulativeDx, cumulativeDy)
                            if (axis == GestureAxis.HORIZONTAL) {
                                dragChange.consume()
                                val prevOffset = offsetX.value
                                val newOffset = policy.clampOffset(prevOffset + positionChange.x, isRtl)

                                if (policy.hasCrossedThreshold(prevOffset, newOffset, isRtl)) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }

                                coroutineScope.launch {
                                    offsetX.snapTo(newOffset)
                                }
                            } else if (axis == GestureAxis.VERTICAL || axis == GestureAxis.BLOCKED) {
                                // Locked vertically or blocked - let parent scroll handle it
                            }
                        }
                    }
                }
        ) {
            content()
        }
    }
}
