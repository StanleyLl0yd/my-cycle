package com.example.mycycle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mycycle.data.local.entity.LogEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface LogEntryDao {
    @Query("SELECT * FROM logs ORDER BY date DESC")
    fun observeLogs(): Flow<List<LogEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LogEntryEntity)

    @Update
    suspend fun update(entry: LogEntryEntity)
}
