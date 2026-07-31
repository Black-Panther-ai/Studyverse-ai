package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.AiHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiHistoryDao {
    @Query("SELECT * FROM ai_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<AiHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: AiHistoryEntity)

    @Query("DELETE FROM ai_history")
    suspend fun clearHistory()
}
