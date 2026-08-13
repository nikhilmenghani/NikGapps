package com.nikgapps.app.presentation.ui.screen

import android.content.Intent
import android.os.Build
import android.os.Process
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

private const val MAX_LOG_LINES = 10_000
private const val LOGCAT_BATCH_INTERVAL_MS = 250L
internal data class LogcatEntry(val raw: String, val date: String, val time: String,
    val level: String, val tag: String, val message: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var lines by remember { mutableStateOf<List<LogcatEntry>>(emptyList()) }
    var paused by remember { mutableStateOf(false) }
    var session by remember { mutableIntStateOf(0) }
    var captureError by remember { mutableStateOf<String?>(null) }
    val pausedState by rememberUpdatedState(paused)
    val clearGeneration = remember { AtomicInteger(0) }

    LaunchedEffect(session) {
        captureError = null
        val incoming = Channel<Pair<Int, LogcatEntry>>(capacity = 4_096, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        var process: java.lang.Process? = null
        try {
            process = withContext(Dispatchers.IO) {
                ProcessBuilder(
                    "logcat",
                    "--pid=${Process.myPid()}",
                    "-v", "threadtime",
                    "-T", MAX_LOG_LINES.toString(),
                    "NikGappsFlow:I", "*:W"
                )
                    .redirectErrorStream(true).start()
            }
            coroutineScope {
                val reader = launch(Dispatchers.IO) {
                    process.inputStream.bufferedReader().useLines { output ->
                        output.forEach { raw ->
                            val entry = parseLogcatLine(raw)
                            if (!isViewerRenderNoise(entry)) incoming.trySend(clearGeneration.get() to entry)
                        }
                    }
                }
                try {
                    while (isActive && reader.isActive) {
                        delay(LOGCAT_BATCH_INTERVAL_MS)
                        val batch = buildList {
                            while (size < 1_024) {
                                val item = incoming.tryReceive().getOrNull() ?: break
                                if (item.first == clearGeneration.get()) add(item.second)
                            }
                        }
                        if (!pausedState && batch.isNotEmpty()) lines = appendLogBatch(lines, batch)
                    }
                    reader.join()
                } finally {
                    process.destroy()
                    reader.cancel()
                }
            }
        } catch (e: Exception) {
            captureError = e.message ?: "Unable to read app Logcat"
        } finally {
            incoming.close()
            process?.destroy()
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
                        lines.forEach { writer.appendLine(it.raw) }
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
        IconButton(onClick = {
            clearGeneration.incrementAndGet()
            lines = emptyList()
        }) { Icon(Icons.Default.DeleteSweep, "Clear captured logs") }
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
                        itemsIndexed(lines) { index, entry ->
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                if (index == 0 || lines[index - 1].date != entry.date) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                                        Text(entry.date, Modifier.padding(horizontal = 10.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontFamily = FontFamily.Monospace)
                                        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top) {
                                    Text(entry.time.substringBeforeLast('.'), modifier = Modifier.widthIn(max = 58.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Surface(shape = RoundedCornerShape(6.dp), color = logColor(entry.level).copy(alpha = .16f),
                                        contentColor = logColor(entry.level)) {
                                        Text(entry.level, Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        if (entry.tag.isNotBlank()) Text(entry.tag, style = MaterialTheme.typography.labelSmall,
                                            color = logColor(entry.level), fontFamily = FontFamily.Monospace)
                                        Text(entry.message, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace)
                                    }
                                }
                                if (index != lines.lastIndex) HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("${lines.size.coerceAtMost(MAX_LOG_LINES)} of $MAX_LOG_LINES lines · actions, warnings and errors",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun logColor(level: String) = when (level) {
    "E", "F" -> MaterialTheme.colorScheme.error
    "W" -> MaterialTheme.colorScheme.tertiary
    "I" -> MaterialTheme.colorScheme.primary
    "D" -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.outline
}

private val THREADTIME_LOG = Regex("""^(\d{2}-\d{2})\s+(\d{2}:\d{2}:\d{2}\.\d{3})\s+\d+\s+\d+\s+([VDIWEF])\s+([^:]+):\s?(.*)$""")

internal fun parseLogcatLine(raw: String): LogcatEntry {
    val match = THREADTIME_LOG.matchEntire(raw)
    return if (match != null) {
        val (date, time, level, tag, message) = match.destructured
        LogcatEntry(raw, date, time, level, tag.trim(), message)
    } else {
        LogcatEntry(raw, "Session", "", "·", "", raw)
    }
}

internal fun appendLogBatch(current: List<LogcatEntry>, batch: List<LogcatEntry>): List<LogcatEntry> =
    (current + batch).takeLast(MAX_LOG_LINES)

/** Framework draw diagnostics caused by rendering this viewer must not become viewer input. */
internal fun isViewerRenderNoise(entry: LogcatEntry): Boolean =
    (entry.tag == "View" && (
        entry.message.contains("setRequestedFrameRate") ||
            entry.message.contains("updateDisplayListIfDirty") ||
            entry.message.contains("recreateChildDisplayList")
        )) ||
        (entry.tag.startsWith("VRI[") && entry.message.contains("Requested frameRateCategory"))
