package com.example.mycycle.ui.today

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Spa
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mycycle.R
import com.example.mycycle.domain.model.CyclePhase
import com.example.mycycle.ui.theme.CycleColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun TodayScreen(
    onLogClick: () -> Unit,
    viewModel: TodayViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CycleColors.backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Phase card
            state.phase?.let { phase ->
                PhaseCard(
                    phase = phase,
                    cycleDay = state.cycleDay,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info cards row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                state.daysUntilPeriod?.let { days ->
                    if (days > 0) {
                        InfoCard(
                            icon = Icons.Rounded.CalendarMonth,
                            title = stringResource(R.string.today_until_period),
                            value = pluralStringResource(R.plurals.days, days, days),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (state.isFertileNow) {
                    InfoCard(
                        icon = Icons.Rounded.Spa,
                        title = stringResource(R.string.today_fertile_window),
                        value = stringResource(R.string.today_fertile_now),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    state.daysUntilFertile?.let { days ->
                        if (days > 0) {
                            InfoCard(
                                icon = Icons.Rounded.Spa,
                                title = stringResource(R.string.today_fertile_window),
                                value = pluralStringResource(R.plurals.fertile_in_days, days, days),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Log button
            ExtendedFloatingActionButton(
                onClick = onLogClick,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.today_mark_period)) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PhaseCard(
    phase: CyclePhase,
    cycleDay: Int?,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = CycleColors.getPhaseColor(phase).copy(alpha = 0.15f),
        label = "phase_color"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(phase.labelRes),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            cycleDay?.let { day ->
                val scale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "day_scale"
                )

                Text(
                    text = day.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = CycleColors.getPhaseColor(phase),
                    modifier = Modifier.scale(scale)
                )

                Text(
                    text = stringResource(R.string.today_cycle_day),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(phase.descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
