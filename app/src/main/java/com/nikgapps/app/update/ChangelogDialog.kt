package com.nikgapps.app.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nikgapps.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ChangelogDialog(title: String, entries: List<ChangelogEntry>, loading: Boolean = false,
    onDismiss: () -> Unit, onUpdate: (() -> Unit)? = null) {
    val context = LocalContext.current
    val installedVersion = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    var cachedEntries by remember { mutableStateOf<List<ChangelogEntry>?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { cachedEntries = ChangelogRepository.fetch(context) }
    val source = cachedEntries ?: entries
    val releases = remember(source, entries, onUpdate != null) {
        val visible = if (onUpdate == null) source else source.filter { item -> entries.any { it.version == item.version } }
        visible.sortedWith { a, b -> ChangelogRepository.compareVersions(b.version, a.version) }
    }
    val maxHeight = (LocalConfiguration.current.screenHeightDp * .85f).dp
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.94f).height(maxHeight), shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface) {
            Column {
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.nikgapps_logo), null,
                        Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                    Column(Modifier.weight(1f)) {
                        Text("ChangeLog", style = MaterialTheme.typography.titleLarge)
                        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("NikGapps", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Text("v$installedVersion", Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
                LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false),
                    contentPadding = PaddingValues(16.dp)) {
                    when {
                        releases.isEmpty() && (loading || cachedEntries == null) -> item { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Loading changelog…", Modifier.padding(top = 12.dp)) }
                        releases.isEmpty() -> item { Text("No changelog entries are available.") }
                        else -> itemsIndexed(releases, key = { _, release -> release.version }) { index, release ->
                            ReleaseTimelineItem(release, index == 0, index == releases.lastIndex,
                                release.version.removePrefix("v") == installedVersion.removePrefix("v").substringBefore('-'))
                        }
                    }
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 10.dp),
                        enabled = !refreshing, onClick = {
                        scope.launch {
                            refreshing = true
                            try { cachedEntries = ChangelogRepository.fetch(context, forceRefresh = true, reportErrors = true) }
                            catch (cancelled: CancellationException) { throw cancelled }
                            catch (error: Exception) { Toast.makeText(context, "Unable to refresh changelog. Cached entries kept.", Toast.LENGTH_LONG).show() }
                            finally { refreshing = false }
                        }
                    }) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (refreshing) "Refreshing…" else "Refresh", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                    FilledTonalButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (onUpdate == null) "Close" else "Later", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                    onUpdate?.let { action ->
                        FilledTonalButton(onClick = action, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) {
                            Text("Update now", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseTimelineItem(entry: ChangelogEntry, newest: Boolean, last: Boolean, installed: Boolean) {
    var expanded by rememberSaveable(entry.version) { mutableStateOf(newest) }
    var showAll by rememberSaveable(entry.version) { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing), label = "releaseChevron")
    val timelineColor = MaterialTheme.colorScheme.outlineVariant
    // Intrinsic height follows the target content size, bypassing the accordion's animated
    // measurement. Draw the timeline using the actual measured height instead.
    Row(Modifier.fillMaxWidth().drawBehind {
        if (!last && size.height > 28.dp.toPx()) drawLine(timelineColor,
            Offset(12.dp.toPx(), 28.dp.toPx()), Offset(12.dp.toPx(), size.height), 2.dp.toPx())
    }) {
        Column(Modifier.width(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.size(10.dp).clip(CircleShape).background(
                if (installed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))
        }
        Column(Modifier.weight(1f).padding(start = 8.dp, bottom = 16.dp)) {
            Row(Modifier.fillMaxWidth().clickable(interactionSource = null, indication = null) { expanded = !expanded }
                .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("v${entry.version}", style = MaterialTheme.typography.titleMedium)
                        if (installed) Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text("Installed", Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    val date = entry.date?.let { LocalDate.parse(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) }
                    Text(listOfNotNull(date, "${entry.changes.size} ${if (entry.changes.size == 1) "change" else "changes"}").joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.ExpandMore,
                    if (expanded) "Collapse v${entry.version}" else "Expand v${entry.version}",
                    Modifier.rotate(chevronRotation))
            }
            AnimatedVisibility(expanded,
                enter = expandVertically(tween(280, easing = FastOutSlowInEasing), expandFrom = Alignment.Top) + fadeIn(tween(240)),
                exit = shrinkVertically(tween(280, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top) + fadeOut(tween(240))) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.fillMaxWidth().animateContentSize(tween(280, easing = FastOutSlowInEasing))) {
                        (if (showAll) entry.changes else entry.changes.take(5)).forEachIndexed { index, change ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("•", color = MaterialTheme.colorScheme.primary)
                                Text(change, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        if (entry.changes.size > 5) TextButton(onClick = { showAll = !showAll }) {
                            Text(if (showAll) "Show less" else "Show all (${entry.changes.size})")
                        }
                    }
                }
            }
        }
    }
}
