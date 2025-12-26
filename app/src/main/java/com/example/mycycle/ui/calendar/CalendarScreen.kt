package com.example.mycycle.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mycycle.R
import com.example.mycycle.domain.model.DayState
import com.example.mycycle.domain.model.FertilityState
import com.example.mycycle.domain.model.PeriodState
import com.example.mycycle.ui.theme.CycleColors
import com.example.mycycle.ui.theme.Fertile
import com.example.mycycle.ui.theme.Ovulation
import com.example.mycycle.ui.theme.PeriodHeavy
import com.example.mycycle.ui.theme.PeriodLight
import com.example.mycycle.ui.theme.PeriodMedium
import com.example.mycycle.ui.theme.PeriodPredicted
import com.example.mycycle.ui.theme.PeriodStrong
import org.koin.androidx.compose.koinViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    onDayClick: (String) -> Unit,
    viewModel: CalendarViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locale = LocalContext.current.resources.configuration.locales[0]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CycleColors.backgroundGradient)
            .padding(16.dp)
    ) {
        // Month header
        MonthHeader(
            yearMonth = state.currentMonth,
            onPreviousMonth = viewModel::previousMonth,
            onNextMonth = viewModel::nextMonth,
            onTodayClick = viewModel::goToToday,
            locale = locale
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Weekday headers
        WeekdayHeaders(locale = locale)

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar grid
        AnimatedContent(
            targetState = state.currentMonth,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 3 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally { it / 3 } + fadeOut())
                }
            },
            label = "month_transition"
        ) { month ->
            MonthGrid(
                yearMonth = month,
                dayStates = state.dayStates,
                onDayClick = { date -> onDayClick(date.toString()) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend
        CalendarLegend()
    }
}

@Composable
private fun MonthHeader(
    yearMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    locale: Locale
) {
    val formatter = remember(locale) {
        DateTimeFormatter.ofPattern("LLLL yyyy", locale)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.a11y_previous_month)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = yearMonth.format(formatter).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                },
                style = MaterialTheme.typography.titleLarge
            )

            if (yearMonth != YearMonth.now()) {
                TextButton(onClick = onTodayClick) {
                    Text(stringResource(R.string.calendar_today))
                }
            }
        }

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = stringResource(R.string.a11y_next_month)
            )
        }
    }
}

@Composable
private fun WeekdayHeaders(locale: Locale) {
    val daysOfWeek = remember(locale) {
        val firstDayOfWeek = DayOfWeek.MONDAY
        (0..6).map { firstDayOfWeek.plus(it.toLong()) }
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        daysOfWeek.forEach { dayOfWeek ->
            Text(
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    dayStates: Map<LocalDate, DayState>,
    onDayClick: (LocalDate) -> Unit
) {
    val days = remember(yearMonth) {
        val firstOfMonth = yearMonth.atDay(1)
        val lastOfMonth = yearMonth.atEndOfMonth()

        val dayOfWeekOfFirst = firstOfMonth.dayOfWeek.value
        val leadingEmptyDays = (dayOfWeekOfFirst - 1)

        val daysInMonth = yearMonth.lengthOfMonth()
        val totalCells = leadingEmptyDays + daysInMonth

        buildList {
            repeat(leadingEmptyDays) { add(null) }
            for (day in 1..daysInMonth) {
                add(yearMonth.atDay(day))
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(days) { date ->
            if (date != null) {
                val dayState = dayStates[date]
                DayCell(
                    date = date,
                    dayState = dayState,
                    onClick = { onDayClick(date) }
                )
            } else {
                Box(modifier = Modifier.aspectRatio(1f))
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    dayState: DayState?,
    onClick: () -> Unit
) {
    val backgroundColor = when (dayState?.periodState) {
        PeriodState.CONFIRMED_SPOTTING -> PeriodLight.copy(alpha = 0.5f)
        PeriodState.CONFIRMED_LIGHT -> PeriodMedium.copy(alpha = 0.5f)
        PeriodState.CONFIRMED_MEDIUM -> PeriodStrong.copy(alpha = 0.5f)
        PeriodState.CONFIRMED_HEAVY -> PeriodHeavy.copy(alpha = 0.5f)
        PeriodState.PREDICTED -> PeriodPredicted
        else -> when (dayState?.fertilityState) {
            FertilityState.FERTILE_PREDICTED -> Fertile.copy(alpha = 0.3f)
            FertilityState.OVULATION_PREDICTED -> Ovulation.copy(alpha = 0.3f)
            else -> Color.Transparent
        }
    }

    val isToday = dayState?.isToday == true

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .then(
                if (isToday) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isToday) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            // Indicators
            if (dayState?.periodState != PeriodState.NONE && dayState?.periodState != PeriodState.PREDICTED) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val dots = when (dayState?.periodState) {
                        PeriodState.CONFIRMED_SPOTTING -> 1
                        PeriodState.CONFIRMED_LIGHT -> 1
                        PeriodState.CONFIRMED_MEDIUM -> 2
                        PeriodState.CONFIRMED_HEAVY -> 3
                        else -> 0
                    }
                    repeat(dots) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(PeriodHeavy)
                        )
                    }
                }
            }

            if (dayState?.fertilityState == FertilityState.OVULATION_PREDICTED) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Ovulation)
                )
            }
        }
    }
}

@Composable
private fun CalendarLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem(color = PeriodMedium, label = stringResource(R.string.calendar_legend_period))
        LegendItem(color = Fertile, label = stringResource(R.string.calendar_legend_fertile))
        LegendItem(color = Ovulation, label = stringResource(R.string.calendar_legend_ovulation))
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.5f))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
