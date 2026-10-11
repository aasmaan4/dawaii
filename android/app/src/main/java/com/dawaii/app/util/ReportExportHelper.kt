package com.dawaii.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.dawaii.app.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object ReportExportHelper {

    fun launchExportReport(
        context: Context,
        startDate: LocalDate = LocalDate.now().minusDays(30),
        endDate: LocalDate = LocalDate.now()
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val chooserIntent = exportAdherenceReport(context, startDate, endDate)
            chooserIntent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }
        }
    }

    suspend fun exportAdherenceReport(
        context: Context,
        startDate: LocalDate,
        endDate: LocalDate
    ): Intent? = withContext(Dispatchers.IO) {
        val database = AppDatabase.getInstance(context)
        val today = LocalDate.now()
        val sDate = if (startDate.isBefore(endDate)) startDate else endDate
        val eDate = if (startDate.isBefore(endDate)) endDate else startDate
        val totalDays = ChronoUnit.DAYS.between(sDate, eDate) + 1

        val logs = database.dailyMedicationLogDao()
            .getLogsWithDetailsForRange(sDate, eDate)
            .firstOrNull() ?: emptyList()

        val allMedicines = database.medicineDao().getAllActiveMedicines().firstOrNull() ?: emptyList()
        val allDoctors = database.doctorDao().getAllDoctors().firstOrNull() ?: emptyList()

        val csvBuilder = StringBuilder()
        csvBuilder.appendLine("DAWAII!! CLINICAL MEDICATION ADHERENCE REPORT")
        csvBuilder.appendLine("Generated on: ${today.format(DateTimeFormatter.ISO_DATE)}")
        csvBuilder.appendLine("Reporting Period: ${sDate.format(DateTimeFormatter.ISO_DATE)} to ${eDate.format(DateTimeFormatter.ISO_DATE)} ($totalDays Days)")
        if (allDoctors.isNotEmpty()) {
            csvBuilder.appendLine("Consulting Doctors: " + allDoctors.joinToString("; ") { "${it.name} (${it.specialty}) [${it.contactNumber}]" })
        }
        csvBuilder.appendLine()
        csvBuilder.appendLine("Date,Medicine Name,Doctor,Dosage,Status")

        var takenCount = 0
        var totalCount = 0

        var curr = sDate
        while (!curr.isAfter(eDate)) {
            val dateLogs = logs.filter { it.log.date == curr }
            if (dateLogs.isNotEmpty()) {
                dateLogs.forEach { item ->
                    totalCount++
                    val status = if (item.log.isTaken) {
                        takenCount++
                        "TAKEN"
                    } else if (curr == today) "PENDING" else "MISSED"

                    val medName = item.medicineWithDoctor.medicine.name
                    val docName = item.medicineWithDoctor.doctor?.name ?: "General Prescribing"
                    val dosage = item.medicineWithDoctor.medicine.dosage
                    csvBuilder.appendLine("${curr.format(DateTimeFormatter.ISO_DATE)},\"$medName\",\"$docName\",\"$dosage\",$status")
                }
            } else if (allMedicines.isNotEmpty()) {
                allMedicines.forEach { med ->
                    totalCount++
                    val doc = allDoctors.find { it.id == med.doctorId }?.name ?: "Dr. Rajesh Sharma"
                    val status = if (curr == today) "PENDING" else "MISSED"
                    csvBuilder.appendLine("${curr.format(DateTimeFormatter.ISO_DATE)},\"${med.name}\",\"$doc\",\"${med.dosage}\",$status")
                }
            }
            curr = curr.plusDays(1)
        }

        val compliancePercent = if (totalCount > 0) (takenCount * 100) / totalCount else 100
        val missedCount = totalCount - takenCount

        csvBuilder.appendLine()
        csvBuilder.appendLine("SUMMARY CLINICAL STATISTICS")
        csvBuilder.appendLine("Days in Selected Range,$totalDays")
        csvBuilder.appendLine("Total Doses Scheduled,$totalCount")
        csvBuilder.appendLine("Total Doses Taken,$takenCount")
        csvBuilder.appendLine("Total Doses Missed,$missedCount")
        csvBuilder.appendLine("Adherence Rate,$compliancePercent%")

        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()

        val fileName = "dawaii_clinical_report_${sDate}_to_${eDate}.csv"
        val file = File(reportsDir, fileName)
        file.writeText(csvBuilder.toString())

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Dawaii Clinical Medication Report: $sDate to $eDate ($compliancePercent%)")
            putExtra(Intent.EXTRA_TEXT, "Attached is the clinical medication adherence report for the doctor ($sDate to $eDate). Overall Adherence: $compliancePercent% across $totalDays days.")
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        Intent.createChooser(sendIntent, "Export Doctor's Medical Report")
    }
}
