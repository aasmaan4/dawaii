package com.dawaii.app.data.local.dao

import androidx.room.*
import com.dawaii.app.data.local.entity.DailyMedicationLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyMedicationLogDao {
    @Query("SELECT * FROM daily_medication_logs WHERE date = :date")
    fun getLogsForDate(date: LocalDate): Flow<List<DailyMedicationLog>>

    @Transaction
    @Query("SELECT * FROM daily_medication_logs WHERE date = :date")
    fun getLogsWithDetailsForDate(date: LocalDate): Flow<List<com.dawaii.app.data.local.entity.LogWithMedicineAndDoctor>>

    @Query("SELECT * FROM daily_medication_logs WHERE date = :date AND medicine_id = :medicineId LIMIT 1")
    suspend fun getLogForMedicine(date: LocalDate, medicineId: Long): DailyMedicationLog?

    @Query("SELECT * FROM daily_medication_logs WHERE date BETWEEN :startDate AND :endDate")
    fun getLogsForRange(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyMedicationLog>>

    @Transaction
    @Query("SELECT * FROM daily_medication_logs WHERE date BETWEEN :startDate AND :endDate")
    fun getLogsWithDetailsForRange(startDate: LocalDate, endDate: LocalDate): Flow<List<com.dawaii.app.data.local.entity.LogWithMedicineAndDoctor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLog(log: DailyMedicationLog)

    @Query("UPDATE daily_medication_logs SET is_taken = :isTaken WHERE date = :date AND medicine_id = :medicineId")
    suspend fun updateTakenStatus(date: LocalDate, medicineId: Long, isTaken: Boolean): Int

    @Query("SELECT COUNT(*) FROM daily_medication_logs")
    suspend fun getLogCount(): Int
}
