package com.nikgapps.app.presentation.ui.component.cards

import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Adb
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.nikgapps.app.presentation.theme.NikGappsThemePreview
import com.nikgapps.app.presentation.ui.component.items.PreferenceItem
import com.nikgapps.app.utils.constants.ApplicationConstants
import com.nikgapps.app.utils.worker.DownloadWorker
import java.io.File

@Composable
fun DownloadNikGappsCard() {
    val downloadPrefs = globalClass.downloadManager.downloadPrefs
    val dialog = globalClass.singleChoiceDialog
    var isProcessing by remember { mutableStateOf(false) }
    var variant by remember { mutableStateOf("") }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val workManager = WorkManager.getInstance(context)

    // Update the variant string based on the selected gappsVariant
    variant = when (downloadPrefs.gappsVariant) {
        GappsVariantPreference.CORE.ordinal -> stringResource(R.string.core)
        GappsVariantPreference.BASIC.ordinal -> stringResource(R.string.basic)
        GappsVariantPreference.OMNI.ordinal -> stringResource(R.string.omni)
        GappsVariantPreference.STOCK.ordinal -> stringResource(R.string.stock)
        GappsVariantPreference.FULL.ordinal -> stringResource(R.string.full)
        else -> stringResource(R.string.core)
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
            PreferenceItem(
                label = stringResource(R.string.gapps_variant),
                supportingText = variant,
                icon = Icons.Rounded.Adb,
                onClick = {
                    dialog.show(
                        title = globalClass.getString(R.string.gapps_variant),
                        description = globalClass.getString(R.string.select_variant_preference),
                        choices = listOf(
                            globalClass.getString(R.string.core),
                            globalClass.getString(R.string.basic),
                            globalClass.getString(R.string.omni),
                            globalClass.getString(R.string.stock),
                            globalClass.getString(R.string.full)
                        ),
                        selectedChoice = downloadPrefs.gappsVariant,
                        onSelect = {
                            variant = when (it) {
                                GappsVariantPreference.CORE.ordinal -> "Core"
                                GappsVariantPreference.BASIC.ordinal -> "Basic"
                                GappsVariantPreference.OMNI.ordinal -> "Omni"
                                GappsVariantPreference.STOCK.ordinal -> "Stock"
                                GappsVariantPreference.FULL.ordinal -> "Full"
                                else -> "Core"
                            }
                            downloadPrefs.gappsVariant = it
                        }
                    )
                }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = {
                    isProcessing = true
                    val downloadUrl =
                        ApplicationConstants.getDownloadUrl(variant.toString().lowercase())
                    val zipFileName = downloadUrl.split("/").lastOrNull { it.endsWith(".zip") }
                        ?: throw IllegalArgumentException("No .zip file found in URL")
                    Log.d("NikGapps-DownloadNikGappsCard", "Zip file name: $zipFileName")

                    val zipFileNameWithoutExtension = zipFileName.removeSuffix(".zip")
                    val destFilePath =
                        "${Environment.getExternalStorageDirectory().absolutePath}/Download/$zipFileNameWithoutExtension.zip"

                    val zipFile = File(destFilePath)

                    if (zipFile.exists()) {
                        zipFile.delete()
                        Toast.makeText(context, "Deleted existing file before proceeding", Toast.LENGTH_LONG).show()
                    }

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
                                        Log.d(
                                            "NikGapps-DownloadNikGappsCard",
                                            "File downloaded successfully"
                                        )
                                    } else if (workInfo.state == WorkInfo.State.FAILED) {
                                        Log.e(
                                            "NikGapps-DownloadNikGappsCard",
                                            "Failed to download file"
                                        )
                                        Toast.makeText(
                                            context,
                                            "Failed to download file",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        isProcessing = false
                                    }
                                }
                            }
                    } else {
                        Log.d("NikGapps-DownloadNikGappsCard", "File already downloaded")
                    }
                }) {
                    Text(text = stringResource(R.string.download) + " NikGapps $variant")
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