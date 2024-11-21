package com.nikgapps.app.utils.constants

import android.content.Context

object ApplicationConstants {
//    const val DOWNLOAD_URL = "https://sourceforge.net/projects/nikgapps/files/Releases/Android-15/04-Nov-2024/Addons/NikGapps-Addon-15-PixelSpecifics-20241104-signed.zip/downloadhttps://sourceforge.net/projects/nikgapps/files/Releases/Android-15/04-Nov-2024/Addons/NikGapps-Addon-15-PixelSpecifics-20241104-signed.zip/download"
    const val DOWNLOAD_URL = "https://sourceforge.net/projects/nikgapps/files/Releases/Android-15/04-Nov-2024/NikGapps-variant-arm64-15-20241104-signed.zip/download"
//    const val DOWNLOAD_URL = "https://sourceforge.net/projects/nikgapps/files/Releases/Android-15/04-Nov-2024/Addons/NikGapps-Addon-15-Books-20241104-signed.zip/download"
    const val REQUEST_INSTALL_UNKNOWN_APPS = 1234

    fun getDownloadUrl(variant: String): String {
        return DOWNLOAD_URL.replace("variant", variant)
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
}