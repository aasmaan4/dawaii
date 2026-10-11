package com.dawaii.app.data.local.dao

import androidx.room.*
import com.dawaii.app.data.local.entity.DoseLog
import com.dawaii.app.data.local.entity.DoseStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DoseLog): Long

    @Query("UPDATE dose_logs SET status = :status, actionTimeMillis = :timestamp WHERE id = :logId")
    suspend fun updateDoseStatus(logId: Long, status: DoseStatus, timestamp: Long)

    @Query("UPDATE dose_logs SET status = 'TAKEN', actionTimeMillis = :timestamp WHERE medicationId = :medicationId AND status = 'PENDING'")
    suspend fun markActiveDoseTaken(medicationId: Long, timestamp: Long)

    @Query("SELECT * FROM dose_logs WHERE status = 'PENDING' ORDER BY scheduledTimeMillis DESC LIMIT 1")
    suspend fun getLatestPendingDose(): DoseLog?

    @Query("SELECT * FROM dose_logs ORDER BY scheduledTimeMillis DESC")
    fun getAllLogs(): Flow<List<DoseLog>>

    @Query("SELECT * FROM dose_logs WHERE medicationId = :medicationId ORDER BY scheduledTimeMillis DESC")
    fun getLogsForMedication(medicationId: Long): Flow<List<DoseLog>>
}
