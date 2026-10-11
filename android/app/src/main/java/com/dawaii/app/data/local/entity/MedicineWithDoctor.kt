package com.dawaii.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class MedicineWithDoctor(
    @Embedded
    val medicine: Medicine,
    @Relation(
        parentColumn = "doctorId",
        entityColumn = "id"
    )
    val doctor: Doctor? = null
)
