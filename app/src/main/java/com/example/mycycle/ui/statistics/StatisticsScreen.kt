package com.example.mycycle.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.mycycle.R
import com.example.mycycle.ui.theme.CycleColors

@Composable
fun StatisticsScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CycleColors.backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.stats_not_enough_data),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
