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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CopyFileButton() {
    val context = LocalContext.current

    Button(onClick = {
        CoroutineScope(Dispatchers.IO).launch {
            if (App.hasRootAccess) {
                Log.d("CopyFileButton", "Root access confirmed")
                val sourcePath = "${Environment.getExternalStorageDirectory().absolutePath}/Download/NikGapps.apk"
                val destPath = "/product/app/NikGapps/NikGapps.apk"

                // Check if the /product partition exists
                val partitionCheckResult = Shell.cmd("[ -d /product ] && echo 'exists' || echo 'not_exists'").exec()
                val partitionExists = partitionCheckResult.out.contains("exists")
                Log.d("CopyFileButton", "Partition check output: ${partitionCheckResult.out.joinToString("\n")}")

                if (partitionExists) {
                    // Remount /product as read-write
                    val remountResult = Shell.cmd("mount -o rw,remount /product 2>&1").exec()
                    Log.d("CopyFileButton", "Remount output: ${remountResult.out.joinToString("\n")}")

                    if (remountResult.isSuccess) {
                        // Create necessary directories
                        val mkdirResult = Shell.cmd("mkdir -p /product/app/NikGapps").exec()
                        Log.d("CopyFileButton", "Directory creation output: ${mkdirResult.out.joinToString("\n")}")
                        val mkdirOutput = mkdirResult.out.joinToString("\n")

                        if (mkdirResult.isSuccess) {
                            // Copy the file using dd
                            val ddResult = Shell.cmd("dd if=$sourcePath of=$destPath bs=1M 2>&1").exec()
                            Log.d("CopyFileButton", "dd command output: ${ddResult.out.joinToString("\n")}")
                            val output = ddResult.out.joinToString("\n")

                            // Check if the file was copied
                            val fileCheckResult = Shell.cmd("ls -l $destPath").exec()
                            Log.d("CopyFileButton", "File check output: ${fileCheckResult.out.joinToString("\n")}")

                            withContext(Dispatchers.Main) {
                                if (ddResult.isSuccess) {
                                    Toast.makeText(context, "File copied successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to copy file: $output. Folder creation output: $mkdirOutput", Toast.LENGTH_LONG).show()
                                    Log.e("CopyFileButton", "Failed to copy file: $output. Folder creation output: $mkdirOutput")
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Failed to create directory: $mkdirOutput", Toast.LENGTH_LONG).show()
                                Log.e("CopyFileButton", "Failed to create directory: $mkdirOutput")
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed to remount /product as read-write", Toast.LENGTH_SHORT).show()
                            Log.e("CopyFileButton", "Failed to remount /product as read-write")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Partition /product does not exist", Toast.LENGTH_SHORT).show()
                        Log.e("CopyFileButton", "Partition /product does not exist")
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Root access not available", Toast.LENGTH_SHORT).show()
                    Log.e("CopyFileButton", "Root access not available")
                }
            }
        }
    }) {
        Text(text = "Copy File to System Partition")
    }
}