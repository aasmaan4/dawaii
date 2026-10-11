package com.dawaii.app.ui.appointments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dawaii.app.data.local.AppDatabase
import com.dawaii.app.data.local.entity.Appointment
import com.dawaii.app.data.local.entity.AppointmentWithDoctor
import com.dawaii.app.data.local.entity.Doctor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppointmentsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val apptDao = db.appointmentDao()
    private val doctorDao = db.doctorDao()

    // Reactive single source of truth: Flow from Room collected as StateFlow
    val appointments: StateFlow<List<AppointmentWithDoctor>> = apptDao.getAllAppointmentsWithDoctor()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val doctors: StateFlow<List<Doctor>> = doctorDao.getAllDoctors()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        ensureSeeded()
    }

    /**
     * Updates doctor details directly in the Doctor entity table.
     * Because Room emits updates via Flow for AppointmentWithDoctor and MedicineWithDoctor,
     * this single update automatically recomposes Appointments, Dashboard, and Cabinet screens!
     */
    /**
     * Safely updates or creates an appointment ensuring Doctor exists first.
     * Prevents SQLite foreign key constraint violation crashes.
     */
    fun saveOrUpdateAppointment(
        appointmentId: Long = 0L,
        existingDoctorId: Long? = null,
        doctorName: String,
        specialty: String,
        date: String,
        time: String,
        location: String,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cleanName = doctorName.trim().ifBlank { "Doctor" }
                val cleanSpec = specialty.trim().ifBlank { "General Physician" }
                val allDocs = doctorDao.getAllDoctors().first()

                var resolvedDocId: Long? = null

                if (existingDoctorId != null && existingDoctorId > 0) {
                    val currentDoc = allDocs.find { it.id == existingDoctorId }
                    if (currentDoc != null && currentDoc.name.equals(cleanName, ignoreCase = true)) {
                        if (currentDoc.specialty != cleanSpec) {
                            doctorDao.updateDoctor(currentDoc.copy(specialty = cleanSpec))
                        }
                        resolvedDocId = currentDoc.id
                    }
                }

                if (resolvedDocId == null) {
                    val matchingDoc = allDocs.find { it.name.equals(cleanName, ignoreCase = true) }
                    if (matchingDoc != null) {
                        resolvedDocId = matchingDoc.id
                    } else {
                        resolvedDocId = doctorDao.insertDoctor(
                            Doctor(
                                name = cleanName,
                                contactNumber = "+91 98450 00000",
                                specialty = cleanSpec
                            )
                        )
                    }
                }

                if (appointmentId > 0L) {
                    val currentAppts = apptDao.getAllAppointmentsWithDoctor().first()
                    val existing = currentAppts.find { it.appointment.id == appointmentId }?.appointment
                    val updated = existing?.copy(
                        doctorId = resolvedDocId,
                        date = date,
                        time = time,
                        location = location,
                        notes = notes
                    ) ?: Appointment(
                        id = appointmentId,
                        doctorId = resolvedDocId,
                        date = date,
                        time = time,
                        location = location,
                        notes = notes,
                        isAttended = false
                    )
                    apptDao.updateAppointment(updated)
                } else {
                    apptDao.insertAppointment(
                        Appointment(
                            doctorId = resolvedDocId,
                            date = date,
                            time = time,
                            location = location,
                            notes = notes,
                            isAttended = false
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AppointmentsVM", "Error saving appointment: ${e.message}", e)
            }
        }
    }

    fun updateDoctor(doctorId: Long, newName: String, newContact: String, newSpecialty: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val doc = doctorDao.getDoctorById(doctorId)
                if (doc != null) {
                    doctorDao.updateDoctor(
                        doc.copy(
                            name = newName,
                            contactNumber = newContact,
                            specialty = newSpecialty
                        )
                    )
                } else {
                    doctorDao.insertDoctor(
                        Doctor(name = newName, contactNumber = newContact, specialty = newSpecialty)
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AppointmentsVM", "Error updating doctor", e)
            }
        }
    }

    fun saveAppointment(appointment: Appointment) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apptDao.insertAppointment(appointment)
            } catch (e: Exception) {
                android.util.Log.e("AppointmentsVM", "Error inserting appointment", e)
            }
        }
    }

    fun updateAppointment(appointment: Appointment) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apptDao.updateAppointment(appointment)
            } catch (e: Exception) {
                android.util.Log.e("AppointmentsVM", "Error updating appointment", e)
            }
        }
    }

    fun deleteAppointment(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apptDao.deleteAppointmentById(id)
            } catch (e: Exception) {
                android.util.Log.e("AppointmentsVM", "Error deleting appointment", e)
            }
        }
    }

    fun markAttended(id: Long, isAttended: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apptDao.updateAttendanceStatus(id, isAttended)
            } catch (e: Exception) {
                android.util.Log.e("AppointmentsVM", "Error marking attended", e)
            }
        }
    }

    fun postponeAppointment(id: Long, newDate: String, newTime: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apptDao.postponeAppointment(id, newDate, newTime)
            } catch (e: Exception) {
                android.util.Log.e("AppointmentsVM", "Error postponing appointment", e)
            }
        }
    }

    private fun ensureSeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            val count = apptDao.getAppointmentCount()
            if (count == 0) {
                // Ensure doctors exist without duplicates (Indian Localization)
                val existingDocs = doctorDao.getAllDoctors().first()
                val doc1Id = existingDocs.find { it.name.contains("Sharma") || it.name.contains("Vance") }?.id
                    ?: doctorDao.insertDoctor(
                        Doctor(name = "Dr. Rajesh Sharma", contactNumber = "+91 98450 12345", specialty = "Cardiologist")
                    )
                val doc2Id = existingDocs.find { it.name.contains("Iyer") || it.name.contains("Reed") }?.id
                    ?: doctorDao.insertDoctor(
                        Doctor(name = "Dr. Ananya Iyer", contactNumber = "+91 99001 67890", specialty = "General Physician")
                    )
                val doc3Id = existingDocs.find { it.name.contains("Nair") || it.name.contains("Lin") }?.id
                    ?: doctorDao.insertDoctor(
                        Doctor(name = "Dr. Priya Nair", contactNumber = "+91 98860 34567", specialty = "Dermatologist")
                    )

                apptDao.insertAppointments(
                    listOf(
                        Appointment(
                            doctorId = doc1Id,
                            date = "Thursday, Oct 8",
                            time = "3:30 PM",
                            location = "Apollo Clinic, Indiranagar, Bengaluru",
                            notes = "Bring recent ECG reports and blood pressure logs",
                            isAttended = false
                        ),
                        Appointment(
                            doctorId = doc2Id,
                            date = "Friday, Oct 23",
                            time = "10:00 AM",
                            location = "Manipal Hospital, Old Airport Road, Room 204",
                            notes = "Quarterly wellness review and lab profile",
                            isAttended = false
                        ),
                        Appointment(
                            doctorId = doc3Id,
                            date = "Tuesday, Sep 15",
                            time = "2:00 PM",
                            location = "Fortis Hospital, Bannerghatta Road",
                            notes = "Post-treatment checkup",
                            isAttended = true
                        )
                    )
                )
            }
        }
    }
}
