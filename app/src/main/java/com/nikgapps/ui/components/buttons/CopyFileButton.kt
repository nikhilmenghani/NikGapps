package com.nikgapps.ui.components.buttons

import android.os.Environment
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
                val sourcePath = "${Environment.getExternalStorageDirectory().absolutePath}/Download/NikGapps.apk"
                val destPath = "/product/app/NikGapps/NikGapps.apk"

                // Check if the /product partition exists
                val partitionCheckResult = Shell.cmd("[ -d /product ] && echo 'exists' || echo 'not_exists'").exec()
                val partitionExists = partitionCheckResult.out.contains("exists")

                if (partitionExists) {
                    // Remount /product as read-write
                    val remountResult = Shell.cmd("mount -o rw,remount /product").exec()
                    val remountOutput = remountResult.out.joinToString("\n")

                    if (remountResult.isSuccess) {
                        // Create necessary directories
                        val mkdirResult = Shell.cmd("mkdir -p /product/app/NikGapps").exec()
                        val mkdirOutput = mkdirResult.out.joinToString("\n")

                        if (mkdirResult.isSuccess) {
                            // Copy the file
                            val result = Shell.cmd("cp $sourcePath $destPath").exec()
                            val output = result.out.joinToString("\n")

                            withContext(Dispatchers.Main) {
                                if (result.isSuccess) {
                                    Toast.makeText(context, "File copied successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to copy file: $output. Folder creation output: $mkdirOutput", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Failed to create directory: $mkdirOutput", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed to remount /product as read-write: $remountOutput", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Partition /product does not exist", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Root access not available", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }) {
        Text(text = "Copy File to System Partition")
    }
}
