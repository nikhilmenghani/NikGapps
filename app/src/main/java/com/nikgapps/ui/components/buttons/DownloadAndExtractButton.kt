package com.nikgapps.ui.components.buttons

import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.nikgapps.App
import com.nikgapps.utils.Constants
import com.nikgapps.utils.DownloadUtility
import com.nikgapps.utils.ZipUtility
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun DownloadAndExtractButton() {
    val context = LocalContext.current

    Button(onClick = {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val destFilePath = "${Environment.getExternalStorageDirectory().absolutePath}/Download/file.zip"
                val outputDirPath = "${Environment.getExternalStorageDirectory().absolutePath}/Download/extracted"

                val zipFile = File(destFilePath)

                // Step 1: Check if file already exists
                if (!zipFile.exists()) {
                    // Download the file using DownloadUtility
                    val downloadSuccessful = DownloadUtility.downloadFile(Constants.DOWNLOAD_URL, destFilePath)

                    if (downloadSuccessful) {
                        Log.d("DownloadAndExtractButton", "File downloaded successfully")
                    } else {
                        Log.e("DownloadAndExtractButton", "Failed to download file")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed to download file", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }

                    // Wait for a few seconds to ensure the download is complete (SourceForge-specific behavior)
                    delay(5000)
                } else {
                    Log.d("DownloadAndExtractButton", "File already exists, skipping download")
                }

                // Step 2: Extract the zip file using ZipUtility
                val extractSuccessful = ZipUtility.extractZip(destFilePath, outputDirPath, listOf(".zip"))

                if (extractSuccessful) {
                    Log.d("DownloadAndExtractButton", "File extracted successfully")
                } else {
                    Log.e("DownloadAndExtractButton", "Failed to extract file")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to extract file", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // Step 3: Copy extracted files to system or product partitions
                if (App.hasRootAccess) {
                    val destPath = "/product/app/NikGapps/" // Example destination path
                    val result = Shell.cmd("cp -r ${outputDirPath}/* $destPath").exec()
                    val output = result.out.joinToString("\n")
                    Log.d("DownloadAndExtractButton", "Shell script output: \n$output")

                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            Toast.makeText(context, "Files copied successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to copy files: $output", Toast.LENGTH_LONG).show()
                            Log.e("DownloadAndExtractButton", "Failed to copy files: $output")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Root access not available", Toast.LENGTH_SHORT).show()
                        Log.e("DownloadAndExtractButton", "Root access not available")
                    }
                }

            } catch (e: Exception) {
                Log.e("DownloadAndExtractButton", "Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }) {
        // Button text
        Text(text = "Download, Extract, and Copy File")
    }
}