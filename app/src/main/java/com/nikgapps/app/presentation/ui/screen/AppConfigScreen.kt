package com.nikgapps.app.presentation.ui.screen

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nikgapps.app.data.*
import com.nikgapps.app.registry.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigScreen(projectId: String, packageId: String, navController: NavHostController) {
    val context = LocalActivity.current ?: return
    val repository = remember { BuildProjectRepository(context) }
    val project = remember(projectId) { repository.getProjects().firstOrNull { it.id == projectId } }
    var metadata by remember { mutableStateOf<RegistryMetadata?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val catalogRepository = remember { CatalogRepository(context.cacheDir) }

    LaunchedEffect(project?.androidVersion, project?.architecture, project?.defaultChannel) {
        val selected = project ?: return@LaunchedEffect
        try { metadata = catalogRepository.load(catalogAndroidVersion(selected.androidVersion.displayName),
            selected.defaultChannel, selected.architecture.value) }
        catch (e: Exception) { error = e.message ?: "Unable to load package information" }
    }
    val current = project
    val outdated = current != null && metadata?.let { unavailableProjectPackages(current, it).isNotEmpty() } == true
    val pkg = metadata?.catalog?.packages?.firstOrNull { it.id == packageId }
    val channel = runCatching { ReleaseChannel.valueOf(
        (current?.channelOverrides?.get(packageId) ?: current?.defaultChannel ?: "stable").uppercase())
    }.getOrDefault(ReleaseChannel.STABLE)
    val version = metadata?.release?.packages?.get(packageId)?.let { pkg?.versions?.get(it) }
        ?: pkg?.channels?.get(channel.wireName)?.let { pkg.versions[it] }
        ?: pkg?.versions?.values?.firstOrNull()
    val flashSize = remember(metadata, current, packageId) {
        runCatching {
            val loaded = metadata ?: return@runCatching null
            val selected = current ?: return@runCatching null
            val owner = selected.selectedPackageAppSets[packageId]
                ?: loaded.appSets.appSets.firstOrNull { packageId in it.packages }?.id
                ?: return@runCatching null
            estimateInstallSize(CatalogResolver(loaded.catalog, loaded.appSets, loaded.release).resolve(
                owner, setOf(packageId), channel,
                selected.channelOverrides.mapValues { ReleaseChannel.valueOf(it.value.uppercase()) },
                selected.androidVersion.apiLevel, selected.architecture.value))
        }.getOrNull()
    }

    Scaffold(topBar = { TopAppBar(title = { Text(pkg?.name ?: "Package") }, navigationIcon = {
        IconButton(onClick = navController::navigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
    }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(48.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Box(contentAlignment = Alignment.Center) {
                                com.nikgapps.app.presentation.ui.component.PackageIcon(version?.packageName, Modifier.size(40.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(pkg?.name ?: "Loading…", style = MaterialTheme.typography.titleLarge,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("Package details", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            if (outdated) item { Text("This project is outdated and read only. Duplicate it to edit a supported copy.",
                color = MaterialTheme.colorScheme.error) }
            if (metadata == null && error == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (pkg != null && current != null) {
                item {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                        AnimatedContent(version, label = "package-version") { shown ->
                            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
                                InfoRow("Package name", shown?.packageName ?: "Not specified")
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
                                InfoRow("Version", shown?.versionName ?: "Unavailable")
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
                                InfoRow("Version code", shown?.versionCode?.toString() ?: "Unavailable")
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
                                InfoRow("Android", listOfNotNull(shown?.android?.minApi?.let { "API $it+" }, shown?.android?.maxApi?.let { "up to $it" }).joinToString(" · ").ifBlank { "Any supported API" })
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
                                InfoRow("Architecture", shown?.architectures?.joinToString().orEmpty().ifBlank { "Universal" })
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
                                InfoRow("Installed payload", shown?.installBytes()?.let(::formatInstallSize) ?: "Unavailable")
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
                                InfoRow("Approx. flash payload · including dependencies",
                                    flashSize?.takeIf { it.complete }?.let { "≈ ${formatInstallSize(it.knownBytes)}" } ?: "Unavailable")
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
                                InfoRow("Install partition", shown?.defaultPartition ?: "Unavailable")
                                Text("Uncompressed files written during flashing. Temporary space and filesystem overhead are additional.",
                                    Modifier.padding(vertical = 10.dp), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

            }
        }
    }
}

@Composable private fun InfoRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.foundation.text.selection.SelectionContainer {
            Text(value, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
