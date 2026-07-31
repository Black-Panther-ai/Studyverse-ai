package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.ReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Delete
    suspend fun deleteReport(report: ReportEntity)
}
