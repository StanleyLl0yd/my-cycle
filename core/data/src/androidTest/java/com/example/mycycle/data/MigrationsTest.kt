package com.example.mycycle.data

import android.content.ContentValues
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mycycle.data.local.MyCycleDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class MigrationsTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MyCycleDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2_preservesData() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS `cycles` (
                    `id` TEXT NOT NULL,
                    `startDate` INTEGER NOT NULL,
                    `endDate` INTEGER,
                    `averageLengthDays` INTEGER NOT NULL,
                    `lutealPhaseDays` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS `logs` (
                    `id` TEXT NOT NULL,
                    `date` INTEGER NOT NULL,
                    `bleedingLevel` TEXT NOT NULL,
                    `symptoms` TEXT NOT NULL,
                    `mood` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS `reminders` (
                    `id` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `time` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            val cycleValues = ContentValues().apply {
                put("id", "cycle-1")
                put("startDate", 10L)
                put("endDate", 20L)
                put("averageLengthDays", 28)
                put("lutealPhaseDays", 14)
            }
            insert("cycles", 0, cycleValues)

            val reminderValues = ContentValues().apply {
                put("id", "reminder-1")
                put("type", "MEDICATION")
                put("time", 36000)
            }
            insert("reminders", 0, reminderValues)

            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, *MyCycleDatabase.migrations).apply {
            query("PRAGMA table_info(`cycles`)").use { cursor ->
                val names = buildList {
                    while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndex("name")))
                }
                check("confidence" in names)
            }
            query("SELECT enabled FROM reminders WHERE id='reminder-1'").use { cursor ->
                check(cursor.moveToFirst())
                check(cursor.getInt(0) == 1)
            }
        }
    }

    companion object {
        private const val TEST_DB = "migration-test.db"
    }
}
