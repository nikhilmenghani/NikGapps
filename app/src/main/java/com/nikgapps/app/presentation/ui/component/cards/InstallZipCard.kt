package com.nikgapps.app.presentation.ui.component.cards

import android.content.Context
import android.net.Uri
import android.util.Log
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nikgapps.app.data.model.LogManager.log
import com.nikgapps.app.presentation.theme.NikGappsThemePreview
import com.nikgapps.app.presentation.ui.component.bottomsheets.InstallZipProgressBottomSheet
import com.nikgapps.app.presentation.ui.component.buttons.FilledTonalButtonWithIcon
import com.nikgapps.app.utils.ZipUtility.extractZip
import com.nikgapps.app.utils.constants.ApplicationConstants.getFileNameFromUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun InstallZipCard() {
    var isProcessing by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
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
            showBottomSheet = true
            CoroutineScope(Dispatchers.IO).launch {
                installZipFile(context, file, progressCallback = { progress ->
                    isProcessing = progress
                })
                isProcessing = false
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
            onDismiss = { showBottomSheet = false }
        )
    }
}

suspend fun installZipFile(context: Context, file: File, progressCallback: (Boolean) -> Unit = {}) {
    withContext(Dispatchers.Main) {
        progressCallback(true)
    }
    Log.d("NikGapps-InstallZipFile", "Installing zip file: ${file.absolutePath}")
    Log.d("NikGapps-InstallZipFile", "Installing zip file: ${file.parentFile?.absolutePath}")
    extractZip(
        file.absolutePath,
        file.parentFile?.absolutePath.toString(),
        extractNestedZips = true,
        deleteZipAfterExtract = true,
        progressCallback = { }
    )
    log("Extraction Complete", context)
    log("Installing NikGapps...", context)
    withContext(Dispatchers.Main) {
        progressCallback(false)
    }
}

@Preview
@Composable
fun PreviewInstallZipCard() {
    NikGappsThemePreview {
        InstallZipCard()
    }
}