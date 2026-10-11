package com.dawaii.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "appointments",
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
data class Appointment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val doctorId: Long,
    val date: String,
    val time: String,
    val location: String,
    val notes: String = "",
    @ColumnInfo(name = "is_attended")
    val isAttended: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
