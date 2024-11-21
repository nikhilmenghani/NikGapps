package com.nikgapps.app.presentation.ui.screen

import android.annotation.SuppressLint
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikgapps.app.data.model.LogManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen() {
    val context = LocalContext.current
    val logs by remember { derivedStateOf { LogManager.logs } }
    val scrollState = rememberScrollState()

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FloatingActionButton(onClick = {
                    // Save logs to SD card
                    val directory = Environment.getExternalStorageDirectory()
                    val file = File(directory, "logs.txt")
                    try {
                        FileOutputStream(file).use { fos ->
                            logs.forEach { log ->
                                fos.write((log + "\n").toByteArray())
                            }
                        }
                        Toast.makeText(context, "Logs saved to ${file.absolutePath}", Toast.LENGTH_SHORT).show()
                    } catch (e: IOException) {
                        Toast.makeText(context, "Failed to save logs: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Save Logs")
                }

                Spacer(modifier = Modifier.height(16.dp))

                FloatingActionButton(onClick = {
                    LogManager.clearLogs()
                }) {
                    Text("Clear Logs")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Column {
                logs.forEach { log ->
                    Text(
                        text = log,
                        style = TextStyle(color = Color.White, fontSize = 14.sp)
                    )
                }
            }
        }
    }
}