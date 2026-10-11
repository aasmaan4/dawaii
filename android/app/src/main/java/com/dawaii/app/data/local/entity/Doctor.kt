package com.dawaii.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "doctors")
data class Doctor(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "contact_number")
    val contactNumber: String,
    val specialty: String = "General Physician",
    val clinicAddress: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
