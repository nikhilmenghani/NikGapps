package com.nikgapps.app.presentation.ui.component.cards

import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.R
import com.nikgapps.app.data.model.GappsVariantPreference
import com.nikgapps.app.data.model.toVariantString
import com.nikgapps.app.presentation.theme.NikGappsThemePreview
import com.nikgapps.app.utils.constants.ApplicationConstants
import com.nikgapps.app.utils.worker.DownloadWorker
import java.io.File

@Composable
fun DownloadNikGappsCard() {
    val downloadPrefs = globalClass.downloadManager.downloadPrefs
    val dialog = globalClass.singleChoiceDialog
    var variant by remember { mutableStateOf(GappsVariantPreference.entries[downloadPrefs.gappsVariant].toVariantString()) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val workManager = WorkManager.getInstance(context)
    var isDownloading by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title Section
            Text(
                text = "Download NikGapps",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF6200EA),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Variant Selection Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        dialog.show(
                            title = globalClass.getString(R.string.gapps_variant),
                            description = globalClass.getString(R.string.select_variant_preference),
                            choices = GappsVariantPreference.entries.map { it.toVariantString() },
                            selectedChoice = downloadPrefs.gappsVariant,
                            onSelect = {
                                variant = GappsVariantPreference.entries[it].toVariantString()
                                downloadPrefs.gappsVariant = it
                            }
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Gapps Variant",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = variant,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.Black
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Download Action Section
            if (isDownloading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = Color(0xFF6200EA)
                )
            } else {
                Button(
                    onClick = {
                        isDownloading = true
                        val downloadUrl =
                            ApplicationConstants.getDownloadUrl(variant.toString().lowercase())
                        val zipFileName = downloadUrl.split("/").lastOrNull { it.endsWith(".zip") }
                            ?: throw IllegalArgumentException("No .zip file found in URL")
                        val zipFileNameWithoutExtension = zipFileName.removeSuffix(".zip")
                        val destFilePath =
                            "${Environment.getExternalStorageDirectory().absolutePath}/Download/$zipFileNameWithoutExtension.zip"

                        val zipFile = File(destFilePath)

                        if (!zipFile.exists()) {
                            val inputData = workDataOf(
                                DownloadWorker.DOWNLOAD_URL_KEY to downloadUrl,
                                DownloadWorker.DEST_FILE_PATH_KEY to destFilePath,
                                DownloadWorker.DOWNLOAD_TYPE_KEY to DownloadWorker.DOWNLOAD_TYPE_FILE
                            )

                            val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                                .setInputData(inputData)
                                .setConstraints(
                                    Constraints.Builder()
                                        .setRequiredNetworkType(NetworkType.CONNECTED)
                                        .build()
                                )
                                .build()

                            workManager.enqueue(downloadRequest)

                            workManager.getWorkInfoByIdLiveData(downloadRequest.id)
                                .observe(lifecycleOwner) { workInfo ->
                                    if (workInfo != null && workInfo.state.isFinished) {
                                        if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                                            Toast.makeText(
                                                context,
                                                "File downloaded successfully",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else if (workInfo.state == WorkInfo.State.FAILED) {
                                            Toast.makeText(
                                                context,
                                                "Failed to download file",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        isDownloading = false
                                    }
                                }
                        } else {
                            Toast.makeText(
                                context,
                                "${zipFile.name} already present!",
                                Toast.LENGTH_LONG
                            ).show()
                            isDownloading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6200EA),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Download NikGapps $variant")
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewDownloadNikGappsCard() {
    NikGappsThemePreview {
        DownloadNikGappsCard()
    }
}