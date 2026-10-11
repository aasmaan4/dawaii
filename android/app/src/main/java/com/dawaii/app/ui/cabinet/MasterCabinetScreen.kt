package com.dawaii.app.ui.cabinet

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dawaii.app.data.preferences.AppPreferences
import com.dawaii.app.data.local.entity.Doctor
import com.dawaii.app.data.local.entity.Medicine
import com.dawaii.app.data.local.entity.MedicineType
import com.dawaii.app.ui.components.AnimatedEditModal
import com.dawaii.app.ui.components.NativeCameraCaptureModal
import com.dawaii.app.ui.theme.*

/**
 * Screen 4: Master Medicine Cabinet
 * Global repository of ALL medicines added across all doctors with photo cards and animated edit modal.
 */
@Composable
fun MasterCabinetScreen(
    medicines: List<Medicine>,
    doctors: List<Doctor>,
    onSaveMedicine: (Medicine) -> Unit,
    onDeleteMedicine: (Medicine) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences.getInstance(context) }
    var medicineToEdit by remember { mutableStateOf<Medicine?>(null) }
    var medicinePendingDeletion by remember { mutableStateOf<Medicine?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MASTER MEDICINE CABINET",
                    style = MaterialTheme.typography.titleMedium,
                    color = BlushPinkPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "All medications across all doctors",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGreyPink
                )
            }

            IconButton(
                onClick = { isAddingNew = true },
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BlushPinkContainer)
                    .border(1.dp, BlushPinkPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add Medicine", tint = BlushPinkPrimary, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (medicines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Your master cabinet is empty.\nAdd your first medication with '+' above.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGreyPink,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(medicines, key = { it.id }) { med ->
                    val docName = doctors.find { it.id == med.doctorId }?.name ?: "Doctor"
                    MasterMedicineListItem(
                        medicine = med,
                        doctorName = docName,
                        onEdit = { medicineToEdit = med },
                        onDelete = { medicinePendingDeletion = med }
                    )
                }
            }
        }
    }

    val isModalOpen = isAddingNew || medicineToEdit != null
    val editingMed = medicineToEdit

    medicinePendingDeletion?.let { med ->
        AlertDialog(
            onDismissRequest = { medicinePendingDeletion = null },
            title = {
                Text(
                    text = "Remove Medicine?",
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
                        medicinePendingDeletion = null
                    }
                ) {
                    Text("Remove", color = Color(0xFFE55353), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { medicinePendingDeletion = null }) {
                    Text("Cancel", color = MutedGreyPink)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    var name by remember(editingMed, isAddingNew) { mutableStateOf(editingMed?.name ?: "") }
    var dosage by remember(editingMed, isAddingNew) { mutableStateOf(editingMed?.dosage ?: "") }
    var selectedDocId by remember(editingMed, isAddingNew) { mutableStateOf(editingMed?.doctorId ?: doctors.firstOrNull()?.id ?: 1L) }
    var type by remember(editingMed, isAddingNew) { mutableStateOf(editingMed?.type ?: MedicineType.PILL) }
    var pillsLeft by remember(editingMed, isAddingNew) { mutableStateOf(editingMed?.pillsLeft?.toString() ?: "30") }
    var daysLeft by remember(editingMed, isAddingNew) { mutableStateOf(editingMed?.daysToComplete?.toString() ?: "14") }
    var photoUri by remember(editingMed, isAddingNew) { mutableStateOf(editingMed?.photoUri) }
    var scheduledHour by remember(editingMed, isAddingNew) { mutableStateOf(editingMed?.scheduledHour ?: 20) }
    var scheduledMinute by remember(editingMed, isAddingNew) { mutableStateOf(editingMed?.scheduledMinute ?: 0) }
    var isReminderEnabled by remember(editingMed, isAddingNew) {
        mutableStateOf(editingMed?.let { prefs.isMedicineReminderEnabled(it.id) } ?: true)
    }

    var showCameraViewfinder by remember { mutableStateOf(false) }

    NativeCameraCaptureModal(
        isVisible = showCameraViewfinder,
        onDismiss = { showCameraViewfinder = false },
        onPhotoCaptured = { uri ->
            photoUri = uri
            showCameraViewfinder = false
        }
    )

    AnimatedEditModal(
        isVisible = isModalOpen,
        onDismissRequest = {
            isAddingNew = false
            medicineToEdit = null
        },
        title = if (editingMed == null) "Add to Cabinet" else "Edit Medication",
        subtitle = "Master Cabinet Registry",
        isSaveEnabled = name.isNotBlank() && dosage.isNotBlank(),
        onSave = {
            val medId = editingMed?.id ?: System.currentTimeMillis()
            prefs.setMedicineReminderEnabled(medId, isReminderEnabled)
            val period = if (scheduledHour >= 12) "PM" else "AM"
            val h12 = if (scheduledHour % 12 == 0) 12 else scheduledHour % 12
            val mFormatted = String.format(java.util.Locale.getDefault(), "%02d", scheduledMinute)
            val freq = "Daily at $h12:$mFormatted $period"

            onSaveMedicine(
                Medicine(
                    id = medId,
                    doctorId = selectedDocId,
                    name = name,
                    dosage = dosage,
                    frequency = freq,
                    type = type,
                    pillsLeft = if (type.isPillCountable) pillsLeft.toIntOrNull() else null,
                    daysToComplete = daysLeft.toIntOrNull() ?: 14,
                    photoUri = photoUri,
                    scheduledHour = scheduledHour,
                    scheduledMinute = scheduledMinute
                )
            )
            isAddingNew = false
            medicineToEdit = null
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
                label = { Text("Dosage") },
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

            // Doctor dropdown assignment
            Text("Assigned Doctor", style = MaterialTheme.typography.labelSmall, color = MutedGreyPink)
            var docDropdownExpanded by remember { mutableStateOf(false) }
            val selectedDoc = doctors.find { it.id == selectedDocId }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                    .clickable { docDropdownExpanded = true }
                    .padding(12.dp)
            ) {
                Text(selectedDoc?.name ?: "Select Doctor", color = SoftWhite)
                DropdownMenu(
                    expanded = docDropdownExpanded,
                    onDismissRequest = { docDropdownExpanded = false },
                    modifier = Modifier.background(DarkSurface)
                ) {
                    doctors.forEach { doc ->
                        DropdownMenuItem(
                            text = { Text(doc.name, color = if (doc.id == selectedDocId) BlushPinkPrimary else SoftWhite) },
                            onClick = {
                                selectedDocId = doc.id
                                docDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Type selector
            Text("Medicine Format", style = MaterialTheme.typography.labelSmall, color = MutedGreyPink)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MedicineType.values().forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(t.displayName, fontSize = 11.sp) }
                    )
                }
            }

            // Conditional Inventory field
            AnimatedVisibility(visible = type.isPillCountable) {
                OutlinedTextField(
                    value = pillsLeft,
                    onValueChange = { pillsLeft = it },
                    label = { Text("Pills Left in Bottle") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Native In-App Camera Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (photoUri != null) "In-App Photo Attached ✓" else "Bottle Photo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (photoUri != null) BlushPinkPrimary else MutedGreyPink
                    )
                    Text(
                        text = if (photoUri != null) "Saved to internal storage" else "Optional visual identifier",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedGreyPink.copy(alpha = 0.7f)
                    )
                }
                Button(
                    onClick = { showCameraViewfinder = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                ) {
                    Icon(
                        Icons.Outlined.PhotoCamera,
                        contentDescription = null,
                        tint = BlushPinkPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (photoUri == null) "Take Photo" else "Retake",
                        fontSize = 12.sp,
                        color = SoftWhite
                    )
                }
            }
        }
    }

@Composable
fun MasterMedicineListItem(
    medicine: Medicine,
    doctorName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(DarkBorder)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo Area or Minimal Serif Fallback Box
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!medicine.photoUri.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = medicine.photoUri,
                        contentDescription = medicine.name,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = medicine.name.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = BirchTypography),
                        color = BlushPinkPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medicine.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = SoftWhite,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${medicine.type.displayName} • ${medicine.dosage}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGreyPink,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$doctorName • ${medicine.frequency}",
                    style = MaterialTheme.typography.labelSmall,
                    color = BlushPinkPrimary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Pills left or Days left chip if applicable
            medicine.pillsLeft?.let { pills ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$pills left",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pills <= 5) BlushPinkPrimary else MutedGreyPink
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Delete Icon Button
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MutedGreyPink,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
