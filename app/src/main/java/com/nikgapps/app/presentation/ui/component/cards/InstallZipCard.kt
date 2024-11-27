package com.nikgapps.app.presentation.ui.component.cards

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nikgapps.App
import com.nikgapps.R
import com.nikgapps.app.data.model.LogManager.log
import com.nikgapps.app.presentation.theme.NikGappsThemePreview
import com.nikgapps.app.presentation.ui.component.bottomsheets.InstallZipProgressBottomSheet
import com.nikgapps.app.presentation.ui.component.buttons.FilledTonalButtonWithIcon
import com.nikgapps.app.presentation.ui.viewmodel.ProgressLogViewModel
import com.nikgapps.app.utils.BuildUtility.installAppSet
import com.nikgapps.app.utils.BuildUtility.scanForApps
import com.nikgapps.app.utils.root.RootUtility
import com.nikgapps.app.utils.ZipUtility.extractZip
import com.nikgapps.app.utils.constants.ApplicationConstants.getFileNameFromUri
import com.nikgapps.app.utils.root.RootManager
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
            if (App.hasRootAccess) {
                val file = File(context.cacheDir, displayName)
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                if (file.exists()){
                    showBottomSheet = true
                    CoroutineScope(Dispatchers.IO).launch {
                        installZipFile(
                            context,
                            progressLogViewModel,
                            file,
                            progressCallback = { progress ->
                                isProcessing = progress
                            })
                        isProcessing = false
                    }
                } else {
                    log("Failed to copy file to cache directory", context)
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Root access required to install $displayName", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isProcessing) {
                    CircularProgressIndicator()
                } else {
                    FilledTonalButtonWithIcon(
                        text = "Install NikGapps",
                        icon = Icons.Default.Download,
                        onClick = {
                            filePickerLauncher.launch("application/zip")
                        })
                }
            }
        }
    }

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
    log("Installing zip file: ${file.name}", context)
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
        var isSuccess = rootManager.executeScript(R.raw.mount , "/system")
        Log.d("RootManager", "Mount /system result: $isSuccess")
        isSuccess = rootManager.executeScript(R.raw.mount , "/product")
        Log.d("RootManager", "Mount /product result: $isSuccess")
        isSuccess = rootManager.executeScript(R.raw.mount , "/system_ext")
        Log.d("RootManager", "Mount /system_ext result: $isSuccess")
        if (isSuccess.success) {
            appsets.forEach { appSet ->
                installAppSet(progressLogViewModel, appSet)
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