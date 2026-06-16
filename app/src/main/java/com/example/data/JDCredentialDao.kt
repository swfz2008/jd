package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JDCredentialDao {
    @Query("SELECT * FROM jd_credentials ORDER BY id ASC")
    fun getAllCredentials(): Flow<List<JDCredential>>

    @Query("SELECT * FROM jd_credentials ORDER BY id ASC")
    suspend fun getAllCredentialsList(): List<JDCredential>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredential(credential: JDCredential): Long

    @Update
    suspend fun updateCredential(credential: JDCredential)

    @Delete
    suspend fun deleteCredential(credential: JDCredential)
}
