package com.example.mycycle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mycycle.data.local.entity.CycleEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface CycleDao {
    @Query("SELECT * FROM cycles ORDER BY startDate DESC")
    fun observeCycles(): Flow<List<CycleEntity>>

    @Query("SELECT * FROM cycles WHERE endDate IS NULL LIMIT 1")
    fun observeActive(): Flow<CycleEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cycle: CycleEntity)

    @Update
    suspend fun update(cycle: CycleEntity)
}
