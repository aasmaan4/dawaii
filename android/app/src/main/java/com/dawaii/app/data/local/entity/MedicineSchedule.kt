package com.dawaii.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "medicine_schedule",
    foreignKeys = [
        ForeignKey(
            entity = Medicine::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["medicineId"]),
        Index(value = ["date"])
    ]
)
data class MedicineSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "medicineId")
    val medicineId: Long,
    @ColumnInfo(name = "medicine_name")
    val medicineName: String,
    @ColumnInfo(name = "dosage")
    val dosage: String,
    @ColumnInfo(name = "scheduled_hour")
    val scheduledHour: Int,
    @ColumnInfo(name = "scheduled_minute")
    val scheduledMinute: Int,
    @ColumnInfo(name = "date")
    val date: LocalDate,
    @ColumnInfo(name = "is_taken")
    val isTaken: Boolean = false,
    @ColumnInfo(name = "status")
    val status: DoseStatus = DoseStatus.PENDING
)
