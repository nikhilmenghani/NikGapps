package com.nikgapps.app.utils.constants

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ApplicationConstants {
//    const val DOWNLOAD_URL = "https://sourceforge.net/projects/nikgapps/files/Releases/Android-15/04-Nov-2024/Addons/NikGapps-Addon-15-PixelSpecifics-20241104-signed.zip/downloadhttps://sourceforge.net/projects/nikgapps/files/Releases/Android-15/04-Nov-2024/Addons/NikGapps-Addon-15-PixelSpecifics-20241104-signed.zip/download"
    const val DOWNLOAD_URL = "https://sourceforge.net/projects/nikgapps/files/Releases/Android-15/04-Nov-2024/NikGapps-variant-arm64-15-20241104-signed.zip/download"
//    const val DOWNLOAD_URL = "https://sourceforge.net/projects/nikgapps/files/Releases/Android-15/04-Nov-2024/Addons/NikGapps-Addon-15-Books-20241104-signed.zip/download"
    const val REQUEST_INSTALL_UNKNOWN_APPS = 1234
    @SuppressLint("SdCardPath")
    const val NIKGAPPS_HOME_DIR = "/sdcard/NikGapps"
    const val NIKGAPPS_APP_DIR = "$NIKGAPPS_HOME_DIR/app_logs"

    var DATETIME_NOW: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm").withZone(ZoneId.of("UTC")))
    } else {
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        sdf.format(Date())
    }

    fun getDownloadUrl(variant: String): String {
        return DOWNLOAD_URL.replace("variant", variant)
    }

    fun getNikGappsDirectory(): String {
        return "${getExternalStorageDir()}/NikGapps"
    }

    fun getCacheDir(context: Context): String {
        return context.cacheDir.absolutePath
    }

    fun getNikGappsAppDownloadUrl(latestVersion: String): String {
        return "https://github.com/nikhilmenghani/nikgapps/releases/download/v$latestVersion/NikGapps-v$latestVersion.apk"
    }

    fun getExternalStorageDir(): String {
        return android.os.Environment.getExternalStorageDirectory().absolutePath
    }

    @SuppressLint("Range")
    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var displayName = "selected_file.zip"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                displayName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        }
        return displayName
    }

    fun getFilePathFromUri(context: Context, uri: Uri): String? {
        val projection = arrayOf(android.provider.MediaStore.MediaColumns.DATA)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATA)
            if (cursor.moveToFirst()) {
                return cursor.getString(columnIndex)
            }
        }

        return null
    }
}