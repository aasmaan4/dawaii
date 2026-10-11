package com.dawaii.app.ui.medicines

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dawaii.app.data.local.AppDatabase
import com.dawaii.app.data.local.entity.Doctor
import com.dawaii.app.data.local.entity.Medicine
import com.dawaii.app.data.local.entity.MedicineType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MedicinesViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val doctorDao = database.doctorDao()
    private val medicineDao = database.medicineDao()

    val doctors: StateFlow<List<Doctor>> = doctorDao.getAllDoctors()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medicines: StateFlow<List<Medicine>> = medicineDao.getAllActiveMedicines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDoctorId = MutableStateFlow<Long?>(null)
    val selectedDoctorId: StateFlow<Long?> = _selectedDoctorId.asStateFlow()

    // Doctor Modal States hoisted
    private val _showDoctorModal = MutableStateFlow(false)
    val showDoctorModal: StateFlow<Boolean> = _showDoctorModal.asStateFlow()

    private val _editingDoctor = MutableStateFlow<Doctor?>(null)
    val editingDoctor: StateFlow<Doctor?> = _editingDoctor.asStateFlow()

    // Medicine Modal States hoisted
    private val _showMedicineModal = MutableStateFlow(false)
    val showMedicineModal: StateFlow<Boolean> = _showMedicineModal.asStateFlow()

    private val _editingMedicine = MutableStateFlow<Medicine?>(null)
    val editingMedicine: StateFlow<Medicine?> = _editingMedicine.asStateFlow()

    init {
        viewModelScope.launch {
            doctors.collect { list ->
                if (_selectedDoctorId.value == null && list.isNotEmpty()) {
                    _selectedDoctorId.value = list.first().id
                }
            }
        }
    }

    fun onDoctorSelected(doctorId: Long) {
        _selectedDoctorId.value = doctorId
    }

    fun onAddDoctorClicked() {
        _editingDoctor.value = null
        _showDoctorModal.value = true
    }

    fun onEditDoctorClicked(doctor: Doctor) {
        _editingDoctor.value = doctor
        _showDoctorModal.value = true
    }

    fun onDismissDoctorModal() {
        _showDoctorModal.value = false
        _editingDoctor.value = null
    }

    fun onSaveDoctor(name: String, contact: String, specialty: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _editingDoctor.value
            if (current != null) {
                doctorDao.updateDoctor(current.copy(name = name, contactNumber = contact, specialty = specialty))
            } else {
                val newId = doctorDao.insertDoctor(Doctor(name = name, contactNumber = contact, specialty = specialty))
                _selectedDoctorId.value = newId
            }
            _showDoctorModal.value = false
            _editingDoctor.value = null
        }
    }

    fun onDeleteDoctor(doctor: Doctor) {
        viewModelScope.launch(Dispatchers.IO) {
            doctorDao.deleteDoctor(doctor)
            if (_selectedDoctorId.value == doctor.id) {
                _selectedDoctorId.value = doctors.value.firstOrNull { it.id != doctor.id }?.id
            }
        }
    }

    fun onAddMedicineClicked() {
        _editingMedicine.value = null
        _showMedicineModal.value = true
    }

    fun onEditMedicineClicked(medicine: Medicine) {
        _editingMedicine.value = medicine
        _showMedicineModal.value = true
    }

    fun onDismissMedicineModal() {
        _showMedicineModal.value = false
        _editingMedicine.value = null
    }

    fun onSaveMedicine(medicine: Medicine) {
        viewModelScope.launch(Dispatchers.IO) {
            if (medicine.id != 0L) {
                medicineDao.updateMedicine(medicine)
            } else {
                medicineDao.insertMedicine(medicine)
            }
            _showMedicineModal.value = false
            _editingMedicine.value = null
        }
    }

    fun onDeleteMedicine(medicine: Medicine) {
        viewModelScope.launch(Dispatchers.IO) {
            medicineDao.deleteMedicine(medicine)
        }
    }
}
