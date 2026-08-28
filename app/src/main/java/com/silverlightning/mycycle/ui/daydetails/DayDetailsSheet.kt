package com.silverlightning.mycycle.ui.daydetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silverlightning.mycycle.R
import com.silverlightning.mycycle.domain.model.FlowIntensity
import com.silverlightning.mycycle.domain.model.Mood
import com.silverlightning.mycycle.domain.model.Symptom
import com.silverlightning.mycycle.ui.theme.PeriodHeavy
import com.silverlightning.mycycle.ui.theme.PeriodLight
import com.silverlightning.mycycle.ui.theme.PeriodMedium
import com.silverlightning.mycycle.ui.theme.PeriodStrong
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DayDetailsSheet(
    dateString: String,
    onDismiss: () -> Unit,
    viewModel: DayDetailsViewModel = koinViewModel { parametersOf(dateString) }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val locale = LocalConfiguration.current.locales[0]

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = state.date.format(
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)
                ),
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.day_details_period_section),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.day_details_bleeding_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowOption(
                    label = stringResource(R.string.flow_none),
                    isSelected = state.flowIntensity == null && !state.hasPeriod,
                    onClick = { viewModel.setFlowIntensity(null) }
                )

                FlowIntensity.entries.forEach { intensity ->
                    FlowOption(
                        label = stringResource(intensity.labelRes),
                        intensity = intensity,
                        isSelected = state.flowIntensity == intensity,
                        onClick = { viewModel.setFlowIntensity(intensity) }
                    )
                }
            }

            if (state.hasPeriod && state.flowIntensity == null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.day_details_bleeding_unknown),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.day_details_mood_section),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Mood.entries.forEach { mood ->
                    MoodOption(
                        mood = mood,
                        isSelected = state.mood == mood,
                        onClick = {
                            viewModel.setMood(if (state.mood == mood) null else mood)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.day_details_symptoms_section),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Symptom.entries.forEach { symptom ->
                    FilterChip(
                        selected = symptom in state.symptoms,
                        onClick = { viewModel.toggleSymptom(symptom) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(symptom.emoji)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(symptom.labelRes))
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.day_details_notes_section),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::setNotes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                placeholder = {
                    Text(stringResource(R.string.day_details_notes_hint))
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (state.isFutureDate) {
                Text(
                    text = stringResource(R.string.error_future_date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (state.hasSaveError) {
                Text(
                    text = stringResource(R.string.error_generic),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && !state.isFutureDate && !state.isSaving
            ) {
                Text(stringResource(R.string.day_details_save))
            }
        }
    }
}

@Composable
private fun FlowOption(
    label: String,
    intensity: FlowIntensity? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val accent = when (intensity) {
        FlowIntensity.SPOTTING -> PeriodLight
        FlowIntensity.LIGHT -> PeriodMedium
        FlowIntensity.MEDIUM -> PeriodStrong
        FlowIntensity.HEAVY -> PeriodHeavy
        null -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) {
                    accent.copy(alpha = 0.22f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, accent, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (intensity != null) {
                Row {
                    repeat(intensity.level) {
                        Icon(
                            imageVector = Icons.Rounded.WaterDrop,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = accent
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MoodOption(
    mood: Mood,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = mood.emoji,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(mood.labelRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
