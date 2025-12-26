package com.example.mycycle.domain.model

import androidx.annotation.StringRes
import com.example.mycycle.R

enum class CyclePhase(
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int
) {
    MENSTRUAL(R.string.phase_menstrual, R.string.phase_menstrual_desc),
    FOLLICULAR(R.string.phase_follicular, R.string.phase_follicular_desc),
    OVULATORY(R.string.phase_ovulatory, R.string.phase_ovulatory_desc),
    LUTEAL(R.string.phase_luteal, R.string.phase_luteal_desc)
}
