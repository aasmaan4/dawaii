package com.dawaii.app.ui.recommendations

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dawaii.app.data.local.AppDatabase
import com.dawaii.app.data.local.entity.Doctor
import com.dawaii.app.data.local.entity.Medicine
import com.dawaii.app.ui.theme.*

enum class RecFilter(val title: String) {
    ALL("All"),
    MORNING("🌅 Morning"),
    NIGHT("🌙 Night"),
    EMPTY_STOMACH("🥣 Empty Stomach"),
    AFTER_FOOD("🍽️ After Food")
}

data class MedicineScheduleGuidance(
    val timeCategory: String, // "morning", "noon", "night"
    val timeLabel: String,
    val mealBadge: String,
    val mealBadgeColor: Color,
    val instruction: String,
    val interactionWarning: String,
    val missedDoseRule: String
)

@Composable
fun RecommendationsScreen() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val medicinesWithDoctor by database.medicineDao().getAllMedicinesWithDoctor().collectAsState(initial = emptyList())
    var selectedFilter by remember { mutableStateOf(RecFilter.ALL) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "WHEN TO TAKE",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = BirchTypography,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            ),
            color = BlushPinkPrimary
        )
        Text(
            text = "Personalized clinical timing & food guidelines for each prescription",
            style = MaterialTheme.typography.bodySmall,
            color = MutedGreyPink
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Filter Pills Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(RecFilter.values()) { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) BlushPinkContainer else DarkSurfaceVariant)
                        .border(
                            1.dp,
                            if (isSelected) BlushPinkPrimary else DarkBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = filter.title,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = if (isSelected) BlushPinkPrimary else SoftWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Filtered list
        val filteredList = remember(medicinesWithDoctor, selectedFilter) {
            medicinesWithDoctor.filter { item ->
                val guidance = computeGuidance(item.medicine)
                when (selectedFilter) {
                    RecFilter.ALL -> true
                    RecFilter.MORNING -> guidance.timeCategory == "morning"
                    RecFilter.NIGHT -> guidance.timeCategory == "night"
                    RecFilter.EMPTY_STOMACH -> guidance.mealBadge.contains("Empty Stomach", ignoreCase = true)
                    RecFilter.AFTER_FOOD -> guidance.mealBadge.contains("After", ignoreCase = true) || guidance.mealBadge.contains("Meals", ignoreCase = true)
                }
            }
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No prescriptions found for this timing category.\nAdd prescriptions to your cabinet to see clinical guidance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGreyPink,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(filteredList, key = { it.medicine.id }) { item ->
                    RecommendationCard(medicine = item.medicine, doctor = item.doctor)
                }
            }
        }
    }
}

@Composable
fun RecommendationCard(medicine: Medicine, doctor: Doctor?) {
    val guidance = remember(medicine) { computeGuidance(medicine) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medicine.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = BodoniPosterFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = SoftWhite
                    )
                    Text(
                        text = "${medicine.type.displayName} • ${medicine.dosage}${if (doctor != null) " • " + doctor.name else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGreyPink
                    )
                }

                // Timing Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(BlushPinkContainer)
                        .border(1.dp, BlushPinkPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = guidance.timeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = BlushPinkPrimary
                    )
                }
            }

            // Clinical Instructions Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkCharcoalBg)
                    .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "WHEN & HOW TO TAKE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = BlushPinkPrimary
                    )
                    Text(
                        text = guidance.instruction,
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftWhite,
                        lineHeight = 18.sp
                    )
                }
            }

            // Meal Guideline Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(guidance.mealBadgeColor.copy(alpha = 0.15f))
                        .border(1.dp, guidance.mealBadgeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = guidance.mealBadge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = guidance.mealBadgeColor
                    )
                }
            }

            // Interaction & Missed Dose Cues
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFFFB86C),
                        modifier = Modifier.size(14.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = guidance.interactionWarning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGreyPink
                    )
                }

                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = BlushPinkPrimary,
                        modifier = Modifier.size(14.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = guidance.missedDoseRule,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGreyPink
                    )
                }
            }
        }
    }
}

fun computeGuidance(medicine: Medicine): MedicineScheduleGuidance {
    val name = medicine.name.lowercase()
    val hour = medicine.scheduledHour
    val minute = medicine.scheduledMinute
    val timeFormatted = String.format("%02d:%02d", hour, minute)

    val timeCategory = when {
        hour < 12 -> "morning"
        hour < 17 -> "noon"
        else -> "night"
    }

    val timeLabel = when (timeCategory) {
        "morning" -> "🌅 Morning ($timeFormatted)"
        "noon" -> "☀️ Afternoon ($timeFormatted)"
        else -> "🌙 Night ($timeFormatted)"
    }

    return when {
        name.contains("pan") || name.contains("pantoprazole") || name.contains("omeprazole") || name.contains("thyro") -> {
            MedicineScheduleGuidance(
                timeCategory = timeCategory,
                timeLabel = timeLabel,
                mealBadge = "🥣 Empty Stomach (AC)",
                mealBadgeColor = Color(0xFFFFB86C),
                instruction = "Take 30 to 60 minutes before morning breakfast with plain water. Swallow whole without crushing.",
                interactionWarning = "Wait at least 45 minutes before morning tea, coffee, milk, or calcium supplements.",
                missedDoseRule = "If missed before breakfast, take before lunch or evening meal on an empty stomach."
            )
        }
        name.contains("telmi") || name.contains("amlo") || name.contains("blood pressure") || name.contains("bp") -> {
            MedicineScheduleGuidance(
                timeCategory = timeCategory,
                timeLabel = timeLabel,
                mealBadge = "💧 Fixed Daily Time",
                mealBadgeColor = BlushPinkPrimary,
                instruction = "Take once daily every morning with water. Maintain consistent timing to keep blood pressure controlled.",
                interactionWarning = "Avoid high-potassium salt substitutes and excessive sodium.",
                missedDoseRule = "Take as soon as remembered unless it's past afternoon. Never double up doses."
            )
        }
        name.contains("beco") || name.contains("multi") || name.contains("zinc") || name.contains("vitamin") -> {
            MedicineScheduleGuidance(
                timeCategory = timeCategory,
                timeLabel = timeLabel,
                mealBadge = "🍽️ After Dinner (PC)",
                mealBadgeColor = Color(0xFF67D59A),
                instruction = "Take after dinner with water. Vitamin and mineral absorption is optimal following a meal.",
                interactionWarning = "Do not take on an empty stomach to avoid mild nausea.",
                missedDoseRule = "If forgotten at night, take after breakfast the next morning."
            )
        }
        name.contains("volini") || name.contains("gel") || name.contains("cream") -> {
            MedicineScheduleGuidance(
                timeCategory = timeCategory,
                timeLabel = timeLabel,
                mealBadge = "🧴 External Application",
                mealBadgeColor = Color(0xFFA29BFE),
                instruction = "Gently apply a thin layer over the affected area at bedtime. Wash hands thoroughly after use.",
                interactionWarning = "For external application only. Do not apply on broken or irritated skin.",
                missedDoseRule = "Apply when pain recurs, keeping at least 4-6 hours between applications."
            )
        }
        name.contains("amox") || name.contains("augmentin") || name.contains("antibiotic") -> {
            MedicineScheduleGuidance(
                timeCategory = timeCategory,
                timeLabel = timeLabel,
                mealBadge = "🍽️ With Meals (Course)",
                mealBadgeColor = Color(0xFF67D59A),
                instruction = "Take at the start of meals every 8 to 12 hours. Complete the entire course as prescribed.",
                interactionWarning = "Do not discontinue early even if symptoms improve.",
                missedDoseRule = "Take as soon as remembered, but skip if the next scheduled dose is within 2 hours."
            )
        }
        else -> {
            MedicineScheduleGuidance(
                timeCategory = timeCategory,
                timeLabel = timeLabel,
                mealBadge = "🍽️ After Food (PC)",
                mealBadgeColor = Color(0xFF67D59A),
                instruction = "Take with or after food with a glass of water. Follow exact dosage prescribed by your doctor.",
                interactionWarning = "Maintain regular spacing and consult your doctor before combining with new OTC drugs.",
                missedDoseRule = "Take as soon as remembered unless it is almost time for your next scheduled dose."
            )
        }
    }
}
