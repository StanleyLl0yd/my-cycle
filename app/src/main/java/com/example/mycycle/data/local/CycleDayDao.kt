package com.example.mycycle.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface CycleDayDao {

    @Query("SELECT * FROM cycle_days ORDER BY date DESC")
    fun observeAll(): Flow<List<CycleDayEntity>>

    @Query("SELECT * FROM cycle_days WHERE date = :date")
    suspend fun getByDate(date: LocalDate): CycleDayEntity?

    @Query("SELECT * FROM cycle_days WHERE date = :date")
    fun observeByDate(date: LocalDate): Flow<CycleDayEntity?>

    @Query("SELECT * FROM cycle_days WHERE date BETWEEN :start AND :end ORDER BY date")
    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<CycleDayEntity>>

    @Query("SELECT * FROM cycle_days WHERE hasPeriod = 1 ORDER BY date")
    suspend fun getAllPeriodDays(): List<CycleDayEntity>

    @Query("SELECT * FROM cycle_days WHERE hasPeriod = 1 ORDER BY date")
    fun observeAllPeriodDays(): Flow<List<CycleDayEntity>>

    @Query("SELECT * FROM cycle_days WHERE hasPeriod = 1 ORDER BY date DESC LIMIT 1")
    suspend fun getLastPeriodDay(): CycleDayEntity?

    @Query("SELECT * FROM cycle_days WHERE hasPeriod = 1 ORDER BY date DESC LIMIT 1")
    fun observeLastPeriodDay(): Flow<CycleDayEntity?>

    @Upsert
    suspend fun upsert(day: CycleDayEntity)

    @Delete
    suspend fun delete(day: CycleDayEntity)

    @Query("DELETE FROM cycle_days")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM cycle_days WHERE hasPeriod = 1")
    suspend fun getPeriodDaysCount(): Int
}
