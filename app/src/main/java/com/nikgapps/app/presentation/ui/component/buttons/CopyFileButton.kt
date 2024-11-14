package com.nikgapps.app.presentation.ui.component.buttons

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
import java.io.File
import com.nikgapps.R

@Composable
fun CopyFileButton() {
    val context = LocalContext.current

    Button(onClick = {
        CoroutineScope(Dispatchers.IO).launch {
            if (App.hasRootAccess) {
                Log.d("NikGapps-CopyFileButton", "Root access confirmed")
                val sourcePath = "${Environment.getExternalStorageDirectory().absolutePath}/Download/NikGapps.apk"
                val destPath = "/product/app/NikGapps/NikGapps.apk"

                // Load the script from raw resources
                val inputStream = context.resources.openRawResource(R.raw.copy_file_script)
                val scriptFile = File(context.filesDir, "copy_file_script.sh")
                scriptFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }

                // Change permission to executable
                scriptFile.setExecutable(true)

                // Execute the shell script with root privileges
                val result = Shell.cmd("sh ${scriptFile.absolutePath} $sourcePath $destPath").exec()
                val output = result.out.joinToString("\n")
                Log.d("NikGapps-CopyFileButton", "Shell script output: \n$output")

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        Toast.makeText(context, "File copied successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to copy file: $output", Toast.LENGTH_LONG).show()
                        Log.e("NikGapps-CopyFileButton", "Failed to copy file: $output")
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Root access not available", Toast.LENGTH_SHORT).show()
                    Log.e("NikGapps-CopyFileButton", "Root access not available")
                }
            }
        }
    }) {
        Text(text = "Copy File to System Partition")
    }
}