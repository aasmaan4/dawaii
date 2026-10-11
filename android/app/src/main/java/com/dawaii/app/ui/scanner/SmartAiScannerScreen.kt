package com.dawaii.app.ui.scanner

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.dawaii.app.data.local.entity.Doctor
import com.dawaii.app.data.local.entity.Medicine
import com.dawaii.app.data.local.entity.MedicineType
import com.dawaii.app.data.preferences.AppPreferences
import com.dawaii.app.ui.components.AnimatedEditModal
import com.dawaii.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.dawaii.app.ui.components.NativeCameraCaptureView

/**
 * Screen 5: Smart AI Scanner (Direct Gemini Multimodal Vision Model)
 * Captures image directly and queries Gemini Vision model using user's Gemini API key.
 * No text recognition required.
 */
@Composable
fun SmartAiScannerScreen(
    doctors: List<Doctor>,
    onSaveMedicine: (Medicine) -> Unit,
    onCloseScanner: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    var isSimulatingOcr by remember { mutableStateOf(false) }
    var scannedExtractedResult by remember { mutableStateOf<ExtractedOcrData?>(null) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showAutoPopulatedModal by remember { mutableStateOf(false) }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf(prefs.geminiApiKey) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
    ) {
        // Native In-App Camera Viewfinder (CameraX + AndroidView)
        NativeCameraCaptureView(
            modifier = Modifier.fillMaxSize(),
            onPhotoCaptured = { savedUri ->
                isSimulatingOcr = true
                scanErrorMessage = null
                coroutineScope.launch {
                    try {
                        val result = com.dawaii.app.util.GeminiPrescriptionHelper.decipherPrescription(
                            context = context,
                            rawOcrText = null,
                            photoUri = savedUri,
                            apiKey = prefs.geminiApiKey
                        )
                        isSimulatingOcr = false
                        if (result.success && !result.name.isNullOrBlank()) {
                            scannedExtractedResult = ExtractedOcrData(
                                name = result.name,
                                dosage = result.dosage ?: "1 Dose Daily",
                                frequency = result.frequency ?: "Daily at 9:00 PM",
                                photoUri = savedUri,
                                type = result.type,
                                pillsLeft = (result.pillsLeft ?: 20).toString(),
                                reasoning = result.reasoning ?: "AI visual model identified ${result.name} (${result.type.displayName})."
                            )
                            showConfirmationDialog = true
                        } else {
                            scanErrorMessage = result.error ?: "Couldn't scan: Could not recognize tablet, bottle, or cream. Please ensure the item is well-lit and clearly centered."
                        }
                    } catch (e: Exception) {
                        isSimulatingOcr = false
                        scanErrorMessage = "Couldn't scan: ${e.localizedMessage ?: "Processing error"}"
                    }
                }
            },
            onClose = onCloseScanner
        )

        // Top Floating Chip: Gemini API Key Status / Config
        // Positioned TopCenter (top = 74.dp) to eliminate any overlap with the cross/close button in the top bar
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 74.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkSurface.copy(alpha = 0.90f),
                border = BorderStroke(1.dp, if (prefs.geminiApiKey.isNotBlank()) BlushPinkPrimary else MutedGreyPink),
                modifier = Modifier.clickable {
                    apiKeyInput = prefs.geminiApiKey
                    showApiKeyDialog = true
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Key,
                        contentDescription = "API Key",
                        tint = if (prefs.geminiApiKey.isNotBlank()) BlushPinkPrimary else SoftWhite,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (prefs.geminiApiKey.isNotBlank()) "Gemini Vision: Active ✓" else "Set Gemini API Key",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = SoftWhite
                    )
                }
            }
        }

        // Gemini API Key Input Dialog
        if (showApiKeyDialog) {
            AlertDialog(
                onDismissRequest = { showApiKeyDialog = false },
                title = {
                    Text(
                        text = "Gemini API Key",
                        style = MaterialTheme.typography.titleMedium,
                        color = BlushPinkPrimary
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter your Google Gemini API key to use your account's vision model for scanning prescriptions:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGreyPink
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            placeholder = { Text("AIzaSy...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            prefs.geminiApiKey = apiKeyInput.trim()
                            showApiKeyDialog = false
                        }
                    ) {
                        Text("Save Key", color = BlushPinkPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showApiKeyDialog = false }) {
                        Text("Cancel", color = MutedGreyPink)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Loading overlay if analyzing OCR / Gemini
        AnimatedVisibility(
            visible = isSimulatingOcr,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(DarkBorder))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = BlushPinkPrimary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "DECIPHERING WITH GEMINI AI...",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = BirchTypography,
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = BlushPinkPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Deciphering medication name, dosage & clinical schedule",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGreyPink
                        )
                    }
                }
            }
        }

        // Couldn't scan alert dialog
        if (scanErrorMessage != null) {
            AlertDialog(
                onDismissRequest = { scanErrorMessage = null },
                title = {
                    Text(
                        "Couldn't Scan Prescription",
                        style = MaterialTheme.typography.titleMedium,
                        color = BlushPinkPrimary
                    )
                },
                text = {
                    Text(
                        scanErrorMessage ?: "No prescription text detected.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftWhite
                    )
                },
                confirmButton = {
                    TextButton(onClick = { scanErrorMessage = null }) {
                        Text("Try Again", color = BlushPinkPrimary)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // AI Suggestion & Confirmation Dialog (Tablet, Bottle, Cream)
        if (showConfirmationDialog && scannedExtractedResult != null) {
            val res = scannedExtractedResult!!
            var selectedDocId by remember(res) { mutableStateOf(doctors.firstOrNull()?.id ?: 1L) }
            var docDropdownExpanded by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showConfirmationDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BlushPinkContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (res.type) {
                                    MedicineType.CREAM -> Icons.Outlined.Healing
                                    MedicineType.SYRUP -> Icons.Outlined.LocalDrink
                                    MedicineType.CAPSULE -> Icons.Outlined.Vaccines
                                    else -> Icons.Outlined.Medication
                                },
                                contentDescription = null,
                                tint = BlushPinkPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "AI MEDICINE SUGGESTION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = BlushPinkPrimary
                            )
                            Text(
                                text = "Detected: ${when (res.type) {
                                    MedicineType.CREAM -> "Cream / Ointment Tube"
                                    MedicineType.SYRUP -> "Bottle / Liquid Syrup"
                                    MedicineType.CAPSULE -> "Capsule"
                                    else -> "Tablet / Pill"
                                }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGreyPink
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Medicine Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DarkCharcoalBg,
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = res.name,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = BodoniPosterFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = SoftWhite
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = BlushPinkContainer
                                    ) {
                                        Text(
                                            text = res.dosage,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = BlushPinkPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(text = "•", color = MutedGreyPink)
                                    Text(
                                        text = res.type.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedGreyPink
                                    )
                                }
                            }
                        }

                        // AI Model Visual Insight Box
                        if (res.reasoning.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = BlushPinkPrimary,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Text(
                                    text = res.reasoning,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SoftWhite
                                )
                            }
                        }

                        // Prescribing Doctor Selector
                        if (doctors.isNotEmpty()) {
                            Column {
                                Text("Assign to Doctor:", style = MaterialTheme.typography.labelSmall, color = MutedGreyPink)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DarkSurfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { docDropdownExpanded = true }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        val curDoc = doctors.find { it.id == selectedDocId } ?: doctors.first()
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(curDoc.name, style = MaterialTheme.typography.bodySmall, color = SoftWhite)
                                            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = BlushPinkPrimary)
                                        }
                                    }
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
                            }
                        }

                        Text(
                            text = "Would you like to add this medicine to your cabinet?",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = SoftWhite
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newMedId = System.currentTimeMillis()
                            prefs.setMedicineReminderEnabled(newMedId, true)
                            onSaveMedicine(
                                Medicine(
                                    id = newMedId,
                                    doctorId = selectedDocId,
                                    name = res.name,
                                    dosage = res.dosage,
                                    frequency = res.frequency,
                                    type = res.type,
                                    pillsLeft = if (res.type.isPillCountable) res.pillsLeft.toIntOrNull() else null,
                                    photoUri = res.photoUri,
                                    scheduledHour = 20,
                                    scheduledMinute = 0
                                )
                            )
                            showConfirmationDialog = false
                            onCloseScanner()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BlushPinkPrimary,
                            contentColor = DarkCharcoalBg
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add to Cabinet", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                showConfirmationDialog = false
                                showAutoPopulatedModal = true
                            }
                        ) {
                            Text("Edit Details", color = BlushPinkPrimary)
                        }
                        TextButton(
                            onClick = {
                                showConfirmationDialog = false
                                scannedExtractedResult = null
                            }
                        ) {
                            Text("Retake", color = MutedGreyPink)
                        }
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(20.dp)
            )
        }

        val isModalOpen = showAutoPopulatedModal && scannedExtractedResult != null
        val data = scannedExtractedResult

        var name by remember(data) { mutableStateOf(data?.name ?: "") }
        var dosage by remember(data) { mutableStateOf(data?.dosage ?: "") }
        var frequency by remember(data) { mutableStateOf(data?.frequency ?: "") }
        var selectedDoctorId by remember(data) { mutableStateOf(doctors.firstOrNull()?.id ?: 1L) }
        var type by remember(data) { mutableStateOf(data?.type ?: MedicineType.CAPSULE) }
        var pillsLeft by remember(data) { mutableStateOf(data?.pillsLeft ?: "20") }
        var scheduledHour by remember(data) { mutableStateOf(20) }
        var scheduledMinute by remember(data) { mutableStateOf(0) }
        var isReminderEnabled by remember(data) { mutableStateOf(true) }

        AnimatedEditModal(
            isVisible = isModalOpen,
            onDismissRequest = { showAutoPopulatedModal = false },
            title = "Prescription Detected",
            subtitle = "Assign to a Doctor & Schedule",
            saveButtonText = "Save to Cabinet",
            isSaveEnabled = name.isNotBlank(),
            onSave = {
                val newMedId = System.currentTimeMillis()
                prefs.setMedicineReminderEnabled(newMedId, isReminderEnabled)
                val period = if (scheduledHour >= 12) "PM" else "AM"
                val h12 = if (scheduledHour % 12 == 0) 12 else scheduledHour % 12
                val mFormatted = String.format(java.util.Locale.getDefault(), "%02d", scheduledMinute)
                val autoFreq = if (frequency.isNotBlank()) frequency else "Daily at $h12:$mFormatted $period"

                onSaveMedicine(
                    Medicine(
                        id = newMedId,
                        doctorId = selectedDoctorId,
                        name = name,
                        dosage = dosage,
                        frequency = autoFreq,
                        type = type,
                        pillsLeft = if (type.isPillCountable) pillsLeft.toIntOrNull() else null,
                        photoUri = data?.photoUri,
                        scheduledHour = scheduledHour,
                        scheduledMinute = scheduledMinute
                    )
                )
                showAutoPopulatedModal = false
                onCloseScanner()
            }
        ) {
                Text(
                    text = "Prescription detected by Gemini Vision. Review details & set reminder time:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGreyPink
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Extracted Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Extracted Dosage") },
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

                // Crucial requirement: "Assign to which Doctor?" dropdown
                Text(
                    text = "Assign to which Doctor?",
                    style = MaterialTheme.typography.labelSmall,
                    color = BlushPinkPrimary
                )
                var docDropdownExpanded by remember { mutableStateOf(false) }
                val selectedDoctor = doctors.find { it.id == selectedDoctorId }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, BlushPinkPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable { docDropdownExpanded = true }
                        .padding(12.dp)
                ) {
                    Text(selectedDoctor?.name ?: "Select Doctor", color = SoftWhite)
                    DropdownMenu(
                        expanded = docDropdownExpanded,
                        onDismissRequest = { docDropdownExpanded = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        doctors.forEach { doc ->
                            DropdownMenuItem(
                                text = { Text(doc.name, color = if (doc.id == selectedDoctorId) BlushPinkPrimary else SoftWhite) },
                                onClick = {
                                    selectedDoctorId = doc.id
                                    docDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Photo preview status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = BlushPinkPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Captured bottle image saved to photo_uri", style = MaterialTheme.typography.labelSmall, color = SoftWhite)
                }
            }
        }
    }

data class ExtractedOcrData(
    val name: String,
    val dosage: String,
    val frequency: String,
    val photoUri: String,
    val type: MedicineType = MedicineType.PILL,
    val pillsLeft: String = "20",
    val reasoning: String = ""
)
