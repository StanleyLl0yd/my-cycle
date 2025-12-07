package com.example.mycycle.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mycycle.designsystem.MyCycleTheme
import com.example.mycycle.model.BleedingLevel
import com.example.mycycle.model.LogEntry
import com.example.mycycle.model.Mood

@Composable
fun LogScreen(titleRes: Int) {
    MyCycleTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = titleRes),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = LogEntry(
                    id = "demo-log",
                    date = java.time.LocalDate.now(),
                    bleedingLevel = BleedingLevel.MEDIUM,
                    symptoms = listOf("cramps", "fatigue"),
                    mood = Mood.NEUTRAL,
                    temperatureCelsius = 36.6f,
                    weightKg = 60.5f,
                    notes = "Demo"
                ).toString(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
