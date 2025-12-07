package com.example.mycycle.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycycle.data.CycleRepository
import com.example.mycycle.data.LogRepository
import com.example.mycycle.R
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal data class InsightsUiState(
    val averageCycleLength: Int = 0,
    val lastPeriodStart: LocalDate? = null,
    val mostCommonSymptom: String? = null,
    val moodBalance: Map<String, Int> = emptyMap(),
    val cycleLengths: List<Int> = emptyList(),
)

@HiltViewModel
internal class InsightsViewModel @Inject constructor(
    cycleRepository: CycleRepository,
    logRepository: LogRepository,
) : ViewModel() {

    val state = combine(
        cycleRepository.cycles,
        logRepository.logs
    ) { cycles, logs ->
        val lengths = cycles.mapNotNull { cycle ->
            cycle.endDate?.let { end -> (end.toEpochDay() - cycle.startDate.toEpochDay()).toInt() }
        }
        val average = lengths.takeIf { it.isNotEmpty() }?.average()?.toInt() ?: 0
        val lastStart = cycles.maxByOrNull { it.startDate }?.startDate
        val symptomCounts = logs.flatMap { it.symptoms }.groupingBy { it }.eachCount()
        val topSymptom = symptomCounts.maxByOrNull { it.value }?.key
        val moodCounts = logs.mapNotNull { it.mood?.name }.
            groupingBy { it }.
            eachCount()

        InsightsUiState(
            averageCycleLength = average,
            lastPeriodStart = lastStart,
            mostCommonSymptom = topSymptom,
            moodBalance = moodCounts,
            cycleLengths = lengths
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())
}

@Composable
fun InsightsScreen(titleRes: Int, viewModel: InsightsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(id = titleRes)) })
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { InsightCard(title = stringResource(id = R.string.insights_avg_cycle), value = state.averageCycleLength.toString()) }
            item { InsightCard(title = stringResource(id = R.string.insights_last_start), value = state.lastPeriodStart?.toString() ?: "—") }
            item { InsightCard(title = stringResource(id = R.string.insights_common_symptom), value = state.mostCommonSymptom ?: stringResource(id = R.string.insights_not_enough_data)) }
            item {
                InsightCard(title = stringResource(id = R.string.insights_mood_balance), value = state.moodBalance.entries.joinToString { (mood, count) -> "$mood: $count" })
            }
            item {
                ChartCard(title = stringResource(id = R.string.insights_cycle_trend)) {
                    if (state.cycleLengths.isEmpty()) {
                        Text(text = stringResource(id = R.string.insights_not_enough_data), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        CycleTrendChart(lengths = state.cycleLengths)
                    }
                }
            }
            item {
                ChartCard(title = stringResource(id = R.string.insights_mood_chart)) {
                    if (state.moodBalance.isEmpty()) {
                        Text(text = stringResource(id = R.string.insights_not_enough_data), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        MoodDistributionChart(moods = state.moodBalance)
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightCard(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun CycleTrendChart(lengths: List<Int>) {
    val maxLength = lengths.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
    val barColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        val barWidth = if (lengths.isNotEmpty()) size.width / (lengths.size * 1.8f) else 0f
        lengths.forEachIndexed { index, length ->
            val normalized = length / maxLength
            val barHeight = size.height * normalized
            val left = index * (barWidth * 1.8f) + barWidth * 0.4f
            val top = size.height - barHeight
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x = left, y = top),
                size = Size(width = barWidth, height = barHeight),
                cornerRadius = CornerRadius(12f, 12f)
            )
        }
    }
}

@Composable
private fun MoodDistributionChart(moods: Map<String, Int>) {
    val total = moods.values.sum().coerceAtLeast(1)
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.surfaceTint
    )
    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
        var startX = 0f
        moods.entries.sortedByDescending { it.value }.forEachIndexed { index, entry ->
            val fraction = entry.value.toFloat() / total
            val barWidth = size.width * fraction
            drawRoundRect(
                brush = Brush.horizontalGradient(colors = listOf(colors[index % colors.size], colors.reversed()[index % colors.size])),
                topLeft = Offset(startX, size.height / 3f),
                size = Size(barWidth, size.height / 3f),
                cornerRadius = CornerRadius(16f, 16f)
            )
            startX += barWidth
        }
    }
    Text(
        text = moods.entries.joinToString { (mood, count) -> "$mood: $count" },
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 8.dp)
    )
}
