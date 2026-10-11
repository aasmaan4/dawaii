package com.dawaii.app.ui.doctors

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dawaii.app.data.local.entity.Doctor
import com.dawaii.app.ui.appointments.AppointmentsScreen
import com.dawaii.app.ui.medicines.MyMedicinesScreen
import com.dawaii.app.ui.theme.*

@Composable
fun DoctorsAndVisitsScreen(
    onOpenDoctorTabs: ((Doctor) -> Unit)? = null
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0 = Doctors & Rx, 1 = Appointments

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
    ) {
        // Top Merged Segmented Switcher
        Surface(
            color = DarkCharcoalBg,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Doctors Sub-Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedSubTab == 0) BlushPinkPrimary else Color.Transparent)
                        .clickable { selectedSubTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = if (selectedSubTab == 0) DarkCharcoalBg else MutedGreyPink,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "My Doctors",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = BirchTypography,
                                fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Normal,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (selectedSubTab == 0) DarkCharcoalBg else MutedGreyPink
                        )
                    }
                }

                // Appointments Sub-Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedSubTab == 1) BlushPinkPrimary else Color.Transparent)
                        .clickable { selectedSubTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = if (selectedSubTab == 1) DarkCharcoalBg else MutedGreyPink,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Appointments",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = BirchTypography,
                                fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Normal,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (selectedSubTab == 1) DarkCharcoalBg else MutedGreyPink
                        )
                    }
                }
            }
        }

        // Animated View Switcher between My Doctors & Appointments
        AnimatedContent(
            targetState = selectedSubTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "doctorsAndVisitsSwitch",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { tab ->
            when (tab) {
                0 -> MyMedicinesScreen(onOpenDoctorTabs = onOpenDoctorTabs)
                1 -> AppointmentsScreen()
            }
        }
    }
}
