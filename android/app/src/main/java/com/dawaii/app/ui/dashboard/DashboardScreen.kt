package com.dawaii.app.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dawaii.app.data.local.entity.AppointmentWithDoctor
import com.dawaii.app.ui.components.PostponeAppointmentModal
import com.dawaii.app.ui.theme.*
import com.dawaii.app.util.ReportExportHelper
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val today = remember { LocalDate.now() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()) }

    var horizontalDragAccumulator by remember { mutableFloatStateOf(0f) }
    var appointmentToPostpone by remember { mutableStateOf<AppointmentWithDoctor?>(null) }
    var showDateRangeDialog by remember { mutableStateOf(false) }
    var selectedRangeDays by remember { mutableIntStateOf(30) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
            .pointerInput(uiState.selectedDate) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDragAccumulator = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        horizontalDragAccumulator += dragAmount
                    },
                    onDragEnd = {
                        if (horizontalDragAccumulator < -50f) {
                            // Swiped left -> Switch to next day with animated slide
                            viewModel.selectDate(uiState.selectedDate.plusDays(1))
                        } else if (horizontalDragAccumulator > 50f) {
                            // Swiped right -> Switch to previous day with animated slide
                            viewModel.selectDate(uiState.selectedDate.minusDays(1))
                        }
                    }
                )
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. TOP ELEMENT: The Daily Medication Checker (Checklist interacting with DailyMedicationLog)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                AnimatedContent(
                    targetState = uiState.selectedDate,
                    transitionSpec = {
                        if (targetState.isAfter(initialState)) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "DateTitleAnimation"
                ) { date ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.selectDate(date.minusDays(1)) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ChevronLeft,
                                    contentDescription = "Previous Day",
                                    tint = BlushPinkPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                            Column(modifier = Modifier.clickable { viewModel.selectDate(today) }) {
                                val subtitle = when {
                                    date.isEqual(today) -> "Today"
                                    date.isEqual(today.minusDays(1)) -> "Yesterday"
                                    date.isEqual(today.plusDays(1)) -> "Tomorrow"
                                    else -> date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = BirchTypography,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = BlushPinkPrimary
                                )
                                Text(
                                    text = date.format(dateFormatter),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedGreyPink
                                )
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                            IconButton(
                                onClick = { viewModel.selectDate(date.plusDays(1)) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ChevronRight,
                                    contentDescription = "Next Day",
                                    tint = BlushPinkPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Summary Pill indicator
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (uiState.remainingCount == 0) Color(0xFF67D59A).copy(alpha = 0.15f) else BlushPinkPrimary.copy(alpha = 0.15f))
                                .border(1.dp, if (uiState.remainingCount == 0) Color(0xFF67D59A) else BlushPinkPrimary, RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (uiState.remainingCount == 0) "ALL TAKEN ✓" else "${uiState.remainingCount} TO TAKE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = if (uiState.remainingCount == 0) Color(0xFF67D59A) else BlushPinkPrimary
                            )
                        }
                    }
                }
            }

            // Checklist Items for Selected Day
            item {
                AnimatedContent(
                    targetState = uiState.selectedDate,
                    transitionSpec = {
                        if (targetState.isAfter(initialState)) {
                            (slideInHorizontally { width -> width / 2 } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width / 2 } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width / 2 } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width / 2 } + fadeOut()
                            )
                        }
                    },
                    label = "ChecklistSlideAnimation"
                ) { _ ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (uiState.checklist.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No medications scheduled for this day.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedGreyPink
                                )
                            }
                        } else {
                            uiState.checklist.forEach { item ->
                                MinimalChecklistItem(
                                    item = item,
                                    onToggle = { viewModel.toggleMedicine(item.medicineId) }
                                )
                            }
                        }
                    }
                }

                // Very minute, breathable spacing between medication marker and calendar
                Spacer(modifier = Modifier.height(6.dp))

                // 2. Box Grid Calendar & Color Legend (MOVED ABOVE DOCTOR APPOINTMENT)
                BoxGridCalendar(
                    displayedMonth = uiState.displayedMonth,
                    selectedDate = uiState.selectedDate,
                    adherenceMap = uiState.adherenceMap,
                    today = today,
                    onDateSelected = { viewModel.selectDate(it) },
                    onPrevMonth = { viewModel.changeMonth(-1) },
                    onNextMonth = { viewModel.changeMonth(1) }
                )
            }

            // Adherence Color Legend directly below calendar
            item {
                AdherenceColorLegend()
            }

            // 3. Next Doctor Appointment Card
            uiState.upcomingAppointment?.let { apptWithDoc ->
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(DarkBorder)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(BlushPinkPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DateRange,
                                        contentDescription = "Appointment",
                                        tint = BlushPinkPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "NEXT DOCTOR APPOINTMENT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 1.5.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = BlushPinkPrimary
                                    )
                                    Text(
                                        text = apptWithDoc.doctor?.name ?: "Attending Doctor",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = BirchTypography,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = SoftWhite
                                    )
                                    Text(
                                        text = "${apptWithDoc.appointment.date} • ${apptWithDoc.appointment.time}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedGreyPink
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = DarkBorder.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Quick Appointment Actions: Mark Attended & Postpone
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mark Attended Pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(BlushPinkPrimary.copy(alpha = 0.12f))
                                        .border(1.dp, BlushPinkPrimary, RoundedCornerShape(20.dp))
                                        .clickable {
                                            viewModel.markAppointmentAttended(apptWithDoc.appointment.id, true)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                 ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = BlushPinkPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "MARK ATTENDED",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = BirchTypography,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            ),
                                            color = BlushPinkPrimary
                                        )
                                    }
                                }

                                // Postpone Button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(DarkSurfaceVariant)
                                        .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
                                        .clickable {
                                            appointmentToPostpone = apptWithDoc
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Schedule,
                                            contentDescription = null,
                                            tint = SoftWhite,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "POSTPONE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = BirchTypography,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            ),
                                            color = SoftWhite
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Doctor's Report Export
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(DarkBorder)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDateRangeDialog = true
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BlushPinkContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DateRange,
                                contentDescription = null,
                                tint = BlushPinkPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Export Doctor's Report",
                                fontFamily = TallSerifFamily,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SoftWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Select date range & download clinical adherence report.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = MutedGreyPink
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(BlushPinkPrimary)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "EXPORT",
                                fontFamily = TallSerifFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = DarkCharcoalBg
                            )
                        }
                    }
                }
            }
        }

        // Animated Postpone Modal
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

        // Date Range Selection Dialog for Doctor's Medical PDF/CSV Report
        if (showDateRangeDialog) {
            val now = remember { LocalDate.now() }
            val startDate = remember(selectedRangeDays) {
                if (selectedRangeDays == -1) now.withDayOfMonth(1)
                else now.minusDays((selectedRangeDays - 1).toLong())
            }

            AlertDialog(
                onDismissRequest = { showDateRangeDialog = false },
                containerColor = DarkSurface,
                title = {
                    Text(
                        text = "Doctor's Medical Report",
                        fontFamily = TallSerifFamily,
                        fontSize = 20.sp,
                        color = BlushPinkPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Choose a date range to generate a clinical adherence report for your doctor.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = SoftWhite
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "QUICK PRESETS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MutedGreyPink
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(7 to "7D", 14 to "14D", 30 to "30D", 90 to "90D", -1 to "Month").forEach { (days, label) ->
                                val isSelected = selectedRangeDays == days
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) BlushPinkPrimary else DarkSurfaceVariant)
                                        .clickable { selectedRangeDays = days }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) DarkCharcoalBg else SoftWhite
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceVariant)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Selected Window:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedGreyPink
                                )
                                Text(
                                    text = "$startDate to $now",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BlushPinkPrimary
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDateRangeDialog = false
                            ReportExportHelper.launchExportReport(context, startDate, now)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlushPinkPrimary)
                    ) {
                        Text("Export Report", color = DarkCharcoalBg, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDateRangeDialog = false }) {
                        Text("Cancel", color = MutedGreyPink)
                    }
                }
            )
        }
    }
}

/**
 * Clean, minimal Box Grid Calendar (Month View)
 */
@Composable
fun BoxGridCalendar(
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    adherenceMap: Map<LocalDate, AdherenceStatus>,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthTitle = remember(displayedMonth) {
        val monthName = displayedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()
        "$monthName ${displayedMonth.year}"
    }

    val daysOfWeek = remember { listOf("S", "M", "T", "W", "T", "F", "S") }

    // Calculate grid cells: first day offset (Sunday = 0 to 6)
    val firstDayOfWeek = displayedMonth.atDay(1).dayOfWeek.value % 7 // Sunday = 0
    val daysInMonth = displayedMonth.lengthOfMonth()
    val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Month Navigation Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous Month", tint = BlushPinkPrimary)
                }

                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = BirchTypography,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = SoftWhite
                )

                IconButton(onClick = onNextMonth, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "Next Month", tint = BlushPinkPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Day of week column headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeek.forEach { dayLetter ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayLetter,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = BirchTypography
                            ),
                            color = MutedGreyPink
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Month Grid Boxes (7 Columns)
            val numRows = totalCells / 7
            for (row in 0 until numRows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - firstDayOfWeek + 1

                        if (dayNumber in 1..daysInMonth) {
                            val date = displayedMonth.atDay(dayNumber)
                            val isSelected = date.isEqual(selectedDate)
                            val adherence = adherenceMap[date]

                            // Dynamic Box Color based on Adherence
                            val boxBgColor = when {
                                adherence == AdherenceStatus.ALL_TAKEN -> Color(0xFF67D59A).copy(alpha = 0.22f)
                                adherence == AdherenceStatus.PARTIALLY_MISSED -> Color(0xFFF5A623).copy(alpha = 0.22f)
                                adherence == AdherenceStatus.ALL_MISSED -> Color(0xFFFF6B6B).copy(alpha = 0.22f)
                                else -> Color.Transparent
                            }

                            val dotColor = when {
                                adherence == AdherenceStatus.ALL_TAKEN -> Color(0xFF67D59A)
                                adherence == AdherenceStatus.PARTIALLY_MISSED -> Color(0xFFF5A623)
                                adherence == AdherenceStatus.ALL_MISSED -> Color(0xFFFF6B6B)
                                else -> Color.Transparent
                            }

                            val borderColor = if (isSelected) BlushPinkPrimary else if (date.isEqual(today)) SoftWhite.copy(alpha = 0.4f) else DarkBorder.copy(alpha = 0.6f)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(boxBgColor)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = borderColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onDateSelected(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = BirchTypography,
                                            fontWeight = if (isSelected || date.isEqual(today)) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) BlushPinkPrimary else SoftWhite,
                                        fontSize = 12.sp
                                    )
                                    if (adherence != null) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Empty slot outside the month
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Minimal Color Legend indicating Daily Medication Adherence
 */
@Composable
fun AdherenceColorLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Green: 0 Missed
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF67D59A))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "0 Missed",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = BirchTypography),
            color = MutedGreyPink
        )

        Spacer(modifier = Modifier.width(18.dp))

        // Amber/Yellow: Partially Missed
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFF5A623))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Partially Missed",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = BirchTypography),
            color = MutedGreyPink
        )

        Spacer(modifier = Modifier.width(18.dp))

        // Red: All Missed
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFFF6B6B))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "All Missed",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = BirchTypography),
            color = MutedGreyPink
        )
    }
}

/**
 * Hyper-minimal Checklist Interface
 * Uses clean lines, custom tall serif typography, and subtle color shifts without bulky cards.
 */
@Composable
fun MinimalChecklistItem(
    item: DailyMedicineCheckItem,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Minimal Custom Checkbox
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (item.isTaken) BlushPinkPrimary else Color.Transparent)
                        .border(
                            width = 1.5.dp,
                            color = if (item.isTaken) BlushPinkPrimary else MutedGreyPink.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isTaken) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Taken",
                            tint = DarkCharcoalBg,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = BirchTypography,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            textDecoration = if (item.isTaken) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (item.isTaken) MutedGreyPink.copy(alpha = 0.7f) else SoftWhite
                    )
                }
            }

            // Scheduled Time
            Text(
                text = item.timeFormatted,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = BirchTypography,
                    fontWeight = FontWeight.Medium
                ),
                color = if (item.isTaken) BlushPinkPrimary.copy(alpha = 0.7f) else BlushPinkPrimary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = DarkBorder.copy(alpha = 0.4f), thickness = 0.8.dp)
    }
}
