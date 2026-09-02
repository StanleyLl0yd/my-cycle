package com.sl.mycycle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush

object CycleColors {

    @Composable
    fun backgroundGradient(): Brush {
        val background = MaterialTheme.colorScheme.background
        val surface = MaterialTheme.colorScheme.surface
        return remember(background, surface) {
            Brush.verticalGradient(
                colors = listOf(background, surface, background)
            )
        }
    }
}
