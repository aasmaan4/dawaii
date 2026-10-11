package com.dawaii.app.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dawaii.app.data.local.AppDatabase
import com.dawaii.app.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class AdherenceStatus {
    ALL_TAKEN,        // Green: 0 Missed (All medications taken)
    PARTIALLY_MISSED, // Amber/Yellow: Partially Missed (Some taken, some missed)
    ALL_MISSED        // Red: All Missed (No medications taken)
}

data class DailyMedicineCheckItem(
    val medicineId: Long,
    val name: String,
    val dosage: String,
    val doctorName: String?,
    val timeFormatted: String,
    val scheduledHour: Int,
    val scheduledMinute: Int,
    val type: MedicineType,
    val isTaken: Boolean
)

data class DashboardUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val displayedMonth: YearMonth = YearMonth.now(),
    val checklist: List<DailyMedicineCheckItem> = emptyList(),
    val adherenceMap: Map<LocalDate, AdherenceStatus> = emptyMap(),
    val remainingCount: Int = 0,
    val upcomingAppointment: AppointmentWithDoctor? = null,
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val doctorDao = database.doctorDao()
    private val medicineDao = database.medicineDao()
    private val appointmentDao = database.appointmentDao()
    private val dailyLogDao = database.dailyMedicationLogDao()

    val doctors: StateFlow<List<Doctor>> = doctorDao.getAllDoctors()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingAppointment: StateFlow<AppointmentWithDoctor?> = appointmentDao.getNextUpcomingAppointment()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _displayedMonth = MutableStateFlow(YearMonth.now())
    val displayedMonth: StateFlow<YearMonth> = _displayedMonth.asStateFlow()

    // Relational query: automatically re-emits when Doctor or Medicine tables are updated
    private val allActiveMedicinesWithDoctor: Flow<List<MedicineWithDoctor>> = medicineDao.getAllMedicinesWithDoctor()

    // Monthly logs for the currently displayed month
    private val monthlyLogs: Flow<List<DailyMedicationLog>> = _displayedMonth.flatMapLatest { ym ->
        val start = ym.atDay(1)
        val end = ym.atEndOfMonth()
        dailyLogDao.getLogsForRange(start, end)
    }

    // Daily logs for the selected date
    private val selectedDateLogs: Flow<List<DailyMedicationLog>> = _selectedDate.flatMapLatest { date ->
        dailyLogDao.getLogsForDate(date)
    }

    private val calendarStateFlow = combine(_selectedDate, _displayedMonth) { selDate, dispMonth ->
        Pair(selDate, dispMonth)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        calendarStateFlow,
        allActiveMedicinesWithDoctor,
        selectedDateLogs,
        monthlyLogs,
        upcomingAppointment
    ) { (selDate, dispMonth), medsWithDoc, dateLogs, mLogs, upcomingAppt ->
        // Build checklist for selectedDate
        val logMapForDay = dateLogs.associateBy { it.medicineId }
        val checklistItems = medsWithDoc.map { medWithDoc ->
            val med = medWithDoc.medicine
            val isTaken = logMapForDay[med.id]?.isTaken ?: false
            DailyMedicineCheckItem(
                medicineId = med.id,
                name = med.name,
                dosage = med.dosage,
                doctorName = medWithDoc.doctor?.name,
                timeFormatted = formatTime(med.scheduledHour, med.scheduledMinute),
                scheduledHour = med.scheduledHour,
                scheduledMinute = med.scheduledMinute,
                type = med.type,
                isTaken = isTaken
            )
        }.sortedWith(compareBy({ it.scheduledHour }, { it.scheduledMinute }))

        val remaining = checklistItems.count { !it.isTaken }

        // Compute adherence for each day in displayed month
        val today = LocalDate.now()
        val logsByDate = mLogs.groupBy { it.date }
        val adherence = mutableMapOf<LocalDate, AdherenceStatus>()

        val daysInMonth = dispMonth.lengthOfMonth()
        for (dayNum in 1..daysInMonth) {
            val date = dispMonth.atDay(dayNum)
            if (!date.isAfter(today) && medsWithDoc.isNotEmpty()) {
                val dayLogs = logsByDate[date] ?: emptyList()
                val takenCount = dayLogs.count { it.isTaken }
                val totalMeds = medsWithDoc.size

                val status = when {
                    takenCount == totalMeds -> AdherenceStatus.ALL_TAKEN
                    takenCount > 0 -> AdherenceStatus.PARTIALLY_MISSED
                    else -> AdherenceStatus.ALL_MISSED
                }
                adherence[date] = status
            }
        }

        DashboardUiState(
            selectedDate = selDate,
            displayedMonth = dispMonth,
            checklist = checklistItems,
            adherenceMap = adherence,
            remainingCount = remaining,
            upcomingAppointment = upcomingAppt,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(selectedDate = LocalDate.now(), isLoading = true)
    )

    init {
        ensureDatabaseSeeded()
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        if (YearMonth.from(date) != _displayedMonth.value) {
            _displayedMonth.value = YearMonth.from(date)
        }
    }

    fun changeMonth(deltaMonths: Long) {
        val newMonth = _displayedMonth.value.plusMonths(deltaMonths)
        _displayedMonth.value = newMonth
    }

    fun markAppointmentAttended(appointmentId: Long, isAttended: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            appointmentDao.updateAttendanceStatus(appointmentId, isAttended)
        }
    }

    fun postponeAppointment(appointmentId: Long, newDate: String, newTime: String) {
        viewModelScope.launch(Dispatchers.IO) {
            appointmentDao.postponeAppointment(appointmentId, newDate, newTime)
        }
    }

    /**
     * Toggles the medication taken status ONLY for the currently selected date.
     * Independent daily logging via DailyMedicationLog table.
     */
    fun toggleMedicine(medicineId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val date = _selectedDate.value
            val existing = dailyLogDao.getLogForMedicine(date, medicineId)
            val newTaken = if (existing != null) !existing.isTaken else true

            dailyLogDao.insertOrUpdateLog(
                DailyMedicationLog(
                    id = existing?.id ?: 0,
                    date = date,
                    medicineId = medicineId,
                    isTaken = newTaken
                )
            )

            if (newTaken) {
                // Cancel persistent lock screen notification once marked taken in app
                com.dawaii.app.util.MedicationNotificationHelper.cancelNotification(getApplication(), medicineId)
                val now = System.currentTimeMillis()
                database.doseLogDao().markActiveDoseTaken(medicineId, now)
                database.doseLogDao().insertLog(
                    DoseLog(
                        medicationId = medicineId,
                        scheduledTimeMillis = now,
                        actualTriggerTimeMillis = now,
                        actionTimeMillis = now,
                        status = DoseStatus.TAKEN,
                        triggerType = "APP_CHECKLIST"
                    )
                )
            }
        }
    }

    private fun ensureDatabaseSeeded() {
        val prefs = com.dawaii.app.data.preferences.AppPreferences.getInstance(getApplication())
        if (prefs.isInitialSeedDone) return

        viewModelScope.launch(Dispatchers.IO) {
            val count = medicineDao.getAllActiveMedicines().first().size
            val today = LocalDate.now()

            val docId1: Long
            val docId2: Long
            val med1: Long
            val med2: Long
            val med3: Long

            if (count == 0) {
                prefs.isInitialSeedDone = true
                docId1 = doctorDao.insertDoctor(
                    Doctor(name = "Dr. Rajesh Sharma", contactNumber = "+91 98450 12345", specialty = "Cardiologist")
                )
                docId2 = doctorDao.insertDoctor(
                    Doctor(name = "Dr. Ananya Iyer", contactNumber = "+91 99001 67890", specialty = "General Physician")
                )

                med1 = medicineDao.insertMedicine(
                    Medicine(
                        doctorId = docId1,
                        name = "Blood Pressure (Amlodipine)",
                        dosage = "5mg",
                        frequency = "Daily at 8:00 AM",
                        type = MedicineType.PILL,
                        pillsLeft = 14,
                        scheduledHour = 8,
                        scheduledMinute = 0
                    )
                )
                med2 = medicineDao.insertMedicine(
                    Medicine(
                        doctorId = docId2,
                        name = "Becosules Multivitamin",
                        dosage = "1 Capsule",
                        frequency = "Daily at 1:00 PM",
                        type = MedicineType.CAPSULE,
                        pillsLeft = 30,
                        scheduledHour = 13,
                        scheduledMinute = 0
                    )
                )
                med3 = medicineDao.insertMedicine(
                    Medicine(
                        doctorId = docId1,
                        name = "Evening Shelcal (Calcium)",
                        dosage = "500mg",
                        frequency = "Daily at 9:30 PM",
                        type = MedicineType.PILL,
                        pillsLeft = 22,
                        scheduledHour = 21,
                        scheduledMinute = 30
                    )
                )
            } else {
                prefs.isInitialSeedDone = true
                val meds = medicineDao.getAllActiveMedicines().first()
                med1 = meds.getOrNull(0)?.id ?: 1L
                med2 = meds.getOrNull(1)?.id ?: 2L
                med3 = meds.getOrNull(2)?.id ?: 3L
            }

            // Seed historical DailyMedicationLog entries if table is empty
            if (dailyLogDao.getLogCount() == 0) {
                // Day -2 (2 days ago): 0 Missed (All 3 taken) -> Green
                val dateMinus2 = today.minusDays(2)
                dailyLogDao.insertOrUpdateLog(DailyMedicationLog(date = dateMinus2, medicineId = med1, isTaken = true))
                dailyLogDao.insertOrUpdateLog(DailyMedicationLog(date = dateMinus2, medicineId = med2, isTaken = true))
                dailyLogDao.insertOrUpdateLog(DailyMedicationLog(date = dateMinus2, medicineId = med3, isTaken = true))

                // Day -1 (Yesterday): Partially Missed (2 taken, 1 missed) -> Amber/Yellow
                val dateMinus1 = today.minusDays(1)
                dailyLogDao.insertOrUpdateLog(DailyMedicationLog(date = dateMinus1, medicineId = med1, isTaken = true))
                dailyLogDao.insertOrUpdateLog(DailyMedicationLog(date = dateMinus1, medicineId = med2, isTaken = false))
                dailyLogDao.insertOrUpdateLog(DailyMedicationLog(date = dateMinus1, medicineId = med3, isTaken = true))

                // Day -3: All Missed (0 taken) -> Red
                val dateMinus3 = today.minusDays(3)
                dailyLogDao.insertOrUpdateLog(DailyMedicationLog(date = dateMinus3, medicineId = med1, isTaken = false))
                dailyLogDao.insertOrUpdateLog(DailyMedicationLog(date = dateMinus3, medicineId = med2, isTaken = false))
                dailyLogDao.insertOrUpdateLog(DailyMedicationLog(date = dateMinus3, medicineId = med3, isTaken = false))

                // Today: Unchecked by default
                dailyLogDao.insertOrUpdateLog(DailyMedicationLog(date = today, medicineId = med1, isTaken = false))
                dailyLogDao.insertOrUpdateLog(DailyMedicationLog(date = today, medicineId = med2, isTaken = false))
                dailyLogDao.insertOrUpdateLog(DailyMedicationLog(date = today, medicineId = med3, isTaken = false))
            }
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val period = if (hour >= 12) "PM" else "AM"
        val h = if (hour % 12 == 0) 12 else hour % 12
        val m = String.format("%02d", minute)
        return "$h:$m $period"
    }
}
