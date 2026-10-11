package com.dawaii.app.ui.medicines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dawaii.app.data.local.entity.Doctor
import com.dawaii.app.data.local.entity.Medicine
import com.dawaii.app.data.local.entity.MedicineType
import com.dawaii.app.ui.components.AnimatedEditModal
import com.dawaii.app.ui.theme.*

@Composable
fun MyMedicinesScreen(
    viewModel: MedicinesViewModel = viewModel(),
    onOpenDoctorTabs: ((Doctor) -> Unit)? = null
) {
    val doctors by viewModel.doctors.collectAsState()
    val medicines by viewModel.medicines.collectAsState()
    val selectedDoctorId by viewModel.selectedDoctorId.collectAsState()

    val showDoctorModal by viewModel.showDoctorModal.collectAsState()
    val editingDoctor by viewModel.editingDoctor.collectAsState()

    val showMedicineModal by viewModel.showMedicineModal.collectAsState()
    val editingMedicine by viewModel.editingMedicine.collectAsState()

    val currentDoctor = doctors.find { it.id == selectedDoctorId }
    val currentDoctorMedicines = medicines.filter { it.doctorId == selectedDoctorId }

    var doctorToDelete by remember { mutableStateOf<Doctor?>(null) }
    var medicineToDelete by remember { mutableStateOf<Medicine?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- 1. Doctor-First Header with Aligned "+" Add Icon ---
        item {
            SectionHeaderWithAdd(
                title = "YOUR DOCTORS",
                onAddClick = { viewModel.onAddDoctorClicked() }
            )
        }

        if (doctors.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(DarkBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No doctors added yet. Tap '+' above to add your first prescribing doctor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedGreyPink,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            // Doctor Cards List
            items(doctors, key = { "doc_${it.id}_${it.name}" }) { doctor ->
                val isSelected = doctor.id == selectedDoctorId
                DoctorItemCard(
                    doctor = doctor,
                    isSelected = isSelected,
                    medicineCount = medicines.count { it.doctorId == doctor.id },
                    onSelect = {
                        viewModel.onDoctorSelected(doctor.id)
                        onOpenDoctorTabs?.invoke(doctor)
                    },
                    onEdit = { viewModel.onEditDoctorClicked(doctor) },
                    onDelete = { doctorToDelete = doctor }
                )
            }
        }

        // --- 2. Medicine Cabinet for Selected Doctor with Aligned "+" Add Icon ---
        if (currentDoctor != null) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                SectionHeaderWithAdd(
                    title = "YOUR MEDICINE CABINET",
                    subtitle = "Prescriptions from ${currentDoctor.name}",
                    onAddClick = { viewModel.onAddMedicineClicked() }
                )
            }

            if (currentDoctorMedicines.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(DarkBorder)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No medicines added under this doctor yet. Tap '+' above to add one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedGreyPink,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(currentDoctorMedicines, key = { "med_${it.id}_${it.name}" }) { med ->
                    MedicineItemCard(
                        medicine = med,
                        onEdit = { viewModel.onEditMedicineClicked(med) },
                        onDelete = { medicineToDelete = med }
                    )
                }
            }
        }
    }

    // Confirmation Dialogs
    doctorToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { doctorToDelete = null },
            title = {
                Text(
                    text = "Remove Doctor?",
                    fontFamily = BirchTypography,
                    color = BlushPinkPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove ${doc.name}? All linked prescriptions will also be removed.",
                    color = SoftWhite
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onDeleteDoctor(doc)
                        doctorToDelete = null
                    }
                ) {
                    Text("Remove", color = Color(0xFFE55353), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { doctorToDelete = null }) {
                    Text("Cancel", color = MutedGreyPink)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    medicineToDelete?.let { med ->
        AlertDialog(
            onDismissRequest = { medicineToDelete = null },
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
                        viewModel.onDeleteMedicine(med)
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

    // --- Hoisted Animated Modals (Scale-Up + Fade-In) ---
    DoctorAnimatedModal(
        isVisible = showDoctorModal,
        doctor = editingDoctor,
        onDismiss = { viewModel.onDismissDoctorModal() },
        onSave = { name, contact, specialty ->
            viewModel.onSaveDoctor(name, contact, specialty)
        }
    )

    MedicineAnimatedModal(
        isVisible = showMedicineModal,
        medicine = editingMedicine,
        defaultDoctorId = selectedDoctorId ?: 1L,
        onDismiss = { viewModel.onDismissMedicineModal() },
        onSave = { updatedMedicine ->
            viewModel.onSaveMedicine(updatedMedicine)
        }
    )
}

// --- Universal Header with Minimalist Aligned "+" Add Icon ---
@Composable
fun SectionHeaderWithAdd(
    title: String,
    subtitle: String? = null,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = BlushPinkPrimary,
                letterSpacing = 1.sp
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGreyPink
                )
            }
        }

        IconButton(
            onClick = onAddClick,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(BlushPinkContainer)
                .border(1.dp, BlushPinkPrimary.copy(alpha = 0.45f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Add Item",
                tint = BlushPinkPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// --- Doctor Item Card with Selection, Edit (Pencil), and Delete (Dustbin) ---
@Composable
fun DoctorItemCard(
    doctor: Doctor,
    isSelected: Boolean,
    medicineCount: Int,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BlushPinkContainer.copy(alpha = 0.6f) else DarkSurface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(if (isSelected) BlushPinkPrimary else DarkBorder)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) BlushPinkPrimary else DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = if (isSelected) DarkCharcoalBg else BlushPinkPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = doctor.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isSelected) BlushPinkPrimary else SoftWhite
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${doctor.specialty} • ${doctor.contactNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedGreyPink
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$medicineCount Active Prescriptions",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) BlushPinkPrimary else MutedGreyPink
                    )
                }
            }

            // Edit and Delete buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit Doctor",
                        tint = BlushPinkPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete Doctor",
                        tint = MutedGreyPink,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

// --- Medicine Item Card with Conditional Pill Tag and Delete (Tap anywhere to edit) ---
@Composable
fun MedicineItemCard(
    medicine: Medicine,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(DarkBorder)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = medicine.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = SoftWhite
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BlushPinkContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = medicine.type.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = BlushPinkPrimary,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${medicine.dosage} • ${medicine.frequency}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGreyPink
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (medicine.type.isPillCountable && medicine.pillsLeft != null) {
                        Text(
                            text = "${medicine.pillsLeft} pills left",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (medicine.pillsLeft <= 5) BlushPinkPrimary else SoftWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "${medicine.daysToComplete} days course left",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedGreyPink
                    )
                }
            }

            // Delete button (Edit is triggered by tapping anywhere on the medicine card)
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete Medicine",
                    tint = MutedGreyPink,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

// --- Animated Doctor Edit / Add Modal using AnimatedEditModal ---
@Composable
fun DoctorAnimatedModal(
    isVisible: Boolean,
    doctor: Doctor?,
    onDismiss: () -> Unit,
    onSave: (name: String, contact: String, specialty: String) -> Unit
) {
    var name by remember(doctor) { mutableStateOf(doctor?.name ?: "") }
    var contact by remember(doctor) { mutableStateOf(doctor?.contactNumber ?: "") }
    var specialty by remember(doctor) { mutableStateOf(doctor?.specialty ?: "Cardiologist") }

    AnimatedEditModal(
        isVisible = isVisible,
        onDismissRequest = onDismiss,
        title = if (doctor == null) "Add New Doctor" else "Edit Doctor",
        subtitle = "Fill in doctor details below",
        saveButtonText = if (doctor == null) "Add Doctor" else "Done",
        isSaveEnabled = name.isNotBlank(),
        onSave = { onSave(name, contact, specialty) }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Doctor Name") },
            placeholder = { Text("e.g. Dr. Rajesh Sharma") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BlushPinkPrimary,
                unfocusedBorderColor = DarkBorder,
                focusedLabelColor = BlushPinkPrimary,
                focusedTextColor = SoftWhite,
                unfocusedTextColor = SoftWhite
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = contact,
            onValueChange = { contact = it },
            label = { Text("Contact Number") },
            placeholder = { Text("+1 (555) 000-0000") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BlushPinkPrimary,
                unfocusedBorderColor = DarkBorder,
                focusedLabelColor = BlushPinkPrimary,
                focusedTextColor = SoftWhite,
                unfocusedTextColor = SoftWhite
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = specialty,
            onValueChange = { specialty = it },
            label = { Text("Specialty") },
            placeholder = { Text("e.g. Cardiologist") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BlushPinkPrimary,
                unfocusedBorderColor = DarkBorder,
                focusedLabelColor = BlushPinkPrimary,
                focusedTextColor = SoftWhite,
                unfocusedTextColor = SoftWhite
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// --- Animated Medicine Edit / Add Modal using AnimatedEditModal ---
@Composable
fun MedicineAnimatedModal(
    isVisible: Boolean,
    medicine: Medicine?,
    defaultDoctorId: Long,
    onDismiss: () -> Unit,
    onSave: (Medicine) -> Unit
) {
    var name by remember(medicine) { mutableStateOf(medicine?.name ?: "") }
    var dosage by remember(medicine) { mutableStateOf(medicine?.dosage ?: "") }
    var frequency by remember(medicine) { mutableStateOf(medicine?.frequency ?: "Daily at 8:00 AM") }
    var type by remember(medicine) { mutableStateOf(medicine?.type ?: MedicineType.PILL) }
    var pillsLeftStr by remember(medicine) { mutableStateOf(medicine?.pillsLeft?.toString() ?: "30") }
    var daysStr by remember(medicine) { mutableStateOf(medicine?.daysToComplete?.toString() ?: "14") }

    AnimatedEditModal(
        isVisible = isVisible,
        onDismissRequest = onDismiss,
        title = if (medicine == null) "Add New Medicine" else "Edit Medicine",
        subtitle = "Enter medication details",
        saveButtonText = if (medicine == null) "Add Medicine" else "Done",
        isSaveEnabled = name.isNotBlank() && dosage.isNotBlank(),
        onSave = {
            val pillsLeft = if (type.isPillCountable) pillsLeftStr.toIntOrNull() else null
            val days = daysStr.toIntOrNull() ?: 14
            val result = medicine?.copy(
                name = name,
                dosage = dosage,
                frequency = frequency,
                type = type,
                pillsLeft = pillsLeft,
                daysToComplete = days
            ) ?: Medicine(
                doctorId = defaultDoctorId,
                name = name,
                dosage = dosage,
                frequency = frequency,
                type = type,
                pillsLeft = pillsLeft,
                daysToComplete = days
            )
            onSave(result)
        }
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Medicine Name") },
            placeholder = { Text("e.g. Blood Pressure") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BlushPinkPrimary,
                unfocusedBorderColor = DarkBorder,
                focusedLabelColor = BlushPinkPrimary,
                focusedTextColor = SoftWhite,
                unfocusedTextColor = SoftWhite
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = dosage,
            onValueChange = { dosage = it },
            label = { Text("Dosage") },
            placeholder = { Text("e.g. 10mg / 1 Tablet") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BlushPinkPrimary,
                unfocusedBorderColor = DarkBorder,
                focusedLabelColor = BlushPinkPrimary,
                focusedTextColor = SoftWhite,
                unfocusedTextColor = SoftWhite
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = frequency,
            onValueChange = { frequency = it },
            label = { Text("Frequency & Schedule") },
            placeholder = { Text("e.g. Daily at 8:00 AM") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BlushPinkPrimary,
                unfocusedBorderColor = DarkBorder,
                focusedLabelColor = BlushPinkPrimary,
                focusedTextColor = SoftWhite,
                unfocusedTextColor = SoftWhite
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Type Selector Chips
        Text(
            text = "MEDICINE TYPE",
            style = MaterialTheme.typography.labelSmall,
            color = MutedGreyPink
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MedicineType.values().forEach { medType ->
                val isSelected = type == medType
                FilterChip(
                    selected = isSelected,
                    onClick = { type = medType },
                    label = { Text(medType.name, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BlushPinkPrimary,
                        selectedLabelColor = DarkCharcoalBg,
                        containerColor = DarkSurfaceVariant,
                        labelColor = SoftWhite
                    )
                )
            }
        }

        // Conditional Pills Left (Only shown for Capsule or Pill)
        if (type.isPillCountable) {
            OutlinedTextField(
                value = pillsLeftStr,
                onValueChange = { pillsLeftStr = it },
                label = { Text("Pills Left in Inventory") },
                placeholder = { Text("e.g. 30") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BlushPinkPrimary,
                    unfocusedBorderColor = DarkBorder,
                    focusedLabelColor = BlushPinkPrimary,
                    focusedTextColor = SoftWhite,
                    unfocusedTextColor = SoftWhite
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = daysStr,
            onValueChange = { daysStr = it },
            label = { Text("Days to Complete Course") },
            placeholder = { Text("e.g. 14") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BlushPinkPrimary,
                unfocusedBorderColor = DarkBorder,
                focusedLabelColor = BlushPinkPrimary,
                focusedTextColor = SoftWhite,
                unfocusedTextColor = SoftWhite
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
