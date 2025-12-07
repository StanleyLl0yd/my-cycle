package com.example.mycycle.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycycle.data.CycleRepository
import com.example.mycycle.model.Cycle
import com.example.mycycle.R
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class CalendarUiState(
    val cycles: List<Cycle> = emptyList(),
    val activeCycle: Cycle? = null,
    val predictedNextStart: LocalDate? = null,
)

@HiltViewModel
internal class CalendarViewModel @Inject constructor(
    cycleRepository: CycleRepository
) : ViewModel() {

    val state = combine(
        cycleRepository.cycles,
        cycleRepository.activeCycle
    ) { cycles, active ->
        val averageLength = cycles.mapNotNull { it.endDate?.toEpochDay()?.minus(it.startDate.toEpochDay()) }
            .average()
            .takeIf { !it.isNaN() }
            ?.toInt() ?: 28
        val latestEnd = cycles.firstOrNull()?.endDate ?: LocalDate.now()
        val predicted = latestEnd.plusDays(averageLength.toLong())
        CalendarUiState(cycles = cycles, activeCycle = active, predictedNextStart = predicted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    init {
        // Start a new cycle automatically if the current one ended yesterday
        viewModelScope.launch {
            val today = LocalDate.now()
            val current = state.value.activeCycle
            if (current == null) {
                cycleRepository.startCycle(today.minusDays(1))
            }
        }
    }

    fun startNewCycle() {
        viewModelScope.launch {
            val today = LocalDate.now()
            cycleRepository.startCycle(today)
        }
    }

    fun endActiveCycle() {
        viewModelScope.launch {
            val today = LocalDate.now()
            cycleRepository.endCycle(today)
        }
    }
}

@Composable
fun CalendarScreen(titleRes: Int, viewModel: CalendarViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(text = stringResource(id = titleRes)) })
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CycleSummaryCard(state = state, onStartNew = viewModel::startNewCycle, onEndActive = viewModel::endActiveCycle)
            }
            items(state.cycles) { cycle ->
                CycleCard(cycle = cycle)
            }
        }
    }
}

@Composable
private fun CycleSummaryCard(state: CalendarUiState, onStartNew: () -> Unit, onEndActive: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(id = R.string.cycle_summary_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            val nextStart = state.predictedNextStart?.toString() ?: "—"
            Text(text = stringResource(id = R.string.cycle_next_start, nextStart))
            val active = state.activeCycle
            if (active != null) {
                Text(text = stringResource(id = R.string.cycle_active_range, active.startDate, active.endDate ?: "…"))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onEndActive) { Text(stringResource(id = R.string.cycle_end_button)) }
                    OutlinedButton(onClick = onStartNew) { Text(stringResource(id = R.string.cycle_restart_button)) }
                }
            } else {
                Button(onClick = onStartNew) {
                    Text(text = stringResource(id = R.string.cycle_start_button))
                }
            }
        }
    }
}

@Composable
private fun CycleCard(cycle: Cycle) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = stringResource(id = R.string.cycle_range_label, cycle.startDate, cycle.endDate ?: "—"), fontWeight = FontWeight.SemiBold)
            Text(
                text = stringResource(id = R.string.cycle_confidence_label, (cycle.confidence * 100).toInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
