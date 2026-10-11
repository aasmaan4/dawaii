package com.dawaii.app.data.local

import androidx.room.TypeConverter
import com.dawaii.app.data.local.entity.DoseStatus
import com.dawaii.app.data.local.entity.MedicineType
import java.time.LocalDate
import java.util.Date

class Converters {
    @TypeConverter
    fun fromDoseStatus(status: DoseStatus): String = status.name

    @TypeConverter
    fun toDoseStatus(value: String): DoseStatus = runCatching {
        DoseStatus.valueOf(value)
    }.getOrDefault(DoseStatus.PENDING)

    @TypeConverter
    fun fromMedicineType(type: MedicineType): String = type.name

    @TypeConverter
    fun toMedicineType(value: String): MedicineType = runCatching {
        MedicineType.valueOf(value)
    }.getOrDefault(MedicineType.PILL)

    // Strict LocalDate TypeConverter (ISO-8601 string: YYYY-MM-DD)
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let {
        runCatching { LocalDate.parse(it) }.getOrNull()
    }

    // Strict Unix Timestamp TypeConverter
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}

