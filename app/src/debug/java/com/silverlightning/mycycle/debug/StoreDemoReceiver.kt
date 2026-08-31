package com.silverlightning.mycycle.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.silverlightning.mycycle.data.preferences.UserPreferencesRepository
import com.silverlightning.mycycle.data.repository.CycleDayRepository
import com.silverlightning.mycycle.domain.model.CycleDay
import com.silverlightning.mycycle.domain.model.CycleStage
import com.silverlightning.mycycle.domain.model.FlowIntensity
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class StoreDemoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SEED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                seedDemoData()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun seedDemoData() {
        val koin = GlobalContext.get()
        val cycleDayRepository = koin.get<CycleDayRepository>()
        val preferencesRepository = koin.get<UserPreferencesRepository>()
        val today = LocalDate.now()
        val periodStarts = listOf(158L, 130L, 102L, 74L, 46L, 18L)
            .map(today::minusDays)

        cycleDayRepository.deleteAll()
        preferencesRepository.clearAll()

        periodStarts.forEach { start ->
            val intensities = listOf(
                FlowIntensity.MEDIUM,
                FlowIntensity.HEAVY,
                FlowIntensity.MEDIUM,
                FlowIntensity.LIGHT,
                FlowIntensity.LIGHT,
            )
            intensities.forEachIndexed { index, intensity ->
                cycleDayRepository.save(
                    CycleDay(
                        date = start.plusDays(index.toLong()),
                        hasPeriod = true,
                        flowIntensity = intensity,
                    )
                )
            }
        }

        preferencesRepository.completeOnboarding(
            lastPeriodDate = periodStarts.last(),
            cycleLength = 28,
            cycleStage = CycleStage.ESTABLISHED,
            periodLength = 5,
        )
    }

    private companion object {
        const val ACTION_SEED = "com.silverlightning.mycycle.SEED_STORE_DEMO"
    }
}
