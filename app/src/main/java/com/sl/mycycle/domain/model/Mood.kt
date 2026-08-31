package com.sl.mycycle.domain.model

import androidx.annotation.StringRes
import com.sl.mycycle.R

enum class Mood(
    val level: Int,
    val emoji: String,
    @StringRes val labelRes: Int
) {
    GREAT(4, "😊", R.string.mood_great),
    GOOD(3, "🙂", R.string.mood_good),
    OKAY(2, "😐", R.string.mood_okay),
    BAD(1, "😔", R.string.mood_bad);

    companion object {
        fun fromLevel(level: Int): Mood? =
            entries.find { it.level == level }
    }
}
