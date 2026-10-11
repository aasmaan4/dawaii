package com.dawaii.app.data.local.entity

enum class MedicineType(val displayName: String, val isPillCountable: Boolean) {
    CAPSULE("Capsule", true),
    PILL("Pill", true),
    CREAM("Cream", false),
    SOLUTION("Solution", false),
    SYRUP("Syrup", false)
}
