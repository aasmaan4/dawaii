package com.dawaii.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Normalized relation combining a DailyMedicationLog with its Medicine and Doctor.
 * When a Doctor's name or a Medicine's dosage is updated, Room's reactive Flow
 * immediately emits updated instances to all observers (Dashboard, Calendar, Cabinet).
 */
data class LogWithMedicineAndDoctor(
    @Embedded
    val log: DailyMedicationLog,
    @Relation(
        entity = Medicine::class,
        parentColumn = "medicine_id",
        entityColumn = "id"
    )
    val medicineWithDoctor: MedicineWithDoctor
)
