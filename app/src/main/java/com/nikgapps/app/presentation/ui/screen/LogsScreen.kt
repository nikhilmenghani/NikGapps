package com.nikgapps.app.presentation.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.nikgapps.app.data.model.LogManager
import com.nikgapps.app.utils.ZipUtility
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fileParent = File(LogManager.APP_LOGS_FILE_NAME).parentFile
    val filename = fileParent?.absolutePath + "/NikGapps_logs_${System.currentTimeMillis()}.zip"

    LaunchedEffect(Unit) {
        scope.launch {
            while (true) {
                LogManager.loadLogs()
                delay(3000) // Poll every second
            }
        }
    }

    val logs by remember { derivedStateOf { LogManager.logs } }
    val scrollState = rememberScrollState()

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FloatingActionButton(onClick = {

                    val result = ZipUtility.saveLogs(filename)
                    if (result) {
                        Toast.makeText(context, "Logs saved successfully to $filename", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to save logs", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = "Save Logs")
                }

                Spacer(modifier = Modifier.height(16.dp))

                FloatingActionButton(onClick = {
                    scope.launch {
                        LogManager.clearLogs()
                    }
                }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Logs")
                }

                Spacer(modifier = Modifier.height(16.dp))

                FloatingActionButton(onClick = {
                    val result = ZipUtility.saveLogs(filename)
                    if (result) {
                        val file = File(filename)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/zip"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share logs via"))
                    } else {
                        Toast.makeText(context, "Failed to save logs", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share Logs")
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