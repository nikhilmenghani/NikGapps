package com.nikgapps

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nikgapps.app.presentation.theme.NikGappsTheme
import com.nikgapps.app.presentation.navigation.ScreenNavigator
import com.nikgapps.app.presentation.ui.viewmodel.ProgressLogViewModel
import com.nikgapps.app.utils.permissions.Permissions
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.app.security.AppLock
import com.nikgapps.app.security.canUseAppLock
import com.nikgapps.app.update.AppUpdateManager
import com.nikgapps.app.update.UpdateActionReceiver
import com.nikgapps.app.utils.AppDiagnostics
import com.nikgapps.app.utils.NotificationUtility
import com.nikgapps.app.utils.worker.DownloadWorker
import com.nikgapps.dumps.installApk
import kotlin.getValue

class MainActivity : ComponentActivity() {
    private val progressLogViewModel: ProgressLogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppDiagnostics.info("app", "launched", mapOf(
            "version" to BuildConfig.VERSION_NAME,
            "androidApi" to Build.VERSION.SDK_INT
        ))
        if (Permissions.hasAllRequiredPermissions(this)) {
            setContent {
                NikGappsTheme {
                    AppLock(
                        enabled = globalClass.preferencesManager.displayPrefs.biometricLockEnabled && canUseAppLock(this@MainActivity),
                        activity = this@MainActivity
                    ) { ScreenNavigator(progressLogViewModel) }
                }
            }
            handleUpdateIntent(intent)
        } else {
            // Launch PermissionsActivity if any permissions are missing
            startActivity(Intent(this, PermissionsActivity::class.java))
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        AppUpdateManager.checkOnAppStart(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUpdateIntent(intent)
    }

    private fun handleUpdateIntent(updateIntent: Intent) {
        when (updateIntent.action) {
            UpdateActionReceiver.ACTION_DOWNLOAD -> {
                val version = updateIntent.getStringExtra(UpdateActionReceiver.EXTRA_VERSION).orEmpty()
                val url = updateIntent.getStringExtra(UpdateActionReceiver.EXTRA_URL).orEmpty()
                if (version.isNotBlank() && url.isNotBlank()) {
                    val workId = AppUpdateManager.enqueueDownload(this, version, url)
                    val workInfo = WorkManager.getInstance(this).getWorkInfoByIdLiveData(workId)
                    lateinit var observer: Observer<WorkInfo?>
                    observer = Observer { info ->
                        if (info?.state?.isFinished == true) {
                            workInfo.removeObserver(observer)
                            if (info.state == WorkInfo.State.SUCCEEDED && !isFinishing && !isDestroyed) {
                                info.outputData.getString(DownloadWorker.OUTPUT_APK_PATH_KEY)
                                    ?.takeIf(String::isNotBlank)
                                    ?.let(::launchUpdateInstaller)
                            }
                        }
                    }
                    workInfo.observeForever(observer)
                }
            }
            UpdateActionReceiver.ACTION_INSTALL -> updateIntent
                .getStringExtra(UpdateActionReceiver.EXTRA_APK_PATH)
                ?.takeIf(String::isNotBlank)
                ?.let(::launchUpdateInstaller)
        }
        updateIntent.action = null
    }

    private fun launchUpdateInstaller(apkPath: String) {
        NotificationManagerCompat.from(this).apply {
            cancel(NotificationUtility.UPDATE_AVAILABLE_NOTIFICATION_ID)
            cancel(NotificationUtility.UPDATE_DOWNLOAD_NOTIFICATION_ID)
            cancel(NotificationUtility.UPDATE_READY_NOTIFICATION_ID)
        }
        installApk(this, apkPath)
    }

    fun restartActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
