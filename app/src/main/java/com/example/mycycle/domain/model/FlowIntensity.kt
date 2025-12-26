package com.example.mycycle.domain.model

import androidx.annotation.StringRes
import com.example.mycycle.R

enum class FlowIntensity(
    val level: Int,
    @StringRes val labelRes: Int
) {
    SPOTTING(1, R.string.flow_spotting),
    LIGHT(2, R.string.flow_light),
    MEDIUM(3, R.string.flow_medium),
    HEAVY(4, R.string.flow_heavy);

    companion object {
        fun fromLevel(level: Int): FlowIntensity? =
            entries.find { it.level == level }
    }
}
