package com.dawaii.app.data.local.dao

import androidx.room.*
import com.dawaii.app.data.local.entity.DoseStatus
import com.dawaii.app.data.local.entity.MedicineSchedule
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MedicineScheduleDao {

    @Query("SELECT * FROM medicine_schedule WHERE date = :selectedDate ORDER BY scheduled_hour ASC, scheduled_minute ASC")
    fun getScheduleForDate(selectedDate: LocalDate): Flow<List<MedicineSchedule>>

    @Query("SELECT * FROM medicine_schedule WHERE date = :selectedDate ORDER BY scheduled_hour ASC, scheduled_minute ASC")
    suspend fun getScheduleForDateSync(selectedDate: LocalDate): List<MedicineSchedule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: MedicineSchedule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<MedicineSchedule>)

    @Update
    suspend fun updateSchedule(schedule: MedicineSchedule)

    @Query("UPDATE medicine_schedule SET is_taken = :isTaken, status = :status WHERE id = :id")
    suspend fun updateTakenStatus(id: Long, isTaken: Boolean, status: DoseStatus)

    @Query("SELECT COUNT(*) FROM medicine_schedule")
    suspend fun getScheduleCount(): Int
}
