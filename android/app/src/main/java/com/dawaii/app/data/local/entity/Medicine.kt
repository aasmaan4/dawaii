package com.dawaii.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medicines",
    foreignKeys = [
        ForeignKey(
            entity = Doctor::class,
            parentColumns = ["id"],
            childColumns = ["doctorId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["doctorId"])
    ]
)
data class Medicine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val doctorId: Long,
    val name: String,
    val dosage: String,
    val frequency: String,
    val type: MedicineType = MedicineType.PILL,
    @ColumnInfo(name = "pills_left")
    val pillsLeft: Int? = null, // Conditional: Only tracked for CAPSULE and PILL
    @ColumnInfo(name = "days_to_complete")
    val daysToComplete: Int = 14,
    @ColumnInfo(name = "photo_uri")
    val photoUri: String? = null, // User-captured camera/gallery image URI
    val scheduledHour: Int = 20,
    val scheduledMinute: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
