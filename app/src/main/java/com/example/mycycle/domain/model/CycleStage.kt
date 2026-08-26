package com.example.mycycle.domain.model

import androidx.annotation.StringRes
import com.example.mycycle.R

enum class CycleStage(
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int
) {
    FIRST_YEAR(
        R.string.cycle_stage_first_year,
        R.string.cycle_stage_first_year_desc
    ),
    YEARS_ONE_TO_THREE(
        R.string.cycle_stage_one_to_three,
        R.string.cycle_stage_one_to_three_desc
    ),
    ESTABLISHED(
        R.string.cycle_stage_established,
        R.string.cycle_stage_established_desc
    ),
    LONG_TERM_UNEVEN(
        R.string.cycle_stage_long_term_uneven,
        R.string.cycle_stage_long_term_uneven_desc
    ),
    CHANGING_WITH_AGE(
        R.string.cycle_stage_changing_with_age,
        R.string.cycle_stage_changing_with_age_desc
    ),
    PERIODS_STOPPED(
        R.string.cycle_stage_periods_stopped,
        R.string.cycle_stage_periods_stopped_desc
    )
}
