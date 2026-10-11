package com.dawaii.app.data.local.dao

import androidx.room.*
import com.dawaii.app.data.local.entity.Doctor
import com.dawaii.app.data.local.entity.DoctorWithMedicines
import kotlinx.coroutines.flow.Flow

@Dao
interface DoctorDao {
    @Query("SELECT * FROM doctors ORDER BY name ASC")
    fun getAllDoctors(): Flow<List<Doctor>>

    @Query("SELECT * FROM doctors WHERE id = :id LIMIT 1")
    suspend fun getDoctorById(id: Long): Doctor?

    @Transaction
    @Query("SELECT * FROM doctors WHERE id = :doctorId")
    fun getDoctorWithMedicines(doctorId: Long): Flow<DoctorWithMedicines?>

    @Transaction
    @Query("SELECT * FROM doctors ORDER BY name ASC")
    fun getAllDoctorsWithMedicines(): Flow<List<DoctorWithMedicines>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctor(doctor: Doctor): Long

    @Update
    suspend fun updateDoctor(doctor: Doctor)

    @Delete
    suspend fun deleteDoctor(doctor: Doctor)
}
