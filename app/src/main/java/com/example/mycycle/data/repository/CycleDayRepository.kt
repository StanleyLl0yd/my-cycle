package com.example.mycycle.data.repository

import com.example.mycycle.data.local.CycleDayDao
import com.example.mycycle.data.local.CycleDayEntity
import com.example.mycycle.domain.model.CycleDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

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

    fun observeAllPeriodDays(): Flow<List<CycleDay>> =
        dao.observeAllPeriodDays().map { entities ->
            entities.map { it.toDomain() }
        }

    fun observeLastPeriodDay(): Flow<CycleDay?> =
        dao.observeLastPeriodDay().map { it?.toDomain() }

    suspend fun getByDate(date: LocalDate): CycleDay? =
        dao.getByDate(date)?.toDomain()

    suspend fun getAllPeriodDays(): List<CycleDay> =
        dao.getAllPeriodDays().map { it.toDomain() }

    suspend fun getLastPeriodDay(): CycleDay? =
        dao.getLastPeriodDay()?.toDomain()

    suspend fun save(cycleDay: CycleDay) {
        dao.upsert(CycleDayEntity.fromDomain(cycleDay))
    }

    suspend fun delete(date: LocalDate) {
        dao.getByDate(date)?.let { dao.delete(it) }
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}
