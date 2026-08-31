package com.silverlightning.mycycle.ui.calendar

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
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silverlightning.mycycle.R
import com.silverlightning.mycycle.domain.model.DayState
import com.silverlightning.mycycle.domain.model.FertilityState
import com.silverlightning.mycycle.domain.model.PeriodState
import com.silverlightning.mycycle.ui.theme.CycleColors
import com.silverlightning.mycycle.ui.theme.Fertile
import com.silverlightning.mycycle.ui.theme.Ovulation
import com.silverlightning.mycycle.ui.theme.PeriodHeavy
import com.silverlightning.mycycle.ui.theme.PeriodLight
import com.silverlightning.mycycle.ui.theme.PeriodMedium
import com.silverlightning.mycycle.ui.theme.PeriodPredicted
import com.silverlightning.mycycle.ui.theme.PeriodStrong
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import org.koin.androidx.compose.koinViewModel

@Composable
fun CalendarScreen(
    onDayClick: (String) -> Unit,
    viewModel: CalendarViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val historicalPeriodState by viewModel.historicalPeriodState.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    var showHistoricalPeriod by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CycleColors.backgroundGradient())
            .padding(16.dp)
    ) {
        MonthHeader(
            yearMonth = state.currentMonth,
            today = state.today,
            onPreviousMonth = viewModel::previousMonth,
            onNextMonth = viewModel::nextMonth,
            onTodayClick = viewModel::goToToday,
            locale = locale
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.calendar_edit_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    viewModel.resetHistoricalPeriodEntry()
                    showHistoricalPeriod = true
                }
            ) {
                Text(stringResource(R.string.calendar_add_past_period))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        WeekdayHeaders(
            locale = locale,
            firstDayOfWeek = firstDayOfWeek
        )
        Spacer(modifier = Modifier.height(8.dp))

        AnimatedContent(
            targetState = state.currentMonth,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
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
                firstDayOfWeek = firstDayOfWeek,
                locale = locale,
                onDayClick = { date -> onDayClick(date.toString()) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        CalendarLegend(
            showFertility = state.prediction?.possiblePregnancyWindow != null,
            showOvulation = state.prediction?.possibleOvulationWindow != null
        )
    }

    if (showHistoricalPeriod) {
        HistoricalPeriodSheet(
            state = historicalPeriodState,
            locale = locale,
            onStartDateChanged = viewModel::setHistoricalPeriodStart,
            onEndDateChanged = viewModel::setHistoricalPeriodEnd,
            onSave = viewModel::saveHistoricalPeriod,
            onAddAnother = viewModel::prepareAnotherHistoricalPeriod,
            onDismiss = {
                if (!historicalPeriodState.isSaving) {
                    showHistoricalPeriod = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoricalPeriodSheet(
    state: HistoricalPeriodState,
    locale: Locale,
    onStartDateChanged: (LocalDate) -> Unit,
    onEndDateChanged: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onAddAnother: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val formatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
    var editStartDate by remember { mutableStateOf(false) }
    var editEndDate by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = {
            if (!state.isSaving) onDismiss()
        },
        sheetState = sheetState,
        sheetGesturesEnabled = !state.isSaving
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.history_period_title),
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { editStartDate = true },
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.history_period_start),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(state.startDate.format(formatter))
                    }
                }

                OutlinedButton(
                    onClick = { editEndDate = true },
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.history_period_end),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(state.endDate.format(formatter))
                    }
                }
            }

            if (state.hasSaveError) {
                Text(
                    text = stringResource(R.string.error_generic),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (state.isSaved) {
                Text(
                    text = stringResource(R.string.history_period_saved),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.history_done))
                }
                TextButton(
                    onClick = onAddAnother,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.history_add_another))
                }
            } else {
                Button(
                    onClick = onSave,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.history_period_save))
                }
                TextButton(
                    onClick = onDismiss,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        }
    }

    if (editStartDate) {
        HistoricalDatePicker(
            selectedDate = state.startDate,
            onDateSelected = onStartDateChanged,
            onDismiss = { editStartDate = false }
        )
    }

    if (editEndDate) {
        HistoricalDatePicker(
            selectedDate = state.endDate,
            onDateSelected = onEndDateChanged,
            onDismiss = { editEndDate = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoricalDatePicker(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(
                            Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        )
                    }
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun MonthHeader(
    yearMonth: YearMonth,
    today: LocalDate,
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

            if (yearMonth != YearMonth.from(today)) {
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
private fun WeekdayHeaders(
    locale: Locale,
    firstDayOfWeek: DayOfWeek
) {
    val daysOfWeek = remember(locale, firstDayOfWeek) {
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
    firstDayOfWeek: DayOfWeek,
    locale: Locale,
    onDayClick: (LocalDate) -> Unit
) {
    val days = remember(yearMonth, firstDayOfWeek) {
        val firstOfMonth = yearMonth.atDay(1)
        val leadingEmptyDays =
            (firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val daysInMonth = yearMonth.lengthOfMonth()

        buildList<LocalDate?> {
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
        modifier = Modifier.fillMaxSize()
    ) {
        items(days) { date ->
            if (date != null) {
                DayCell(
                    date = date,
                    dayState = dayStates[date],
                    locale = locale,
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
    locale: Locale,
    onClick: () -> Unit
) {
    val backgroundColor = when (dayState?.periodState) {
        PeriodState.CONFIRMED_UNSPECIFIED -> PeriodMedium.copy(alpha = 0.35f)
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
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)
    }
    val periodDescription = when (dayState?.periodState) {
        PeriodState.CONFIRMED_UNSPECIFIED ->
            stringResource(R.string.a11y_period_amount_unknown)
        PeriodState.CONFIRMED_SPOTTING -> stringResource(R.string.a11y_spotting_day)
        PeriodState.CONFIRMED_LIGHT,
        PeriodState.CONFIRMED_MEDIUM,
        PeriodState.CONFIRMED_HEAVY -> stringResource(R.string.a11y_period_day)
        PeriodState.PREDICTED -> stringResource(R.string.a11y_predicted_period)
        else -> null
    }
    val fertilityDescription = when (dayState?.fertilityState) {
        FertilityState.FERTILE_PREDICTED -> stringResource(R.string.a11y_fertile_window)
        FertilityState.OVULATION_PREDICTED -> stringResource(R.string.a11y_ovulation_day)
        else -> null
    }
    val spokenDescription = buildList {
        add(date.format(dateFormatter))
        if (isToday) add(stringResource(R.string.a11y_today))
        periodDescription?.let(::add)
        fertilityDescription?.let(::add)
    }.joinToString(". ")

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
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = spokenDescription
                role = Role.Button
                onClick {
                    onClick()
                    true
                }
            },
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

            if (
                dayState?.periodState != PeriodState.NONE &&
                dayState?.periodState != PeriodState.PREDICTED
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val dots = when (dayState?.periodState) {
                        PeriodState.CONFIRMED_SPOTTING,
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
private fun CalendarLegend(
    showFertility: Boolean,
    showOvulation: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LegendItem(
                color = PeriodMedium,
                label = stringResource(R.string.calendar_legend_period),
                modifier = Modifier.weight(1f)
            )
            LegendItem(
                color = PeriodPredicted,
                label = stringResource(R.string.calendar_legend_predicted),
                modifier = Modifier.weight(1f)
            )
        }

        if (showFertility || showOvulation) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showFertility) {
                    LegendItem(
                        color = Fertile,
                        label = stringResource(R.string.calendar_legend_fertile),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (showOvulation) {
                    LegendItem(
                        color = Ovulation,
                        label = stringResource(R.string.calendar_legend_ovulation),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.calendar_guess_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}
