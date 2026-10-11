package com.dawaii.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class AppointmentWithDoctor(
    @Embedded
    val appointment: Appointment,
    @Relation(
        parentColumn = "doctorId",
        entityColumn = "id"
    )
    val doctor: Doctor? = null
)
