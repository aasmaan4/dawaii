package com.dawaii.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateManager {

    private const val TAG = "AppUpdateManager"
    const val DEFAULT_VERSION_URL = "https://dawiiii.vercel.app/version.json"
    const val DEFAULT_APK_URL = "https://dawiiii.vercel.app/dawaii.apk"

    data class UpdateInfo(
        val hasUpdate: Boolean,
        val currentVersionName: String,
        val currentVersionCode: Long,
        val serverVersionName: String,
        val serverVersionCode: Int,
        val apkUrl: String,
        val releaseNotes: String
    ) {
        val versionName: String get() = serverVersionName
        val versionCode: Int get() = serverVersionCode
    }

    fun getCurrentVersion(context: Context): Pair<String, Long> {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
            val name = pInfo.versionName ?: "Unknown"
            Pair(name, code)
        } catch (_: Exception) {
            Pair("Unknown", 0L)
        }
    }

    suspend fun checkUpdate(context: Context, versionUrl: String = DEFAULT_VERSION_URL): UpdateInfo = withContext(Dispatchers.IO) {
        val (currentName, currentCode) = getCurrentVersion(context)
        try {
            val url = URL("$versionUrl?t=${System.currentTimeMillis()}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
            }
            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(jsonStr)
                val serverVersionCode = obj.optInt("versionCode", 0)
                val serverVersionName = obj.optString("versionName", obj.optString("version", "Latest"))
                val apkUrl = obj.optString("apkUrl", DEFAULT_APK_URL)
                val releaseNotes = obj.optString("releaseNotes", "New update available")

                val hasUpdate = serverVersionCode.toLong() > currentCode
                return@withContext UpdateInfo(
                    hasUpdate = hasUpdate,
                    currentVersionName = currentName,
                    currentVersionCode = currentCode,
                    serverVersionName = serverVersionName,
                    serverVersionCode = serverVersionCode,
                    apkUrl = apkUrl,
                    releaseNotes = releaseNotes
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking update", e)
        }
        return@withContext UpdateInfo(
            hasUpdate = false,
            currentVersionName = currentName,
            currentVersionCode = currentCode,
            serverVersionName = currentName,
            serverVersionCode = currentCode.toInt(),
            apkUrl = DEFAULT_APK_URL,
            releaseNotes = "Up to date"
        )
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        apkUrl: String = DEFAULT_APK_URL,
        onStatus: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            withContext(Dispatchers.Main) {
                onStatus("Connecting to deployment server...")
            }

            val downloadDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                ?: File(context.filesDir, "updates").apply { mkdirs() }
            val apkFile = File(downloadDir, "dawaii-update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            val url = URL("$apkUrl?t=${System.currentTimeMillis()}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                requestMethod = "GET"
            }

            val totalBytes = conn.contentLength
            var downloadedBytes = 0

            conn.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var lastReport = System.currentTimeMillis()

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastReport > 400) {
                            lastReport = now
                            val progressStr = if (totalBytes > 0) {
                                val pct = (downloadedBytes * 100L / totalBytes).toInt()
                                "Downloading: $pct% (${downloadedBytes / (1024 * 1024)} MB)"
                            } else {
                                "Downloading: ${downloadedBytes / 1024} KB"
                            }
                            withContext(Dispatchers.Main) {
                                onStatus(progressStr)
                            }
                        }
                    }
                    output.flush()
                }
            }

            apkFile.setReadable(true, false)

            withContext(Dispatchers.Main) {
                onStatus("Download complete. Opening installer...")
            }

            // Trigger system package installer intent with FileProvider
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
            withContext(Dispatchers.Main) {
                onStatus("Tap Install to update!")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed downloading or launching update", e)
            withContext(Dispatchers.Main) {
                onStatus("Switching to direct browser download...")
                openDirectBrowserDownload(context, apkUrl)
            }
        }
    }

    fun openDirectBrowserDownload(context: Context, apkUrl: String = DEFAULT_APK_URL) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open browser: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
