package com.nikgapps.app.presentation.ui.screen

import android.content.Intent
import android.os.Build
import android.os.Process
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_LOG_LINES = 2_000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val lines = remember { mutableStateListOf<String>() }
    var paused by remember { mutableStateOf(false) }
    var session by remember { mutableIntStateOf(0) }
    var captureError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(session) {
        captureError = null
        withContext(Dispatchers.IO) {
            var process: java.lang.Process? = null
            try {
                process = ProcessBuilder("logcat", "--pid=${Process.myPid()}", "-v", "threadtime", "-T", "200", "*:V")
                    .redirectErrorStream(true).start()
                process.inputStream.bufferedReader().useLines { output ->
                    output.forEach { line ->
                        if (!paused) withContext(Dispatchers.Main) {
                            lines += line
                            while (lines.size > MAX_LOG_LINES) lines.removeAt(0)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { captureError = e.message ?: "Unable to read app Logcat" }
            } finally { process?.destroy() }
        }
    }
    LaunchedEffect(lines.size, paused) {
        if (!paused && lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex)
    }

    fun exportLogs() {
        scope.launch {
            val file = withContext(Dispatchers.IO) {
                val directory = File(context.cacheDir, "diagnostics").apply { mkdirs() }
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                File(directory, "NikGapps_logcat_$stamp.txt").apply {
                    bufferedWriter().use { writer ->
                        writer.appendLine("NikGapps diagnostics")
                        writer.appendLine("Captured: ${Date()}")
                        writer.appendLine("App: ${context.packageName}")
                        writer.appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                        writer.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
                        writer.appendLine("Process ID: ${Process.myPid()}")
                        writer.appendLine("Lines: ${lines.size}")
                        writer.appendLine("-".repeat(72))
                        lines.forEach(writer::appendLine)
                    }
                }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"; putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "NikGapps diagnostics")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Export diagnostics"))
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Column {
        Text("App logs")
        Text("NikGapps process · PID ${Process.myPid()}", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    } }, actions = {
        IconButton(onClick = { paused = !paused }) {
            Icon(if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                if (paused) "Resume Logcat" else "Pause Logcat")
        }
        IconButton(onClick = { lines.clear(); session++ }) { Icon(Icons.Default.DeleteSweep, "Clear captured logs") }
        IconButton(onClick = ::exportLogs, enabled = lines.isNotEmpty()) { Icon(Icons.Default.IosShare, "Export diagnostics") }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Terminal, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(if (paused) "Capture paused" else "Live Logcat", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(50), color = if (paused) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.primaryContainer) {
                    Text(if (paused) "PAUSED" else "LIVE", Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(10.dp))
            Surface(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                when {
                    captureError != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp)); Text(captureError.orEmpty(), color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { session++ }) { Text("Retry") }
                        }
                    }
                    lines.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Waiting for NikGapps activity…", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> LazyColumn(Modifier.fillMaxSize().padding(12.dp), state = listState,
                        verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        items(lines) { line -> Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("›", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall,
                                color = logColor(line))
                            Text(line, Modifier.weight(1f), fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        } }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("${lines.size.coerceAtMost(MAX_LOG_LINES)} lines · only this app's process is captured",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun logColor(line: String) = when {
    " E " in line || " F " in line -> MaterialTheme.colorScheme.error
    " W " in line -> MaterialTheme.colorScheme.tertiary
    " I " in line -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.outline
}
