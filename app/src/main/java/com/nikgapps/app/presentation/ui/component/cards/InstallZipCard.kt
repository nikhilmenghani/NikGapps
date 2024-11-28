package com.nikgapps.app.presentation.ui.component.cards

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Repartition
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikgapps.R
import com.nikgapps.app.data.model.LogManager.log
import com.nikgapps.app.presentation.theme.NikGappsThemePreview
import com.nikgapps.app.presentation.ui.component.bottomsheets.InstallZipProgressBottomSheet
import com.nikgapps.app.presentation.ui.component.buttons.FilledTonalButtonWithIcon
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
    var mountResult by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var resultText by rememberSaveable { mutableStateOf("") }
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
            if (file.exists()){
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
                    }
                    else{
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(context, "Root access required to install $displayName", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                log("Failed to copy file to cache directory", context)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
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
                Spacer(modifier = Modifier.width(16.dp))
                FilledTonalButtonWithIcon(
                    text = "Execute Mount",
                    icon = Icons.Default.Repartition,
                    onClick = {
                        val rootManager = RootManager(context)
                        val result = rootManager.executeScript(R.raw.mount)
                        resultText = result.output
                        Log.d("InstallZipCard", "Mount result: $result")
                        mountResult = result.success
                    })
            }
        }
    }

    mountResult?.let { result ->
        Text(
            text = resultText,
            color = if (result) Color.Green else Color.Red,
            fontSize = 11.sp
        )
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
        var isSuccess = rootManager.executeScript(R.raw.mount)
        Log.d("RootManager", "Mount /system result: $isSuccess")
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