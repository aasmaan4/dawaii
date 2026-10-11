package com.dawaii.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class DoctorWithMedicines(
    @Embedded val doctor: Doctor,
    @Relation(
        parentColumn = "id",
        entityColumn = "doctorId"
    )
    val medicines: List<Medicine>
)
