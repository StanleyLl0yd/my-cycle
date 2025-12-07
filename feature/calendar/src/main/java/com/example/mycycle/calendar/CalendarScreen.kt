package com.example.mycycle.calendar

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
import com.example.mycycle.model.Cycle

@Composable
fun CalendarScreen(titleRes: Int) {
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
                text = "${'$'}{Cycle(id = \"demo\", startDate = java.time.LocalDate.now(), endDate = null, averageLengthDays = 28, lutealPhaseDays = 14, confidence = 0.9f)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
