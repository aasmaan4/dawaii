package com.dawaii.app.ui.medicines

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.dawaii.app.data.preferences.AppPreferences
import com.dawaii.app.data.local.entity.Doctor
import com.dawaii.app.data.local.entity.Medicine
import com.dawaii.app.data.local.entity.MedicineType
import com.dawaii.app.ui.components.AnimatedEditModal
import com.dawaii.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Screen 3: User Flow - Doctors -> Animated Medicine Tabs.
 * Tapping a Doctor opens this screen displaying an animated TabRow and HorizontalPager
 * for all medicines prescribed by this specific Doctor.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DoctorMedicinesTabScreen(
    doctor: Doctor,
    medicines: List<Medicine>,
    onBackClick: () -> Unit,
    onSaveMedicine: (Medicine) -> Unit,
    onDeleteMedicine: (Medicine) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    val doctorMedicines = remember(medicines, doctor.id) {
        medicines.filter { it.doctorId == doctor.id }
    }

    var selectedMedicineToEdit by remember { mutableStateOf<Medicine?>(null) }
    var isAddingNewMedicine by remember { mutableStateOf(false) }
    var medicineToDelete by remember { mutableStateOf<Medicine?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
    ) {
        // Top Bar with Doctor Info & Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = BlushPinkPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doctor.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = SoftWhite
                )
                Text(
                    text = "${doctor.specialty} • ${doctor.contactNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGreyPink
                )
            }

            IconButton(
                onClick = { isAddingNewMedicine = true },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BlushPinkContainer)
                    .border(1.dp, BlushPinkPrimary.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add Prescription",
                    tint = BlushPinkPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(color = DarkBorder)

        if (doctorMedicines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No prescriptions linked to ${doctor.name} yet.\nTap '+' above to add one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGreyPink,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            val pagerState = rememberPagerState(initialPage = 0, pageCount = { doctorMedicines.size })

            val safePage = pagerState.currentPage.coerceIn(0, (doctorMedicines.size - 1).coerceAtLeast(0))

            // Animated TabRow
            ScrollableTabRow(
                selectedTabIndex = safePage,
                containerColor = DarkSurface,
                contentColor = BlushPinkPrimary,
                edgePadding = 16.dp,
                divider = { HorizontalDivider(color = DarkBorder) },
                indicator = { tabPositions ->
                    if (safePage < tabPositions.size) {
                        TabRowDefaults.Indicator(
                            color = BlushPinkPrimary,
                            modifier = Modifier.tabIndicatorOffset(tabPositions[safePage])
                        )
                    }
                }
            ) {
                doctorMedicines.forEachIndexed { index, med ->
                    val isSelected = pagerState.currentPage == index
                    Tab(
                        selected = isSelected,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                text = med.name,
                                style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) BlushPinkPrimary else MutedGreyPink
                            )
                        }
                    )
                }
            }

            // Animated Horizontal Pager between medicines
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val currentMed = doctorMedicines.getOrNull(page)
                if (currentMed != null) {
                    MedicineDetailPagerItem(
                        medicine = currentMed,
                        doctor = doctor,
                        onEdit = { selectedMedicineToEdit = currentMed },
                        onDelete = { medicineToDelete = currentMed }
                    )
                }
            }
        }
    }

    medicineToDelete?.let { med ->
        AlertDialog(
            onDismissRequest = { medicineToDelete = null },
            title = {
                Text(
                    text = "Remove Prescription?",
                    fontFamily = BirchTypography,
                    color = BlushPinkPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove ${med.name}?",
                    color = SoftWhite
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        prefs.recordMedicineDeleted(med.id, med.name)
                        onDeleteMedicine(med)
                        medicineToDelete = null
                    }
                ) {
                    Text("Remove", color = Color(0xFFE55353), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { medicineToDelete = null }) {
                    Text("Cancel", color = MutedGreyPink)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    val isModalOpen = isAddingNewMedicine || selectedMedicineToEdit != null
    val editingMed = selectedMedicineToEdit

    var name by remember(editingMed, isAddingNewMedicine) { mutableStateOf(editingMed?.name ?: "") }
    var dosage by remember(editingMed, isAddingNewMedicine) { mutableStateOf(editingMed?.dosage ?: "") }
    var frequency by remember(editingMed, isAddingNewMedicine) { mutableStateOf(editingMed?.frequency ?: "") }
    var type by remember(editingMed, isAddingNewMedicine) { mutableStateOf(editingMed?.type ?: MedicineType.PILL) }
    var pillsLeft by remember(editingMed, isAddingNewMedicine) { mutableStateOf(editingMed?.pillsLeft?.toString() ?: "30") }
    var daysLeft by remember(editingMed, isAddingNewMedicine) { mutableStateOf(editingMed?.daysToComplete?.toString() ?: "14") }
    var scheduledHour by remember(editingMed, isAddingNewMedicine) { mutableStateOf(editingMed?.scheduledHour ?: 20) }
    var scheduledMinute by remember(editingMed, isAddingNewMedicine) { mutableStateOf(editingMed?.scheduledMinute ?: 0) }
    var isReminderEnabled by remember(editingMed, isAddingNewMedicine) {
        mutableStateOf(editingMed?.let { prefs.isMedicineReminderEnabled(it.id) } ?: true)
    }

    AnimatedEditModal(
        isVisible = isModalOpen,
        onDismissRequest = {
            isAddingNewMedicine = false
            selectedMedicineToEdit = null
        },
        title = if (editingMed == null) "New Prescription" else "Edit Prescription",
        subtitle = "Prescribed by ${doctor.name}",
        isSaveEnabled = name.isNotBlank() && dosage.isNotBlank(),
        onSave = {
            val medId = editingMed?.id ?: 0L
            if (medId != 0L) {
                prefs.setMedicineReminderEnabled(medId, isReminderEnabled)
            }
            val period = if (scheduledHour >= 12) "PM" else "AM"
            val h12 = if (scheduledHour % 12 == 0) 12 else scheduledHour % 12
            val mFormatted = String.format(java.util.Locale.getDefault(), "%02d", scheduledMinute)
            val autoFreq = if (frequency.isNotBlank()) frequency else "Daily at $h12:$mFormatted $period"

            onSaveMedicine(
                Medicine(
                    id = medId,
                    doctorId = doctor.id,
                    name = name,
                    dosage = dosage,
                    frequency = autoFreq,
                    type = type,
                    pillsLeft = if (type.isPillCountable) pillsLeft.toIntOrNull() else null,
                    daysToComplete = daysLeft.toIntOrNull() ?: 14,
                    photoUri = editingMed?.photoUri,
                    scheduledHour = scheduledHour,
                    scheduledMinute = scheduledMinute
                )
            )
            isAddingNewMedicine = false
            selectedMedicineToEdit = null
        }
    ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Medicine Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text("Dosage (e.g. 500mg, 1 Spoon)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Scheduled Dose Time Selection
            Text("Scheduled Dose Time (When to take)", style = MaterialTheme.typography.labelSmall, color = MutedGreyPink)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(8 to "8:00 AM", 13 to "1:00 PM", 20 to "8:00 PM", 21 to "9:30 PM", 22 to "10:00 PM").forEach { (h, label) ->
                    FilterChip(
                        selected = scheduledHour == h,
                        onClick = {
                            scheduledHour = h
                            scheduledMinute = if (h == 21) 30 else 0
                        },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }

            // Minute Precision (0-59 minutes) Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val period = if (scheduledHour >= 12) "PM" else "AM"
                val h12 = if (scheduledHour % 12 == 0) 12 else scheduledHour % 12
                val mFormatted = String.format(java.util.Locale.getDefault(), "%02d", scheduledMinute)
                Text(
                    text = "Scheduled: $h12:$mFormatted $period",
                    fontSize = 12.sp,
                    color = SoftWhite,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(
                    onClick = {
                        android.app.TimePickerDialog(
                            context,
                            { _, h, m ->
                                scheduledHour = h
                                scheduledMinute = m
                            },
                            scheduledHour,
                            scheduledMinute,
                            false
                        ).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Change (0-59 min)", fontSize = 11.sp, color = BlushPinkPrimary)
                }
            }

            // Send Lock-Screen Reminder Option
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Send Lock-Screen Reminder",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = SoftWhite
                        )
                        Text(
                            text = "Spawns DAWAII!! overlay at this dose time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGreyPink
                        )
                    }
                    Switch(
                        checked = isReminderEnabled,
                        onCheckedChange = { isReminderEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkCharcoalBg,
                            checkedTrackColor = BlushPinkPrimary
                        )
                    )
                }
            }

            Text("Format", style = MaterialTheme.typography.labelSmall, color = MutedGreyPink)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MedicineType.values().forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(t.displayName, fontSize = 11.sp) }
                    )
                }
            }

            AnimatedVisibility(visible = type.isPillCountable) {
                OutlinedTextField(
                    value = pillsLeft,
                    onValueChange = { pillsLeft = it },
                    label = { Text("Pills in Inventory") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

@Composable
fun MedicineDetailPagerItem(
    medicine: Medicine,
    doctor: Doctor,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(DarkBorder)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEdit() }
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (!medicine.photoUri.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = medicine.photoUri,
                            contentDescription = medicine.name,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = medicine.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = BlushPinkPrimary
                        )

                        // Delete button (Edit is triggered by tapping anywhere on the card)
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete", tint = MutedGreyPink)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Format: ${medicine.type.displayName}  •  ${medicine.dosage}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = SoftWhite
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    if (medicine.type.isPillCountable && medicine.pillsLeft != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pills Left in Bottle", style = MaterialTheme.typography.bodyMedium, color = MutedGreyPink)
                            Text("${medicine.pillsLeft} remaining", style = MaterialTheme.typography.titleMedium, color = BlushPinkPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Course Duration", style = MaterialTheme.typography.bodyMedium, color = MutedGreyPink)
                        Text("${medicine.daysToComplete} days total", style = MaterialTheme.typography.bodyMedium, color = SoftWhite)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Frequency", style = MaterialTheme.typography.bodyMedium, color = MutedGreyPink)
                        Text(medicine.frequency, style = MaterialTheme.typography.bodyMedium, color = SoftWhite)
                    }
                }
            }
        }
    }
}
