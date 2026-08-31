package com.sl.mycycle.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sl.mycycle.R
import com.sl.mycycle.domain.model.CycleNotice
import com.sl.mycycle.domain.model.CycleStage
import com.sl.mycycle.domain.model.DateRange
import com.sl.mycycle.domain.model.Prediction
import com.sl.mycycle.ui.theme.CycleColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import org.koin.androidx.compose.koinViewModel

@Composable
fun TodayScreen(
    onLogClick: (String) -> Unit,
    viewModel: TodayViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CycleColors.backgroundGradient())
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        CycleDayCard(
            cycleDay = state.cycleDay,
            isPeriodToday = state.isPeriodToday
        )

        Spacer(modifier = Modifier.height(16.dp))

        state.prediction?.let { prediction ->
            PredictionCard(
                prediction = prediction,
                today = state.today,
                locale = locale
            )
        }

        state.notice?.let { notice ->
            Spacer(modifier = Modifier.height(16.dp))
            NoticeCard(notice)
        }

        state.prediction?.possiblePregnancyWindow
            ?.takeIf { !state.today.isAfter(it.end) }
            ?.let { range ->
                Spacer(modifier = Modifier.height(16.dp))
                PregnancyCard(
                    range = range,
                    today = state.today,
                    locale = locale
                )
            }

        Spacer(modifier = Modifier.height(32.dp))

        ExtendedFloatingActionButton(
            onClick = { onLogClick(state.today.toString()) },
            icon = {
                Icon(
                    imageVector = if (state.isPeriodToday) Icons.Rounded.Check else Icons.Rounded.Add,
                    contentDescription = null
                )
            },
            text = {
                Text(
                    stringResource(
                        if (state.isPeriodToday) {
                            R.string.today_period_started
                        } else {
                            R.string.today_mark_period
                        }
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CycleDayCard(
    cycleDay: Int?,
    isPeriodToday: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(
                    if (isPeriodToday) R.string.today_period_today else R.string.today_cycle_day_title
                ),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )

            cycleDay?.let { day ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = day.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.today_cycle_day),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.today_cycle_day_help),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PredictionCard(
    prediction: Prediction,
    today: LocalDate,
    locale: Locale
) {
    val window = prediction.nextPeriodStartWindow

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.today_next_period_window),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            val value = when {
                window == null -> stringResource(R.string.today_no_period_prediction)
                today.isAfter(window.end) -> stringResource(R.string.today_prediction_passed)
                else -> formatDateRange(window, locale)
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = when {
                    window == null ->
                        stringResource(R.string.today_no_period_prediction_help)
                    prediction.stage == CycleStage.NOT_SET ->
                        stringResource(R.string.today_unset_prediction)
                    prediction.basedOnCycles == 0 ->
                        stringResource(R.string.today_learning_prediction)
                    prediction.outsideCommonRange ->
                        stringResource(R.string.today_outside_range_prediction)
                    prediction.highlyVariable ->
                        stringResource(R.string.today_wide_prediction)
                    else ->
                        stringResource(R.string.today_history_prediction)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PregnancyCard(
    range: DateRange,
    today: LocalDate,
    locale: Locale
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.today_possible_pregnancy),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (today in range) {
                    stringResource(R.string.today_possible_pregnancy_now)
                } else {
                    formatDateRange(range, locale)
                },
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.today_calendar_guess_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoticeCard(notice: CycleNotice) {
    val important = notice in setOf(
        CycleNotice.THREE_MONTH_GAP,
        CycleNotice.LONG_UNEXPLAINED_GAP,
        CycleNotice.BLEEDING_AFTER_YEAR_GAP,
        CycleNotice.LONG_BLEEDING
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (important) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Text(
            text = stringResource(
                when (notice) {
                    CycleNotice.CYCLE_STAGE_NOT_SET -> R.string.today_notice_stage_not_set
                    CycleNotice.FIRST_YEAR_CHANGES_ARE_COMMON -> R.string.today_notice_first_year
                    CycleNotice.EARLY_YEARS_CHANGES_ARE_COMMON -> R.string.today_notice_early_years
                    CycleNotice.LONG_TERM_UNEVEN -> R.string.today_notice_long_term_uneven
                    CycleNotice.CHANGING_WITH_AGE -> R.string.today_notice_changing_age
                    CycleNotice.PERIODS_STOPPED -> R.string.today_notice_periods_stopped
                    CycleNotice.THREE_MONTH_GAP -> R.string.today_notice_three_month_gap
                    CycleNotice.LONG_UNEXPLAINED_GAP -> R.string.today_notice_long_unexplained_gap
                    CycleNotice.BLEEDING_AFTER_YEAR_GAP -> R.string.today_notice_year_gap_bleeding
                    CycleNotice.LONG_BLEEDING -> R.string.today_notice_long_bleeding
                    CycleNotice.OUTSIDE_COMMON_RANGE -> R.string.today_notice_outside_common_range
                }
            ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(20.dp),
            color = if (important) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
        )
    }
}

private fun formatDateRange(range: DateRange, locale: Locale): String {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    return if (range.start == range.end) {
        range.start.format(formatter)
    } else {
        "${range.start.format(formatter)} – ${range.end.format(formatter)}"
    }
}
