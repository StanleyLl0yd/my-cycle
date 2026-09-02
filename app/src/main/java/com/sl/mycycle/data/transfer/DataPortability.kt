package com.sl.mycycle.data.transfer

import com.sl.mycycle.data.preferences.UserPreferencesRepository
import com.sl.mycycle.data.repository.CycleDayRepository
import com.sl.mycycle.domain.model.CycleDay
import com.sl.mycycle.domain.model.CycleStage
import com.sl.mycycle.domain.model.FlowIntensity
import com.sl.mycycle.domain.model.Mood
import com.sl.mycycle.domain.model.Symptom
import com.sl.mycycle.domain.model.ThemeMode
import com.sl.mycycle.domain.model.UserPreferences
import java.time.LocalDate
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class CsvImportPreview(
    val totalRecords: Int,
    val newRecords: Int,
    val replacedRecords: Int
)

data class BackupPreview(
    val diaryRecords: Int,
    val onboardingCompleted: Boolean
)

data class BackupSnapshot(
    val preferences: UserPreferences,
    val days: List<CycleDay>
)

class DataPortabilityService(
    private val preferencesRepository: UserPreferencesRepository,
    private val cycleDayRepository: CycleDayRepository
) {
    suspend fun buildCsv(): String = CsvCodec.encodeDays(
        cycleDayRepository.observeAll().first().sortedBy { it.date }
    )

    suspend fun previewCsv(csv: String): CsvImportPreview {
        val incoming = CsvCodec.decodeDays(csv)
        validateDays(incoming)
        val existingDates = cycleDayRepository.observeAll().first().mapTo(hashSetOf()) { it.date }
        val replaced = incoming.count { it.date in existingDates }
        return CsvImportPreview(
            totalRecords = incoming.size,
            newRecords = incoming.size - replaced,
            replacedRecords = replaced
        )
    }

    suspend fun importCsv(csv: String) {
        val incoming = CsvCodec.decodeDays(csv)
        validateDays(incoming)
        cycleDayRepository.saveAll(incoming)
    }

    suspend fun buildBackup(): String = BackupCodec.encode(
        preferences = preferencesRepository.preferences.first(),
        days = cycleDayRepository.observeAll().first().sortedBy { it.date }
    )

    fun previewBackup(backup: String): BackupPreview {
        val snapshot = BackupCodec.decode(backup)
        validateDays(snapshot.days)
        return BackupPreview(
            diaryRecords = snapshot.days.size,
            onboardingCompleted = snapshot.preferences.onboardingCompleted
        )
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun restoreBackup(backup: String) {
        val snapshot = BackupCodec.decode(backup)
        validateDays(snapshot.days)
        val restoredPreferences = snapshot.preferences.copy(appLockEnabled = false)
        val oldDays = cycleDayRepository.observeAll().first()
        val oldPreferences = preferencesRepository.preferences.first()
        var daysReplaced = false

        try {
            cycleDayRepository.replaceAll(snapshot.days)
            daysReplaced = true
            preferencesRepository.replaceAll(restoredPreferences)
        } catch (error: Throwable) {
            withContext(NonCancellable) {
                if (daysReplaced) {
                    runCatching { cycleDayRepository.replaceAll(oldDays) }
                        .exceptionOrNull()
                        ?.let(error::addSuppressed)
                }
                runCatching { preferencesRepository.replaceAll(oldPreferences) }
                    .exceptionOrNull()
                    ?.let(error::addSuppressed)
            }
            throw error
        }
    }

    private fun validateDays(days: List<CycleDay>) {
        require(days.none { it.date.isAfter(LocalDate.now()) })
    }
}

object CsvCodec {
    private val header = listOf("date", "period", "flow", "mood", "symptoms", "notes")

    fun encodeDays(days: List<CycleDay>): String = buildString {
        append('\uFEFF')
        appendLine(header.joinToString(","))
        days.forEach { day ->
            appendLine(
                listOf(
                    day.date.toString(),
                    day.hasPeriod.toString(),
                    day.flowIntensity?.name.orEmpty(),
                    day.mood?.name.orEmpty(),
                    day.symptoms.joinToString("|") { it.name },
                    day.notes.orEmpty()
                ).joinToString(",", transform = ::escape)
            )
        }
    }

    fun decodeDays(csv: String): List<CycleDay> {
        val rows = parseRows(csv.removePrefix("\uFEFF"))
        require(rows.isNotEmpty() && rows.first() == header)
        val days = rows.drop(1).filterNot { row -> row.all(String::isBlank) }.map { row ->
            require(row.size == header.size)
            CycleDay(
                date = LocalDate.parse(row[0]),
                hasPeriod = row[1].toBooleanStrictOrNull()
                    ?: throw IllegalArgumentException("Invalid period value"),
                flowIntensity = enumOrNull<FlowIntensity>(row[2]),
                mood = enumOrNull<Mood>(row[3]),
                symptoms = if (row[4].isBlank()) {
                    emptySet()
                } else {
                    row[4].split('|').mapTo(linkedSetOf()) { Symptom.valueOf(it) }
                },
                notes = row[5].ifEmpty { null }
            )
        }
        require(days.map { it.date }.distinct().size == days.size)
        return days
    }

    private fun escape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.any { it == ',' || it == '\n' || it == '\r' || it == '\"' }) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    private inline fun <reified T : Enum<T>> enumOrNull(value: String): T? =
        value.takeIf(String::isNotBlank)?.let { enumValueOf<T>(it) }

    private fun parseRows(input: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var index = 0

        fun finishField() {
            row += field.toString()
            field.setLength(0)
        }

        fun finishRow() {
            finishField()
            if (row.any(String::isNotEmpty)) rows += row
            row = mutableListOf()
        }

        while (index < input.length) {
            when (val char = input[index]) {
                '\"' -> {
                    if (quoted && index + 1 < input.length && input[index + 1] == '\"') {
                        field.append('\"')
                        index++
                    } else {
                        quoted = !quoted
                    }
                }
                ',' -> if (quoted) field.append(char) else finishField()
                '\n' -> if (quoted) field.append(char) else finishRow()
                '\r' -> if (quoted) {
                    field.append(char)
                } else {
                    if (index + 1 < input.length && input[index + 1] == '\n') index++
                    finishRow()
                }
                else -> field.append(char)
            }
            index++
        }

        require(!quoted)
        if (field.isNotEmpty() || row.isNotEmpty()) finishRow()
        return rows
    }
}

object BackupCodec {
    private const val MAGIC = "MYCYCLE_BACKUP_V1"
    private const val DAYS_SEPARATOR = "---DAYS---"

    fun encode(preferences: UserPreferences, days: List<CycleDay>): String = buildString {
        appendLine(MAGIC)
        appendLine("onboardingCompleted=${preferences.onboardingCompleted}")
        appendLine("initialPeriodDate=${preferences.initialPeriodDate.orEmpty()}")
        appendLine("estimatedCycleLength=${preferences.estimatedCycleLength}")
        appendLine("estimatedPeriodLength=${preferences.estimatedPeriodLength}")
        appendLine("cycleStage=${preferences.cycleStage.name}")
        appendLine("themeMode=${preferences.themeMode.name}")
        appendLine("useDynamicColors=${preferences.useDynamicColors}")
        appendLine("dailyReminderEnabled=${preferences.dailyReminderEnabled}")
        appendLine("reminderHour=${preferences.reminderHour}")
        appendLine("reminderMinute=${preferences.reminderMinute}")
        appendLine("appLockEnabled=${preferences.appLockEnabled}")
        appendLine("protectScreenEnabled=${preferences.protectScreenEnabled}")
        appendLine(DAYS_SEPARATOR)
        append(CsvCodec.encodeDays(days).removePrefix("\uFEFF"))
    }

    fun decode(backup: String): BackupSnapshot {
        val normalized = backup.removePrefix("\uFEFF")
        val separator = "\n$DAYS_SEPARATOR\n"
        val separatorIndex = normalized.indexOf(separator)
        require(separatorIndex > 0)

        val metadataLines = normalized.substring(0, separatorIndex).lineSequence().toList()
        require(metadataLines.firstOrNull() == MAGIC)
        val metadata = metadataLines.drop(1).associate { line ->
            val split = line.indexOf('=')
            require(split > 0)
            line.substring(0, split) to line.substring(split + 1)
        }

        val preferences = UserPreferences(
            onboardingCompleted = metadata.boolean("onboardingCompleted"),
            initialPeriodDate = metadata.getValue("initialPeriodDate")
                .takeIf(String::isNotBlank)
                ?.let(LocalDate::parse),
            estimatedCycleLength = metadata.int("estimatedCycleLength", 1..365),
            estimatedPeriodLength = metadata.int("estimatedPeriodLength", 1..30),
            cycleStage = CycleStage.valueOf(metadata.getValue("cycleStage")),
            themeMode = ThemeMode.valueOf(metadata.getValue("themeMode")),
            useDynamicColors = metadata.boolean("useDynamicColors"),
            dailyReminderEnabled = metadata.boolean("dailyReminderEnabled"),
            reminderHour = metadata.int("reminderHour", 0..23),
            reminderMinute = metadata.int("reminderMinute", 0..59),
            appLockEnabled = metadata.boolean("appLockEnabled"),
            protectScreenEnabled = metadata.boolean("protectScreenEnabled")
        )

        val daysCsv = normalized.substring(separatorIndex + separator.length)
        return BackupSnapshot(preferences, CsvCodec.decodeDays(daysCsv))
    }

    private fun Map<String, String>.boolean(key: String): Boolean =
        getValue(key).toBooleanStrictOrNull()
            ?: throw IllegalArgumentException("Invalid boolean: $key")

    private fun Map<String, String>.int(key: String, range: IntRange): Int =
        getValue(key).toIntOrNull()
            ?.takeIf { it in range }
            ?: throw IllegalArgumentException("Invalid number: $key")

    private fun LocalDate?.orEmpty(): String = this?.toString().orEmpty()
}
