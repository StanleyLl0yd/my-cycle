package com.sl.mycycle.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleDayDao {

    @Query("SELECT * FROM cycle_days ORDER BY date DESC")
    fun observeAll(): Flow<List<CycleDayEntity>>

    @Query("SELECT * FROM cycle_days WHERE date = :date")
    suspend fun getByDate(date: LocalDate): CycleDayEntity?

    @Query("SELECT * FROM cycle_days WHERE date = :date")
    fun observeByDate(date: LocalDate): Flow<CycleDayEntity?>

    @Upsert
    suspend fun upsert(day: CycleDayEntity)

    @Query("DELETE FROM cycle_days WHERE date = :date")
    suspend fun delete(date: LocalDate)

    @Query("DELETE FROM cycle_days")
    suspend fun deleteAll()
}
