package com.dawaii.app.ui.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dawaii.app.data.local.entity.Appointment
import com.dawaii.app.data.local.entity.AppointmentWithDoctor
import com.dawaii.app.ui.components.AnimatedEditModal
import com.dawaii.app.ui.components.PostponeAppointmentModal
import com.dawaii.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    viewModel: AppointmentsViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Upcoming, 1 = Attended

    // Single Source of Truth: Reactive Room Flow collected as Compose State
    val appointmentsWithDoctor by viewModel.appointments.collectAsState()
    val doctors by viewModel.doctors.collectAsState()

    var appointmentToEdit by remember { mutableStateOf<AppointmentWithDoctor?>(null) }
    var appointmentToPostpone by remember { mutableStateOf<AppointmentWithDoctor?>(null) }
    var appointmentPendingDeletion by remember { mutableStateOf<AppointmentWithDoctor?>(null) }
    var isAddingAppointment by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkCharcoalBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddingAppointment = true },
                containerColor = BlushPinkPrimary,
                contentColor = DarkCharcoalBg,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add Appointment")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Doctor Visits",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = BirchTypography,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = BlushPinkPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Keep track of upcoming and completed visits",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGreyPink
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Minimal Custom Tall Serif Tabs with Sakura/Dark Palette
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = BlushPinkPrimary,
                divider = { HorizontalDivider(color = DarkBorder, thickness = 1.dp) },
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedTab])
                                .height(2.dp)
                                .background(BlushPinkPrimary)
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "UPCOMING",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = BirchTypography,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = if (selectedTab == 0) BlushPinkPrimary else MutedGreyPink
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "ATTENDED",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = BirchTypography,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = if (selectedTab == 1) BlushPinkPrimary else MutedGreyPink
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val filteredList = appointmentsWithDoctor.filter {
                if (selectedTab == 0) !it.appointment.isAttended else it.appointment.isAttended
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == 0) "No upcoming appointments." else "No past appointments recorded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedGreyPink
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredList, key = { "appt_${it.appointment.id}" }) { item ->
                        val appt = item.appointment
                        val doc = item.doctor

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(DarkBorder)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { appointmentToEdit = item }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = doc?.name ?: "Doctor Visit",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontFamily = BirchTypography,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = SoftWhite
                                        )
                                        Text(
                                            text = doc?.specialty ?: "General Consultation",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MutedGreyPink
                                        )
                                    }

                                    // Action Icons: Postpone & Delete (Edit is triggered by tapping card)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { appointmentToPostpone = item }) {
                                            Icon(
                                                imageVector = Icons.Outlined.Schedule,
                                                contentDescription = "Postpone Visit",
                                                tint = SoftWhite,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(onClick = { appointmentPendingDeletion = item }) {
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteOutline,
                                                contentDescription = "Delete Visit",
                                                tint = MutedGreyPink,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${appt.date} • ${appt.time}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = BirchTypography,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = BlushPinkPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.LocationOn,
                                                contentDescription = null,
                                                tint = MutedGreyPink,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = appt.location,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MutedGreyPink
                                            )
                                        }
                                    }

                                    // Minimal, elegant Mark Attended button / pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (appt.isAttended) Color(0xFF67D59A).copy(alpha = 0.15f) else BlushPinkPrimary.copy(alpha = 0.12f))
                                            .border(
                                                1.dp,
                                                if (appt.isAttended) Color(0xFF67D59A) else BlushPinkPrimary,
                                                RoundedCornerShape(20.dp)
                                            )
                                            .clickable {
                                                viewModel.markAttended(appt.id, !appt.isAttended)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (appt.isAttended) Icons.Outlined.CheckCircle else Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = if (appt.isAttended) Color(0xFF67D59A) else BlushPinkPrimary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = if (appt.isAttended) "ATTENDED ✓" else "MARK ATTENDED",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = BirchTypography,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp
                                                ),
                                                color = if (appt.isAttended) Color(0xFF67D59A) else BlushPinkPrimary
                                            )
                                        }
                                    }
                                }

                                if (appt.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "“${appt.notes}”",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedGreyPink,
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Confirmation dialog for deleting an appointment
        appointmentPendingDeletion?.let { item ->
            AlertDialog(
                onDismissRequest = { appointmentPendingDeletion = null },
                title = {
                    Text(
                        "Remove Appointment?",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = BirchTypography),
                        color = BlushPinkPrimary
                    )
                },
                text = {
                    Text(
                        "Are you sure you want to remove the appointment with ${item.doctor?.name ?: "Doctor"} on ${item.appointment.date}?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftWhite
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteAppointment(item.appointment.id)
                            appointmentPendingDeletion = null
                        }
                    ) {
                        Text("Remove", color = Color(0xFFE55353), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { appointmentPendingDeletion = null }) {
                        Text("Cancel", color = MutedGreyPink)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(18.dp)
            )
        }

        // Global Animated Edit Modal for Appointments
        val isModalOpen = isAddingAppointment || appointmentToEdit != null
        val editing = appointmentToEdit

        var selectedDoctorId by remember(editing, isAddingAppointment) {
            mutableStateOf(editing?.doctor?.id)
        }
        var docName by remember(editing, isAddingAppointment) {
            mutableStateOf(editing?.doctor?.name ?: "")
        }
        var spec by remember(editing, isAddingAppointment) {
            mutableStateOf(editing?.doctor?.specialty ?: "General Physician")
        }
        var dateStr by remember(editing, isAddingAppointment) {
            mutableStateOf(editing?.appointment?.date ?: "Thursday, Oct 2")
        }
        var timeStr by remember(editing, isAddingAppointment) {
            mutableStateOf(editing?.appointment?.time ?: "3:30 PM")
        }
        var locStr by remember(editing, isAddingAppointment) {
            mutableStateOf(editing?.appointment?.location ?: "City Medical Center")
        }
        var notesStr by remember(editing, isAddingAppointment) {
            mutableStateOf(editing?.appointment?.notes ?: "")
        }

        AnimatedEditModal(
            isVisible = isModalOpen,
            onDismissRequest = {
                isAddingAppointment = false
                appointmentToEdit = null
            },
            title = if (editing == null) "New Appointment" else "Edit Appointment",
            subtitle = "Doctor Visit Details",
            isSaveEnabled = docName.isNotBlank(),
            onSave = {
                viewModel.saveOrUpdateAppointment(
                    appointmentId = editing?.appointment?.id ?: 0L,
                    existingDoctorId = selectedDoctorId,
                    doctorName = docName,
                    specialty = spec,
                    date = dateStr,
                    time = timeStr,
                    location = locStr,
                    notes = notesStr
                )

                isAddingAppointment = false
                appointmentToEdit = null
            }
        ) {
            // Doctor Name (Directly editable, updates Doctor entity)
            OutlinedTextField(
                value = docName,
                onValueChange = { docName = it },
                label = { Text("Doctor's Name") },
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
                value = spec,
                onValueChange = { spec = it },
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

            OutlinedTextField(
                value = dateStr,
                onValueChange = { dateStr = it },
                label = { Text("Date") },
                placeholder = { Text("e.g. Thursday, Oct 2") },
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
                value = timeStr,
                onValueChange = { timeStr = it },
                label = { Text("Time") },
                placeholder = { Text("e.g. 3:30 PM") },
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
                value = locStr,
                onValueChange = { locStr = it },
                label = { Text("Location") },
                placeholder = { Text("e.g. City Medical Center, Suite 402") },
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
                value = notesStr,
                onValueChange = { notesStr = it },
                label = { Text("Notes / Instructions") },
                placeholder = { Text("e.g. Bring recent blood test results") },
                singleLine = false,
                maxLines = 3,
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

        // Postpone Visit Modal
        PostponeAppointmentModal(
            isVisible = appointmentToPostpone != null,
            appointmentWithDoctor = appointmentToPostpone,
            onDismissRequest = { appointmentToPostpone = null },
            onConfirmPostpone = { newDate, newTime ->
                appointmentToPostpone?.let {
                    viewModel.postponeAppointment(it.appointment.id, newDate, newTime)
                }
                appointmentToPostpone = null
            }
        )
    }
}
