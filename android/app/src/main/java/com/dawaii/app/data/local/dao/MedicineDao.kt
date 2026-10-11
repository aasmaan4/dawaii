package com.dawaii.app.data.local.dao

import androidx.room.*
import com.dawaii.app.data.local.entity.Medicine
import com.dawaii.app.data.local.entity.MedicineWithDoctor
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines WHERE isActive = 1 ORDER BY scheduledHour ASC, scheduledMinute ASC")
    fun getAllActiveMedicines(): Flow<List<Medicine>>

    @Transaction
    @Query("SELECT * FROM medicines WHERE isActive = 1 ORDER BY scheduledHour ASC, scheduledMinute ASC")
    fun getAllMedicinesWithDoctor(): Flow<List<MedicineWithDoctor>>

    @Query("SELECT * FROM medicines WHERE doctorId = :doctorId ORDER BY scheduledHour ASC, scheduledMinute ASC")
    fun getMedicinesByDoctor(doctorId: Long): Flow<List<Medicine>>

    @Transaction
    @Query("SELECT * FROM medicines WHERE doctorId = :doctorId ORDER BY scheduledHour ASC, scheduledMinute ASC")
    fun getMedicinesWithDoctorByDoctor(doctorId: Long): Flow<List<MedicineWithDoctor>>

    @Query("SELECT * FROM medicines WHERE id = :id LIMIT 1")
    suspend fun getMedicineById(id: Long): Medicine?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: Medicine): Long

    @Update
    suspend fun updateMedicine(medicine: Medicine)

    @Delete
    suspend fun deleteMedicine(medicine: Medicine)

    @Query("UPDATE medicines SET pills_left = pills_left - 1 WHERE id = :id AND pills_left > 0")
    suspend fun decrementPills(id: Long)
}
