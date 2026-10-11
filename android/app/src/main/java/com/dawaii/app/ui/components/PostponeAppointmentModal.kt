package com.dawaii.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dawaii.app.data.local.entity.AppointmentWithDoctor
import com.dawaii.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun PostponeAppointmentModal(
    isVisible: Boolean,
    appointmentWithDoctor: AppointmentWithDoctor?,
    onDismissRequest: () -> Unit,
    onConfirmPostpone: (newDate: String, newTime: String) -> Unit
) {
    if (!isVisible || appointmentWithDoctor == null) return

    val appt = appointmentWithDoctor.appointment
    val doctor = appointmentWithDoctor.doctor

    var selectedDateText by remember(appointmentWithDoctor) {
        mutableStateOf(appt.date)
    }
    var selectedTimeText by remember(appointmentWithDoctor) {
        mutableStateOf(appt.time)
    }

    val datePresets = remember {
        listOf(
            "Tomorrow" to 1,
            "+3 Days" to 3,
            "+1 Week" to 7,
            "+2 Weeks" to 14,
            "+1 Month" to 30
        )
    }

    val timePresets = remember {
        listOf("9:00 AM", "11:00 AM", "2:30 PM", "4:00 PM", "5:30 PM")
    }

    val formatter = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable { onDismissRequest() },
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.92f, animationSpec = tween(250)),
                exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.92f, animationSpec = tween(180))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 24.dp)
                        .clickable(enabled = false) {}, // Prevent dismiss when tapping card
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "POSTPONE VISIT",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = BirchTypography,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp
                                    ),
                                    color = BlushPinkPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = doctor?.name ?: "Doctor Consultation",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SoftWhite
                                )
                            }

                            IconButton(
                                onClick = onDismissRequest,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Close",
                                    tint = SoftWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Date Shift Chips
                        Text(
                            text = "QUICK SHIFT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = BirchTypography,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MutedGreyPink
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            datePresets.forEach { (label, days) ->
                                val targetCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }
                                val targetDateStr = formatter.format(targetCal.time)
                                val isSelected = selectedDateText == targetDateStr

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) BlushPinkPrimary.copy(alpha = 0.2f) else DarkSurfaceVariant)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) BlushPinkPrimary else DarkBorder.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedDateText = targetDateStr
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = BirchTypography,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) BlushPinkPrimary else SoftWhite,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Date Input
                        OutlinedTextField(
                            value = selectedDateText,
                            onValueChange = { selectedDateText = it },
                            label = { Text("New Date") },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    tint = BlushPinkPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
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

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Time Chips
                        Text(
                            text = "TIME OPTIONS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = BirchTypography,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MutedGreyPink
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            timePresets.forEach { timeOption ->
                                val isSelected = selectedTimeText == timeOption
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) BlushPinkPrimary.copy(alpha = 0.2f) else DarkSurfaceVariant)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) BlushPinkPrimary else DarkBorder.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedTimeText = timeOption
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = timeOption,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = BirchTypography,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) BlushPinkPrimary else SoftWhite,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Time Input
                        OutlinedTextField(
                            value = selectedTimeText,
                            onValueChange = { selectedTimeText = it },
                            label = { Text("New Time") },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = BlushPinkPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
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

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onDismissRequest) {
                                Text("Cancel", color = MutedGreyPink)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onConfirmPostpone(selectedDateText, selectedTimeText)
                                    onDismissRequest()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BlushPinkPrimary,
                                    contentColor = DarkCharcoalBg
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "Save New Time",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = BirchTypography,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
