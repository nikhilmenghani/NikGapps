package com.nikgapps.app.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nikgapps.dumps.installApk

class UpdateActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DOWNLOAD -> {
                val version = intent.getStringExtra(EXTRA_VERSION).orEmpty()
                val url = intent.getStringExtra(EXTRA_URL).orEmpty()
                if (version.isNotBlank() && url.isNotBlank())
                    AppUpdateManager.enqueueDownload(context, version, url)
            }
            ACTION_INSTALL -> intent.getStringExtra(EXTRA_APK_PATH)?.let { installApk(context, it) }
        }
    }

    companion object {
        const val ACTION_DOWNLOAD = "com.nikgapps.action.DOWNLOAD_UPDATE"
        const val ACTION_INSTALL = "com.nikgapps.action.INSTALL_UPDATE"
        const val EXTRA_VERSION = "update_version"
        const val EXTRA_URL = "update_url"
        const val EXTRA_APK_PATH = "update_apk_path"
    }
}
