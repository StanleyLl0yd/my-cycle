package com.sl.mycycle.data.repository

import androidx.room.withTransaction
import com.sl.mycycle.data.local.AppDatabase
import com.sl.mycycle.data.local.CycleDayEntity
import com.sl.mycycle.domain.model.CycleDay
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CycleDayRepository(
    private val database: AppDatabase
) {
    private val dao = database.cycleDayDao()

    fun observeAll(): Flow<List<CycleDay>> =
        dao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    fun observeByDate(date: LocalDate): Flow<CycleDay?> =
        dao.observeByDate(date).map { it?.toDomain() }

    suspend fun getByDate(date: LocalDate): CycleDay? =
        dao.getByDate(date)?.toDomain()

    suspend fun save(cycleDay: CycleDay) {
        val existing = dao.getByDate(cycleDay.date)
        val now = Instant.now()
        val entity = CycleDayEntity.fromDomain(cycleDay).copy(
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        dao.upsert(entity)
    }

    suspend fun saveAll(cycleDays: List<CycleDay>) {
        if (cycleDays.isEmpty()) return

        database.withTransaction {
            cycleDays.chunked(SQLITE_BIND_PARAMETER_LIMIT).forEach { batch ->
                val createdAtByDate = dao.getByDates(batch.map { it.date })
                    .associate { it.date to it.createdAt }
                    .toMutableMap()
                val entities = batch.map { cycleDay ->
                    val now = Instant.now()
                    val createdAt = createdAtByDate.getOrPut(cycleDay.date) { now }
                    CycleDayEntity.fromDomain(cycleDay).copy(
                        createdAt = createdAt,
                        updatedAt = now
                    )
                }
                dao.upsertAll(entities)
            }
        }
    }

    suspend fun delete(date: LocalDate) {
        dao.delete(date)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    private companion object {
        const val SQLITE_BIND_PARAMETER_LIMIT = 999
    }
}
