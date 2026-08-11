package com.nikgapps

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import com.nikgapps.app.data.SingleChoice
import com.nikgapps.app.data.SingleText
import com.nikgapps.app.utils.managers.DownloadManager
import com.nikgapps.app.utils.managers.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.nikgapps.app.data.UpdatePrefs
import com.nikgapps.app.update.AppUpdateManager

class App: Application() {
    companion object {
        lateinit var appContext: Context

        val globalClass
            get() = appContext as App

        var hasRootAccess: Boolean = false
    }

    val preferencesManager: PreferencesManager by lazy { PreferencesManager() }
    val downloadManager: DownloadManager by lazy { DownloadManager() }
    val singleChoiceDialog: SingleChoice by lazy { SingleChoice }
    val singleTextDialog: SingleText by lazy { SingleText }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        AppUpdateManager.scheduleChecks(this, UpdatePrefs.intervalHours)
        if (UpdatePrefs.intervalHours > 0) AppUpdateManager.checkOnAppStart(this)
    }

    fun showMsg(@StringRes msgSrc: Int) {
        showMsg(getString(msgSrc))
    }

    fun showMsg(msg: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(this@App, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
