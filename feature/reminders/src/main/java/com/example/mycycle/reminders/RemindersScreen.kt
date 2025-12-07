package com.example.mycycle.reminders

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
import com.example.mycycle.model.Reminder
import com.example.mycycle.model.ReminderType

@Composable
fun RemindersScreen(titleRes: Int) {
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
                text = Reminder(
                    id = "demo-reminder",
                    type = ReminderType.CYCLE_START,
                    time = java.time.LocalTime.of(8, 0),
                    enabled = true
                ).toString(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
