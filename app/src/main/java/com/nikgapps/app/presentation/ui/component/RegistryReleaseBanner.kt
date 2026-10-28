package com.nikgapps.app.presentation.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nikgapps.app.data.ReleaseNoticeRepository
import com.nikgapps.app.data.BuildProject
import com.nikgapps.app.registry.*
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RegistryReleaseBanner(online: Boolean, projects: List<BuildProject>, updates: Map<String, ProjectUpdate>) {
    val context = LocalContext.current
    val preferences = remember { ReleaseNoticeRepository(context) }
    var release by remember { mutableStateOf<ReleaseSummary?>(null) }
    var dismissTarget by remember { mutableStateOf<ReleaseSummary?>(null) }
    LaunchedEffect(online, projects, updates) {
        if (projects.isEmpty()) {
            release = null
            return@LaunchedEffect
        }
        val repository = CatalogRepository(context.cacheDir)
        fun display(metadata: RegistryMetadata) {
            val latest = metadata.releaseIndex?.latest?.values?.flatMap { it.values.flatMap { architectures -> architectures.values } }.orEmpty().toSet()
            metadata.releaseIndex?.releases?.filter { it.id in latest && releaseKnownToProjects(it, projects) }
                ?.forEach { preferences.acknowledge(it.id) }
            release = metadata.releaseIndex?.releases?.filter { it.id in latest && it.channel == "stable" &&
                releaseAvailableToProjects(it, projects, updates) }
                ?.maxByOrNull { it.createdAt }?.takeUnless { preferences.dismissed(it.id) }
        }
        repository.cached()?.let(::display)
        if (online) try {
            display(repository.load())
        } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { }
    }
    release?.takeIf { releaseAvailableToProjects(it, projects, updates) }?.let { published ->
        var expanded by rememberSaveable(published.id) { mutableStateOf(false) }
        val releaseDate = remember(published.createdAt) {
            runCatching { DateTimeFormatter.ofPattern("MMM d, yyyy 'UTC'", Locale.getDefault())
                .withZone(ZoneOffset.UTC).format(Instant.parse(published.createdAt)) }.getOrNull()
        }
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
                Row(modifier = Modifier.fillMaxWidth()
                    .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                    .clickable(role = Role.Button, onClickLabel = if (expanded) "Hide release details" else "Show release details") {
                        expanded = !expanded
                    }.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                        Icon(Icons.Outlined.SystemUpdateAlt, contentDescription = null,
                            modifier = Modifier.padding(8.dp).size(20.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("NikGapps release available", style = MaterialTheme.typography.titleSmall)
                        Text(listOfNotNull("Android ${published.androidVersion}", releaseDate).joinToString(" · "),
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Text("Updated packages and fixes are available. Press and hold a compatible project and choose Build latest ZIP to update.",
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.End) {
                        FilledTonalButton(onClick = { dismissTarget = published }, shape = CircleShape,
                            modifier = Modifier.height(40.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) { Text("Dismiss", style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
        }
    }
    dismissTarget?.let { target ->
        Dialog(onDismissRequest = { dismissTarget = null }) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                Column {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Dismiss this release announcement?", style = MaterialTheme.typography.titleLarge)
                        Text("Project update indicators will remain visible. You'll see another announcement when a newer release is available.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { dismissTarget = null },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 10.dp)) {
                            Text("Keep", style = MaterialTheme.typography.labelMedium)
                        }
                        FilledTonalButton(onClick = {
                            preferences.dismiss(target.id)
                            if (release?.id == target.id) release = null
                            dismissTarget = null
                        }, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 10.dp)) {
                            Text("Dismiss", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
