package com.nikgapps.ui.components.buttons

import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.nikgapps.App
import com.nikgapps.utils.Constants
import com.nikgapps.utils.DownloadUtility
import com.nikgapps.utils.ZipUtility
import com.nikgapps.utils.RootUtility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.measureTimeMillis

@Composable
fun DownloadAndExtractButton() {
    val context = LocalContext.current
    var isProcessing by remember { mutableStateOf(false) } // State to indicate if process is ongoing
    var progressText by remember { mutableStateOf("Download, Extract, and Copy File") }

    Column {
        Button(onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    isProcessing = true
                    progressText = "Downloading and extracting file..."
                    val zipFileName =
                        Constants.DOWNLOAD_URL.split("/").lastOrNull { it.endsWith(".zip") }
                            ?: throw IllegalArgumentException("No .zip file found in URL")

                    val zipFileNameWithoutExtension = zipFileName.removeSuffix(".zip")

                    val destFilePath =
                        "${Environment.getExternalStorageDirectory().absolutePath}/Download/$zipFileNameWithoutExtension.zip"
                    val outputDirPath =
                        "${Environment.getExternalStorageDirectory().absolutePath}/Download/$zipFileNameWithoutExtension"

                    val zipFile = File(destFilePath)

                    // Step 1: Check if file already exists
                    if (!zipFile.exists()) {
                        progressText = "Downloading file..."
                        // Download the file using DownloadUtility
                        val downloadSuccessful =
                            DownloadUtility.downloadFile(Constants.DOWNLOAD_URL, destFilePath)

                        if (downloadSuccessful) {
                            Log.d("DownloadAndExtractButton", "File downloaded successfully")
                        } else {
                            progressText = "Failed to download file"
                            Log.e("DownloadAndExtractButton", "Failed to download file")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Failed to download file",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            isProcessing = false
                            return@launch
                        }

                        // Wait for a few seconds to ensure the download is complete (SourceForge-specific behavior)
                        delay(5000)
                    } else {
                        Log.d("DownloadAndExtractButton", "File already exists, skipping download")
                    }

                    // Step 2: Extract the zip file using ZipUtility
                    val extractionTime = measureTimeMillis {
                        progressText = "Extracting file..."
                        val extractSuccessful = ZipUtility.extractZip(destFilePath,
                            outputDirPath,
                            extractNestedZips = true,
                            progressCallback = { progress ->
                                progressText = progress
                            })

                        if (extractSuccessful) {
                            progressText = "File extracted successfully"
                            Log.d("DownloadAndExtractButton", "File extracted successfully")
                        } else {
                            Log.e("DownloadAndExtractButton", "Failed to extract file")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Failed to extract file", Toast.LENGTH_LONG)
                                    .show()
                            }
                            isProcessing = false
                            return@launch
                        }
                    }
                    Log.d("DownloadAndExtractButton", "Extraction completed in $extractionTime ms")

                    // Step 3: Copy extracted files to system or product partitions
                    if (App.hasRootAccess) {
                        val copySuccessful =
                            RootUtility.copyFilesToSystem(outputDirPath, "/product/app/NikGapps/")
                        withContext(Dispatchers.Main) {
                            if (copySuccessful) {
                                // Show success message if files are copied successfully
                                Toast.makeText(
                                    context,
                                    "Files copied successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                // Show error message if copying files failed
                                Toast.makeText(context, "Failed to copy files", Toast.LENGTH_LONG)
                                    .show()
                                Log.e("DownloadAndExtractButton", "Failed to copy files")
                            }
                        }
                    } else {
                        // Show error message if root access is not available
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Root access not available", Toast.LENGTH_SHORT)
                                .show()
                            Log.e("DownloadAndExtractButton", "Root access not available")
                        }
                    }
                } catch (e: Exception) {
                    // Log and show error message if an exception occurs
                    Log.e("DownloadAndExtractButton", "Error: ${e.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    isProcessing = false // Reset processing state
                }
            }
        }) {
            // Button text
            Text(text = progressText)
        }
    }
}