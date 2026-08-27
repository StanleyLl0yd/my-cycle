package com.silverlightning.mycycle.domain.model

import androidx.annotation.StringRes
import com.silverlightning.mycycle.R

enum class Symptom(
    val bit: Int,
    val emoji: String,
    @StringRes val labelRes: Int
) {
    CRAMPS(1 shl 0, "⚡", R.string.symptom_cramps),
    HEADACHE(1 shl 1, "🤕", R.string.symptom_headache),
    FATIGUE(1 shl 2, "😴", R.string.symptom_fatigue),
    MOOD_SWINGS(1 shl 3, "💭", R.string.symptom_mood_swings),
    BLOATING(1 shl 4, "🫧", R.string.symptom_bloating),
    BREAST_TENDERNESS(1 shl 5, "💔", R.string.symptom_breast_tenderness),
    ACNE(1 shl 6, "✨", R.string.symptom_acne),
    BACKACHE(1 shl 7, "🔙", R.string.symptom_backache),
    NAUSEA(1 shl 8, "🤢", R.string.symptom_nausea),
    INSOMNIA(1 shl 9, "🌙", R.string.symptom_insomnia),
    CRAVINGS(1 shl 10, "🍫", R.string.symptom_cravings),
    DIZZINESS(1 shl 11, "💫", R.string.symptom_dizziness);

    companion object {
        fun fromMask(mask: Int): Set<Symptom> =
            entries.filter { (mask and it.bit) != 0 }.toSet()

        fun toMask(symptoms: Set<Symptom>): Int =
            symptoms.fold(0) { acc, symptom -> acc or symptom.bit }
    }
}
