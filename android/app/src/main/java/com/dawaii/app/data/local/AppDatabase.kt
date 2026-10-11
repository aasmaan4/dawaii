package com.dawaii.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dawaii.app.data.local.dao.AppointmentDao
import com.dawaii.app.data.local.dao.DailyMedicationLogDao
import com.dawaii.app.data.local.dao.DoctorDao
import com.dawaii.app.data.local.dao.DoseLogDao
import com.dawaii.app.data.local.dao.MedicineDao
import com.dawaii.app.data.local.dao.MedicineScheduleDao
import com.dawaii.app.data.local.entity.Appointment
import com.dawaii.app.data.local.entity.DailyMedicationLog
import com.dawaii.app.data.local.entity.Doctor
import com.dawaii.app.data.local.entity.DoseLog
import com.dawaii.app.data.local.entity.Medicine
import com.dawaii.app.data.local.entity.MedicineSchedule

@Database(
    entities = [
        Doctor::class,
        Medicine::class,
        Appointment::class,
        DoseLog::class,
        MedicineSchedule::class,
        DailyMedicationLog::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun doctorDao(): DoctorDao
    abstract fun medicineDao(): MedicineDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun doseLogDao(): DoseLogDao
    abstract fun medicineScheduleDao(): MedicineScheduleDao
    abstract fun dailyMedicationLogDao(): DailyMedicationLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dawaii_database.db"
                ).fallbackToDestructiveMigration()
                 .fallbackToDestructiveMigrationOnDowngrade()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
