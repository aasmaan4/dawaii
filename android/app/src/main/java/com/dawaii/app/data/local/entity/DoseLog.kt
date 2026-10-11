package com.dawaii.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dose_logs",
    foreignKeys = [
        ForeignKey(
            entity = Medicine::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["medicationId"]),
        Index(value = ["scheduledTimeMillis"])
    ]
)
data class DoseLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val scheduledTimeMillis: Long,
    val actualTriggerTimeMillis: Long,
    val actionTimeMillis: Long? = null,
    val status: DoseStatus = DoseStatus.PENDING,
    val snoozeMinutes: Int = 0,
    val triggerType: String = "STANDARD" // "STANDARD", "NIGHT_SHIFT", "SNOOZED"
)
