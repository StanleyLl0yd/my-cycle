package com.example.mycycle.data.repository

import com.example.mycycle.data.local.CycleDayDao
import com.example.mycycle.data.local.CycleDayEntity
import com.example.mycycle.domain.model.CycleDay
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CycleDayRepository(
    private val dao: CycleDayDao
) {
    fun observeAll(): Flow<List<CycleDay>> =
        dao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<CycleDay>> =
        dao.observeRange(start, end).map { entities ->
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

    suspend fun delete(date: LocalDate) {
        dao.getByDate(date)?.let { dao.delete(it) }
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}
