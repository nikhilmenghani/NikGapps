package com.nikgapps.app.presentation.ui.component.cards

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.nikgapps.R
import com.nikgapps.app.data.model.LogManager.log
import com.nikgapps.app.presentation.theme.NikGappsThemePreview
import com.nikgapps.app.presentation.ui.component.bottomsheets.InstallZipProgressBottomSheet
import com.nikgapps.app.presentation.ui.viewmodel.ProgressLogViewModel
import com.nikgapps.app.utils.BuildUtility.installAppSet
import com.nikgapps.app.utils.BuildUtility.scanForApps
import com.nikgapps.app.utils.ZipUtility.extractZip
import com.nikgapps.app.utils.constants.ApplicationConstants.getFileNameFromUri
import com.nikgapps.app.utils.root.RootManager
import com.nikgapps.dumps.RootUtility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun InstallZipCard(
    progressLogViewModel: ProgressLogViewModel
) {
    var isProcessing by rememberSaveable { mutableStateOf(false) }
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val displayName = getFileNameFromUri(context, it)
            val file = File(context.cacheDir, displayName)
            context.contentResolver.openInputStream(it)?.use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            if (file.exists()) {
                CoroutineScope(Dispatchers.IO).launch {
                    if (RootUtility.hasRootAccess()) {
                        showBottomSheet = true
                        installZipFile(
                            context,
                            progressLogViewModel,
                            file,
                            progressCallback = { progress ->
                                isProcessing = progress
                            })
                        isProcessing = false
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(
                                context,
                                "Root access required to install $displayName",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } else {
                log("Failed to copy file to cache directory")
            }
        }
    }

    ActionCard(
        title = "Install NikGapps",
        description = "Pick a NikGapps ZIP file and install it on your device. Requires root access.",
        buttonText = "Install ZIP",
        icon = Icons.Default.Download,
        isProcessing = isProcessing,
        onClick = {
            filePickerLauncher.launch("application/zip")
        }
    )

    if (showBottomSheet) {
        InstallZipProgressBottomSheet(
            viewModel = progressLogViewModel,
            onDismiss = { showBottomSheet = false },
            isProcessing = isProcessing
        )
    }
}

suspend fun installZipFile(
    context: Context,
    progressLogViewModel: ProgressLogViewModel,
    file: File,
    progressCallback: (Boolean) -> Unit = {}
) {
    withContext(Dispatchers.Main) {
        progressCallback(true)
    }
    log("Installing zip file: ${file.name}")
    if (file.exists()){
        progressLogViewModel.addLog("Extracting zip file...")
        extractZip(
            progressLogViewModel,
            file.absolutePath,
            extractNestedZips = true,
            deleteZipAfterExtract = true,
            cleanExtract = true,
            progressCallback = { }
        )
        progressLogViewModel.clearLogs()
        progressLogViewModel.addLog("Extraction Successful!")
        val appsets = scanForApps(progressLogViewModel, file.absolutePath.toString())
        progressLogViewModel.addLog("Installing NikGapps...")
        var rootManager = RootManager(context)
        var isSuccess = rootManager.executeScript(R.raw.mount)
        Log.d("RootManager", "Mount /system result: $isSuccess")
        if (isSuccess.success) {
            val baseScript = context.resources.openRawResource(R.raw.install_package).bufferedReader().use { it.readText() }
            appsets.forEach { appSet ->
                installAppSet(progressLogViewModel, appSet, rootManager, baseScript)
            }
        } else {
            Log.e("RootManager", "Failed to execute mount script")
        }
        progressLogViewModel.addLog("Installed NikGapps...")
    }

    withContext(Dispatchers.Main) {
        progressCallback(false)
    }
}

@Preview
@Composable
fun PreviewInstallZipCard() {
    NikGappsThemePreview {
        InstallZipCard(ProgressLogViewModel())
    }
}