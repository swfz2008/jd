package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QinglongConfigDao {
    @Query("SELECT * FROM qinglong_configs ORDER BY id ASC")
    fun getAllConfigs(): Flow<List<QinglongConfig>>

    @Query("SELECT * FROM qinglong_configs ORDER BY id ASC")
    suspend fun getAllConfigsList(): List<QinglongConfig>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: QinglongConfig): Long

    @Update
    suspend fun updateConfig(config: QinglongConfig)

    @Delete
    suspend fun deleteConfig(config: QinglongConfig)
}
