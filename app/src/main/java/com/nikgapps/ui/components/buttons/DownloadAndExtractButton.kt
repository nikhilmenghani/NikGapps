package com.nikgapps.ui.components.buttons

import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.nikgapps.App
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

@Composable
fun DownloadAndExtractButton() {
    val context = LocalContext.current
    val client = OkHttpClient()

    Button(onClick = {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Step 1: Download the file from SourceForge
                val downloadPageUrl = "https://sourceforge.net/projects/nikgapps-beta/files/Releases/Android-15/18-Oct-2024/NikGapps-core-arm64-15-20241018-signed.zip/download"
                val destFilePath = "${Environment.getExternalStorageDirectory().absolutePath}/Download/file.zip"
                val request = Request.Builder().url(downloadPageUrl).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    response.body?.let { responseBody ->
                        val file = File(destFilePath)
                        file.sink().buffer().use { bufferedSink ->
                            bufferedSink.writeAll(responseBody.source())
                        }
                    }
                    Log.d("DownloadAndExtractButton", "File downloaded successfully")
                } else {
                    Log.e("DownloadAndExtractButton", "Failed to download file: ${response.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to download file", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // Wait for a few seconds to ensure the download is complete (SourceForge-specific behavior)
                delay(5000)

                // Step 2: Extract the zip file
                val zipFile = File(destFilePath)
                val outputDir = File(Environment.getExternalStorageDirectory().absolutePath + "/Download/extracted")
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }

                ZipInputStream(zipFile.inputStream()).use { zipStream ->
                    var entry = zipStream.nextEntry
                    while (entry != null) {
                        val extractedFile = File(outputDir, entry.name)
                        Log.d("DownloadAndExtractButton", "Extracted file: ${extractedFile.absolutePath}")
                        if (entry.isDirectory) {
                            Log.d("DownloadAndExtractButton", "${extractedFile.absolutePath} is a directory")
                            extractedFile.mkdirs()
                        } else {
                            Log.d("DownloadAndExtractButton", "${extractedFile.absolutePath} is a file")
                            // make sure the parent directory exists
                            extractedFile.parentFile?.mkdirs()
                            FileOutputStream(extractedFile).use { outputStream ->
                                zipStream.copyTo(outputStream)
                            }
                        }
                        zipStream.closeEntry()
                        entry = zipStream.nextEntry
                    }
                }
                Log.d("DownloadAndExtractButton", "File extracted successfully")

//                // Step 3: Copy extracted files to system or product partitions
//                if (App.hasRootAccess) {
//                    val destPath = "/product/app/NikGapps/" // Example destination path
//                    val result = Shell.cmd("cp -r ${outputDir.absolutePath}/* $destPath").exec()
//                    val output = result.out.joinToString("\n")
//                    Log.d("DownloadAndExtractButton", "Shell script output: \n$output")
//
//                    withContext(Dispatchers.Main) {
//                        if (result.isSuccess) {
//                            Toast.makeText(context, "Files copied successfully", Toast.LENGTH_SHORT).show()
//                        } else {
//                            Toast.makeText(context, "Failed to copy files: $output", Toast.LENGTH_LONG).show()
//                            Log.e("DownloadAndExtractButton", "Failed to copy files: $output")
//                        }
//                    }
//                } else {
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(context, "Root access not available", Toast.LENGTH_SHORT).show()
//                        Log.e("DownloadAndExtractButton", "Root access not available")
//                    }
//                }

            } catch (e: Exception) {
                Log.e("DownloadAndExtractButton", "Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }) {
        Text(text = "Download, Extract, and Copy File")
    }
}
