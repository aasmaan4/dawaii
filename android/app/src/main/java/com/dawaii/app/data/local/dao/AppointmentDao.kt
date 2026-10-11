package com.dawaii.app.data.local.dao

import androidx.room.*
import com.dawaii.app.data.local.entity.Appointment
import com.dawaii.app.data.local.entity.AppointmentWithDoctor
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Transaction
    @Query("SELECT * FROM appointments ORDER BY id ASC")
    fun getAllAppointmentsWithDoctor(): Flow<List<AppointmentWithDoctor>>

    @Transaction
    @Query("SELECT * FROM appointments WHERE is_attended = :isAttended ORDER BY id ASC")
    fun getAppointmentsByStatus(isAttended: Boolean): Flow<List<AppointmentWithDoctor>>

    @Transaction
    @Query("SELECT * FROM appointments WHERE is_attended = 0 ORDER BY id ASC LIMIT 1")
    fun getNextUpcomingAppointment(): Flow<AppointmentWithDoctor?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: Appointment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointments(appointments: List<Appointment>)

    @Update
    suspend fun updateAppointment(appointment: Appointment)

    @Delete
    suspend fun deleteAppointment(appointment: Appointment)

    @Query("UPDATE appointments SET is_attended = :isAttended WHERE id = :id")
    suspend fun updateAttendanceStatus(id: Long, isAttended: Boolean)

    @Query("UPDATE appointments SET date = :date, time = :time WHERE id = :id")
    suspend fun postponeAppointment(id: Long, date: String, time: String)

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deleteAppointmentById(id: Long)

    @Query("SELECT COUNT(*) FROM appointments")
    suspend fun getAppointmentCount(): Int
}
