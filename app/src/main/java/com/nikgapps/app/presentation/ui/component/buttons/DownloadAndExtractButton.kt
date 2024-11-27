package com.nikgapps.app.presentation.ui.component.buttons

import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nikgapps.App
import com.nikgapps.app.presentation.ui.viewmodel.ProgressLogViewModel
import com.nikgapps.app.utils.ZipUtility
import com.nikgapps.app.utils.root.RootUtility
import com.nikgapps.app.utils.constants.ApplicationConstants
import com.nikgapps.app.utils.worker.DownloadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.measureTimeMillis

@Composable
fun DownloadAndExtractButton() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isProcessing by remember { mutableStateOf(false) } // State to indicate if process is ongoing
    var progressText by remember { mutableStateOf("Download, Extract, and Copy File") }
    val workManager = WorkManager.getInstance(context)

    Column {
        Button(onClick = {
            isProcessing = true
            progressText = "Downloading and extracting file..."

            val zipFileName = ApplicationConstants.DOWNLOAD_URL.split("/").lastOrNull { it.endsWith(".zip") }
                ?: throw IllegalArgumentException("No .zip file found in URL")
            val zipFileNameWithoutExtension = zipFileName.removeSuffix(".zip")
            val destFilePath =
                "${Environment.getExternalStorageDirectory().absolutePath}/Download/$zipFileNameWithoutExtension.zip"
            val outputDirPath =
                "${Environment.getExternalStorageDirectory().absolutePath}/Download/$zipFileNameWithoutExtension"

            val zipFile = File(destFilePath)

            if (!zipFile.exists()) {
                // Download file using DownloadWorker
                progressText = "Downloading file..."
                val inputData = workDataOf(
                    DownloadWorker.DOWNLOAD_URL_KEY to ApplicationConstants.DOWNLOAD_URL,
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

                // Enqueue the download work request
                workManager.enqueue(downloadRequest)

                // Observe the work status on the main thread
                workManager.getWorkInfoByIdLiveData(downloadRequest.id).observe(lifecycleOwner) { workInfo ->
                    if (workInfo != null && workInfo.state.isFinished) {
                        if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                            progressText = "File downloaded successfully"
                            Log.d("NikGapps-DownloadAndExtractButton", "File downloaded successfully")
                            // Proceed with extraction and copying after download is complete
//                            handleExtractionAndCopy(
//                                context,
//                                destFilePath,
//                                outputDirPath,
//                                { progress -> progressText = progress },
//                                { processing -> isProcessing = processing }
//                            )
                        } else if (workInfo.state == WorkInfo.State.FAILED) {
                            // Handle the failure of the download
                            progressText = "Failed to download file"
                            Log.e("NikGapps-DownloadAndExtractButton", "Failed to download file")
                            Toast.makeText(context, "Failed to download file", Toast.LENGTH_LONG).show()
                            isProcessing = false
                        }
                    }
                }
            } else {
                Log.d("NikGapps-DownloadAndExtractButton", "File already exists, skipping download")
                progressText = "File already exists, skipping download"
                // Proceed with extraction and copying if file already exists
//                handleExtractionAndCopy(
//                    context,
//                    destFilePath,
//                    outputDirPath,
//                    { progress -> progressText = progress },
//                    { processing -> isProcessing = processing }
//                )
            }
        }) {
            // Button text
            Text(text = progressText)
        }
    }
}

private fun handleExtractionAndCopy(
    viewModel: ProgressLogViewModel = ProgressLogViewModel(),
    context: Context,
    destFilePath: String,
    outputDirPath: String,
    updateProgressText: (String) -> Unit,
    updateProcessingState: (Boolean) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            updateProgressText("Extracting file...")
            val extractionTime = measureTimeMillis {
                val extractSuccessful = ZipUtility.extractZip(
                    viewModel,
                    destFilePath,
                    extractNestedZips = true,
                    progressCallback = { progress ->
                        updateProgressText(progress)
                    }
                )

                if (extractSuccessful) {
                    updateProgressText("File extracted successfully")
                    Log.d("NikGapps-DownloadAndExtractButton", "File extracted successfully")
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to extract file", Toast.LENGTH_LONG).show()
                    }
                    updateProcessingState(false)
                    return@launch
                }
            }
            Log.d("NikGapps-DownloadAndExtractButton", "Extraction completed in $extractionTime ms")

            // Copy extracted files to system/product partitions
            if (App.hasRootAccess) {
                val copySuccessful = RootUtility.copyFilesToSystem(outputDirPath, "/product/app/NikGapps/")
                withContext(Dispatchers.Main) {
                    if (copySuccessful) {
                        Toast.makeText(context, "Files copied successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to copy files", Toast.LENGTH_LONG).show()
                        Log.e("NikGapps-DownloadAndExtractButton", "Failed to copy files")
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Root access not available", Toast.LENGTH_SHORT).show()
                    Log.e("NikGapps-DownloadAndExtractButton", "Root access not available")
                }
            }
        } catch (e: Exception) {
            Log.e("NikGapps-DownloadAndExtractButton", "Error: ${e.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } finally {
            updateProcessingState(false) // Reset processing state
        }
    }
}

