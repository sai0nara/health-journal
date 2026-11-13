package com.example.healthjournal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthjournal.R
import com.example.healthjournal.domain.DatePreset
import com.example.healthjournal.domain.HistoryDateFilter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HistoryCalendarPanel(
    visibleMonth: YearMonth,
    dateFilter: HistoryDateFilter,
    daysWithEntries: Set<LocalDate>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onPresetSelected: (DatePreset) -> Unit,
    onClearDateFilter: () -> Unit,
    onApply: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("history_calendar_panel"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quick Presets Row
            DatePresetRow(
                onPresetSelected = onPresetSelected,
                modifier = Modifier.fillMaxWidth()
            )

            // Month navigation header
            CalendarHeader(
                visibleMonth = visibleMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                modifier = Modifier.fillMaxWidth()
            )

            // Days of Week Header (Mon-Sun or Sun-Sat based on locale)
            WeekdayHeaderRow(modifier = Modifier.fillMaxWidth())

            // Month Day Grid
            MonthDayGrid(
                visibleMonth = visibleMonth,
                dateFilter = dateFilter,
                daysWithEntries = daysWithEntries,
                onDateSelected = onDateSelected,
                modifier = Modifier.fillMaxWidth()
            )

            // Active Filter Chip (if filter is not All)
            if (dateFilter !is HistoryDateFilter.All) {
                ActiveDateFilterChip(
                    dateFilter = dateFilter,
                    onClear = onClearDateFilter,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // Apply button to close the calendar panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onApply,
                    modifier = Modifier.testTag("calendar_apply_button")
                ) {
                    Text(text = stringResource(R.string.history_calendar_apply))
                }
            }
        }
    }
}

@Composable
fun DatePresetRow(
    onPresetSelected: (DatePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf(
        DatePreset.TODAY to R.string.history_preset_today,
        DatePreset.YESTERDAY to R.string.history_preset_yesterday,
        DatePreset.LAST_7_DAYS to R.string.history_preset_last_7_days,
        DatePreset.LAST_30_DAYS to R.string.history_preset_last_30_days
    )

    LazyRow(
        modifier = modifier.testTag("history_preset_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(presets) { (preset, labelRes) ->
            SuggestionChip(
                onClick = { onPresetSelected(preset) },
                label = { Text(text = stringResource(id = labelRes), fontSize = 12.sp) },
                modifier = Modifier.testTag("preset_chip_${preset.name}")
            )
        }
    }
}

@Composable
private fun CalendarHeader(
    visibleMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = Locale.getDefault()
    val monthTitle = visibleMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() } +
        " " + visibleMonth.year

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousMonth,
            modifier = Modifier.testTag("calendar_prev_month")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.history_calendar_prev_month_cd)
            )
        }

        Text(
            text = monthTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("calendar_month_title")
        )

        IconButton(
            onClick = onNextMonth,
            modifier = Modifier.testTag("calendar_next_month")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.history_calendar_next_month_cd)
            )
        }
    }
}

@Composable
private fun WeekdayHeaderRow(modifier: Modifier = Modifier) {
    val locale = Locale.getDefault()
    val daysOfWeek = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        daysOfWeek.forEach { dayOfWeek ->
            Text(
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, locale).take(2).uppercase(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthDayGrid(
    visibleMonth: YearMonth,
    dateFilter: HistoryDateFilter,
    daysWithEntries: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstDayOfMonth = visibleMonth.atDay(1)
    val daysInMonth = visibleMonth.lengthOfMonth()
    // Align Monday = 0 .. Sunday = 6
    val leadingEmptyDays = (firstDayOfMonth.dayOfWeek.value - 1) % 7
    val totalCells = leadingEmptyDays + daysInMonth
    val rowCount = (totalCells + 6) / 7

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until rowCount) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayOfMonth = cellIndex - leadingEmptyDays + 1

                    if (dayOfMonth in 1..daysInMonth) {
                        val date = visibleMonth.atDay(dayOfMonth)
                        val isSelected = when (dateFilter) {
                            is HistoryDateFilter.All -> false
                            is HistoryDateFilter.SingleDay -> dateFilter.date == date
                            is HistoryDateFilter.Period -> date in dateFilter.startDate..dateFilter.endDate
                        }
                        val hasEntries = daysWithEntries.contains(date)

                        CalendarDayCell(
                            date = date,
                            isSelected = isSelected,
                            hasEntries = hasEntries,
                            onClick = { onDateSelected(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    isSelected: Boolean,
    hasEntries: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isToday = date == LocalDate.now()
    val locale = Locale.getDefault()
    val monthName = date.month.getDisplayName(TextStyle.FULL, locale)
    val entryStateString = stringResource(
        if (hasEntries) R.string.history_calendar_has_entries else R.string.history_calendar_no_entries
    )
    val cd = stringResource(R.string.history_calendar_day_cd, date.dayOfMonth, monthName, entryStateString)

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                } else Modifier
            )
            .clickable(onClick = onClick)
            .testTag("calendar_day_${date}")
            .semantics { contentDescription = cd },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
            )

            if (hasEntries) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                        .testTag("event_dot_${date}")
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun ActiveDateFilterChip(
    dateFilter: HistoryDateFilter,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (dateFilter is HistoryDateFilter.All) return

    val label = when (dateFilter) {
        is HistoryDateFilter.All -> ""
        is HistoryDateFilter.SingleDay -> {
            dateFilter.date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
        }
        is HistoryDateFilter.Period -> {
            val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
            val yearFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
            if (dateFilter.startDate.year == dateFilter.endDate.year) {
                "${dateFilter.startDate.format(formatter)} – ${dateFilter.endDate.format(yearFormatter)}"
            } else {
                "${dateFilter.startDate.format(yearFormatter)} – ${dateFilter.endDate.format(yearFormatter)}"
            }
        }
    }

    InputChip(
        selected = true,
        onClick = onClear,
        label = { Text(text = label) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.history_date_filter_clear_cd),
                modifier = Modifier
                    .size(16.dp)
                    .testTag("clear_date_filter_icon")
            )
        },
        modifier = modifier.testTag("history_date_filter_chip")
    )
}
