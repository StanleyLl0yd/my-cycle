package com.example.mycycle.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mycycle.R
import com.example.mycycle.domain.model.Cycle
import com.example.mycycle.ui.theme.CycleColors
import org.koin.androidx.compose.koinViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CycleColors.backgroundGradient)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.stats_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            return@Column
        }

        val averageCycleLength = state.averageCycleLength
        val averagePeriodLength = state.averagePeriodLength

        if (averageCycleLength == null || averagePeriodLength == null) {
            Text(
                text = stringResource(R.string.stats_not_enough_data),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 48.dp)
            )
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = stringResource(R.string.stats_avg_cycle),
                value = pluralStringResource(
                    R.plurals.days,
                    averageCycleLength,
                    averageCycleLength
                ),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = stringResource(R.string.stats_avg_period),
                value = pluralStringResource(
                    R.plurals.days,
                    averagePeriodLength,
                    averagePeriodLength
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        StatCard(
            title = stringResource(R.string.stats_regularity),
            value = regularityLabel(state.regularity),
            subtitle = stringResource(
                R.string.stats_based_on_cycles,
                state.completedCycleCount
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.stats_history_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        state.cycles.forEach { cycle ->
            CycleHistoryCard(cycle)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CycleHistoryCard(cycle: Cycle) {
    val locale: Locale = LocalConfiguration.current.locales[0]
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
    val cycleLength = cycle.length
    val value = if (cycle.isComplete && cycleLength != null) {
        pluralStringResource(R.plurals.days, cycleLength, cycleLength)
    } else {
        stringResource(R.string.stats_current_cycle)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cycle.startDate.format(formatter),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.stats_avg_period) + ": " +
                        pluralStringResource(
                            R.plurals.days,
                            cycle.periodLength,
                            cycle.periodLength
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun regularityLabel(regularity: CycleRegularity?): String = when (regularity) {
    CycleRegularity.REGULAR -> stringResource(R.string.stats_regular)
    CycleRegularity.SOMEWHAT_REGULAR -> stringResource(R.string.stats_somewhat_regular)
    CycleRegularity.IRREGULAR -> stringResource(R.string.stats_irregular)
    null -> stringResource(R.string.stats_not_enough_data)
}
