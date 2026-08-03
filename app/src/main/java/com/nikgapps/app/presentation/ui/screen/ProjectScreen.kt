package com.nikgapps.app.presentation.ui.screen

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nikgapps.app.data.*
import com.nikgapps.app.presentation.navigation.appConfigRoute
import com.nikgapps.app.presentation.navigation.buildZipRoute
import com.nikgapps.app.registry.*
import com.nikgapps.app.utils.ZipBuildProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private data class RegistryDeviceStatus(
    val installed: Boolean,
    val versionName: String? = null,
    val versionCode: Long? = null
)

private enum class PackageSort(val label: String) {
    NAME("Name"),
    INSTALLED("Install status")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(projectId: String, autoBuild: Boolean = false, navController: NavHostController) {
    val context = LocalActivity.current ?: return
    val repository = remember { BuildProjectRepository(context) }
    var project by remember(projectId) { mutableStateOf(repository.getProjects().firstOrNull { it.id == projectId }) }
    var metadata by remember { mutableStateOf<RegistryMetadata?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf<ZipBuildProgress?>(null) }
    var result by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var packageSort by rememberSaveable(projectId) { mutableStateOf(PackageSort.NAME) }
    var sortDescending by rememberSaveable(projectId) { mutableStateOf(false) }
    var installedOnly by rememberSaveable(projectId) { mutableStateOf(false) }
    var summaryExpanded by rememberSaveable(projectId) { mutableStateOf(false) }
    var autoBuildConsumed by rememberSaveable(projectId, autoBuild) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val catalogRepository = remember { CatalogRepository(context.cacheDir) }

    LaunchedEffect(Unit) {
        try { metadata = catalogRepository.load() }
        catch (e: Exception) { loadError = e.message ?: "Unable to load the NikGapps catalog" }
    }
    val current = project
    if (current == null) { Text("Project not found", Modifier.padding(24.dp)); return }
    val registry = metadata
    val selectedAppSet = registry?.appSets?.appSets?.firstOrNull { it.id == current.selectedAppSetId }
        ?: registry?.appSets?.appSets?.firstOrNull()
    val packageAppSets = registry?.let { loaded ->
        loaded.appSets.appSets.flatMap { appSet ->
            loaded.catalog.publicPackages(appSet).map { pkg -> pkg.id to appSet }
        }.groupBy({ it.first }, { it.second })
    }.orEmpty()
    val displayedPackages = registry?.catalog?.packages?.filter { it.id in packageAppSets }.orEmpty()

    LaunchedEffect(registry, selectedAppSet?.id) {
        if (registry != null && selectedAppSet != null) {
            val packageOwners = current.selectedPackageAppSets.toMutableMap()
            current.selectedAppIds.forEach { id ->
                val validOwners = packageAppSets[id].orEmpty()
                if (packageOwners[id] !in validOwners.map { it.id }) {
                    packageOwners[id] = validOwners.firstOrNull()?.id ?: selectedAppSet.id
                }
            }
            val normalized = current.copy(selectedAppSetId = selectedAppSet.id, selectedPackageAppSets = packageOwners)
            if (normalized != current) { repository.updateProject(normalized); project = normalized }
        }
    }
    val catalogPackages = registry?.catalog?.packages.orEmpty()
    val deviceStatuses = remember(catalogPackages) {
        catalogPackages.associate { pkg ->
            val info = pkg.versions.values.asSequence().mapNotNull { version ->
                version.packageName?.let { packageName -> runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) context.packageManager.getPackageInfo(
                        packageName, PackageManager.PackageInfoFlags.of(0))
                    else @Suppress("DEPRECATION") context.packageManager.getPackageInfo(packageName, 0)
                }.getOrNull() }
            }.firstOrNull()
            pkg.id to if (info == null) RegistryDeviceStatus(false) else RegistryDeviceStatus(
                installed = true,
                versionName = info.versionName,
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
                    else @Suppress("DEPRECATION") info.versionCode.toLong()
            )
        }
    }
    val sortedPackages = remember(displayedPackages, deviceStatuses, packageSort, sortDescending, installedOnly) {
        val filtered = displayedPackages.filter { !installedOnly || deviceStatuses[it.id]?.installed == true }
        val sorted = when (packageSort) {
            PackageSort.NAME -> filtered.sortedBy { it.name.lowercase() }
            PackageSort.INSTALLED -> filtered.sortedWith(
                compareByDescending<CatalogPackage> { deviceStatuses[it.id]?.installed == true }
                    .thenBy { it.name.lowercase() }
            )
        }
        if (sortDescending) sorted.reversed() else sorted
    }

    fun save(value: BuildProject) { repository.updateProject(value); project = value }
    fun startBuild() {
        val loaded = metadata ?: return
        val appSet = selectedAppSet ?: return
        scope.launch {
            try {
                progress = ZipBuildProgress(0, current.selectedAppIds.size, "Resolving package versions…")
                val defaultChannel = ReleaseChannel.valueOf(current.defaultChannel.uppercase())
                val overrides = current.channelOverrides.mapValues { ReleaseChannel.valueOf(it.value.uppercase()) }
                val selections = current.selectedAppIds.associateWith { id ->
                    current.selectedPackageAppSets[id] ?: loaded.appSets.appSets.firstOrNull { id in it.packages }?.id
                    ?: throw IllegalArgumentException("No AppSet owns selected package '$id'")
                }
                val resolution = withContext(Dispatchers.IO) { CatalogResolver(loaded.catalog, loaded.appSets).resolveAcrossAppSets(
                    selections, defaultChannel, overrides, current.androidVersion.apiLevel, current.architecture.value) }
                val resolved = resolution.packages
                val visibleTotal = resolved.count { !it.hidden }
                val downloader = ArtifactDownloader(context.cacheDir)
                val validator = PackageZipValidator()
                val deviceFactory = DeviceArtifactFactory(context)
                val deviceDir = File(context.cacheDir, "nikgapps/device-packages").apply { mkdirs() }
                val artifacts = mutableListOf<ValidatedArtifact>()
                var completedVisible = 0
                resolved.forEach { pkg ->
                    val source = current.appSources[pkg.catalogPackage.id]?.source ?: AppSource.GITLAB
                    val operation = when {
                        pkg.hidden -> "Downloading required dependency ${pkg.catalogPackage.name}…"
                        source == AppSource.DEVICE -> "Reading ${pkg.catalogPackage.name} from this device…"
                        else -> "Downloading ${pkg.catalogPackage.name} from GitLab…"
                    }
                    progress = ZipBuildProgress(completedVisible, visibleTotal, operation)
                    val artifact = if (!pkg.hidden && source == AppSource.DEVICE) {
                        withContext(Dispatchers.IO) { deviceFactory.create(pkg, deviceDir) }
                    } else {
                        val file = downloader.obtain(pkg) { download -> withContext(Dispatchers.Main) {
                            val percent = download.total?.takeIf { it > 0 }?.let { download.downloaded * 100 / it }
                            progress = ZipBuildProgress(completedVisible, visibleTotal,
                                "${if (pkg.hidden) "Downloading required dependency" else "Downloading from GitLab"}: ${pkg.catalogPackage.name}${percent?.let { " ($it%)" }.orEmpty()}")
                        } }
                        withContext(Dispatchers.IO) { ValidatedArtifact(pkg, file, validator.validate(file, pkg)) }
                    }
                    artifacts += artifact
                    if (!pkg.hidden) completedVisible++
                }
                progress = ZipBuildProgress(visibleTotal, visibleTotal, "Assembling the flashable ZIP…")
                val output = withContext(Dispatchers.IO) {
                    RegistryZipAssembler(AndroidBuilderAssetSource(context)).build(
                        File(context.cacheDir, "zip-builds"), BuildRequest(current.androidVersion.displayName,
                            current.androidVersion.apiLevel, current.architecture.value, appSet, defaultChannel,
                            overrides, current.selectedAppIds, packageAppSets = resolution.packageAppSets), artifacts)
                }
                val published = withContext(Dispatchers.IO) { ZipPublisher(context).publish(output) }
                output.delete()
                progress = null; result = false to "Flashable ZIP created:\n$published"
            } catch (e: Exception) { progress = null; result = true to (e.message ?: "Build failed") }
        }
    }

    LaunchedEffect(autoBuild, metadata) {
        if (autoBuild && !autoBuildConsumed && metadata != null) {
            autoBuildConsumed = true
            startBuild()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(current.name) }, navigationIcon = {
        IconButton(onClick = navController::navigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
    }) }, bottomBar = { Surface(tonalElevation = 3.dp) { Button(onClick = { navController.navigate(buildZipRoute(projectId)) },
        enabled = metadata != null && current.selectedAppIds.isNotEmpty(), modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(20.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
        Icon(Icons.Default.Inventory2, null); Spacer(Modifier.width(8.dp))
        Text("Build flashable ZIP · ${current.selectedAppIds.size} apps")
    } } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Build your app set", style = MaterialTheme.typography.headlineMedium)
                Text("Pick any supported package, its source, and the AppSet used for shared apps.",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth().clickable { summaryExpanded = !summaryExpanded }) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${current.selectedAppIds.size} apps selected", style = MaterialTheme.typography.titleMedium)
                                Text("Tap to ${if (summaryExpanded) "hide" else "review"} your selection", style = MaterialTheme.typography.labelMedium)
                            }
                            Icon(if (summaryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                        }
                        AnimatedVisibility(summaryExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .18f))
                                if (current.selectedAppIds.isEmpty()) Text("No apps selected yet", style = MaterialTheme.typography.bodyMedium)
                                displayedPackages.filter { it.id in current.selectedAppIds }.forEach { selectedPkg ->
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Android, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(selectedPkg.name, style = MaterialTheme.typography.bodyMedium)
                                            Text(selectedPkg.versions.values.firstNotNullOfOrNull { it.packageName } ?: selectedPkg.id,
                                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .72f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Sort, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sort and filter", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                            IconButton(onClick = { sortDescending = !sortDescending }) {
                                Icon(if (sortDescending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    if (sortDescending) "Descending" else "Ascending")
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PackageSort.entries.forEach { option ->
                                FilterChip(selected = packageSort == option, onClick = { packageSort = option },
                                    label = { Text(option.label) },
                                    leadingIcon = if (packageSort == option) {{
                                        Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                                    }} else null)
                            }
                            FilterChip(selected = installedOnly, onClick = { installedOnly = !installedOnly },
                                label = { Text("Installed only") }, leadingIcon = { Icon(Icons.Default.PhoneAndroid, null, Modifier.size(18.dp)) })
                        }
                    }
                }
                loadError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
                if (metadata == null && loadError == null) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
            }
            sortedPackages.forEach { pkg ->
                item(key = pkg.id) {
                    val owners = packageAppSets[pkg.id].orEmpty()
                    val owner = owners.firstOrNull { it.id == current.selectedPackageAppSets[pkg.id] }
                        ?: owners.firstOrNull()
                    val source = current.appSources[pkg.id]?.source ?: AppSource.GITLAB
                    val deviceStatus = deviceStatuses[pkg.id] ?: RegistryDeviceStatus(false)
                    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        ProjectPackageRow(pkg, source, deviceStatus, pkg.id in current.selectedAppIds,
                            onOpen = { navController.navigate(appConfigRoute(projectId, pkg.id)) },
                            onSelected = { enabled -> save(current.copy(
                                selectedAppSetId = owner?.id ?: current.selectedAppSetId,
                                selectedAppIds = if (enabled) current.selectedAppIds + pkg.id else current.selectedAppIds - pkg.id,
                                selectedPackageAppSets = if (enabled && owner != null) current.selectedPackageAppSets + (pkg.id to owner.id)
                                    else current.selectedPackageAppSets - pkg.id)) })
                    }
                }
            }
        }
    }
    progress?.let { p -> AlertDialog({}, title = { Text("Building flashable ZIP") }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LinearProgressIndicator(progress = { p.fraction }, modifier = Modifier.fillMaxWidth()); Text(p.message); Text("${p.completed} of ${p.total} packages")
    } }, confirmButton = {}) }
    result?.let { (failed, message) -> AlertDialog({ result = null }, title = { Text(if (failed) "Build failed" else "Build complete") },
        text = { Text(message) }, confirmButton = { TextButton({ result = null }) { Text("OK") } }) }
}

@Composable
private fun RegistryAppRow(pkg: CatalogPackage, source: AppSource, device: RegistryDeviceStatus, selected: Boolean,
    channel: String, memberAppSets: List<CatalogAppSet>, selectedAppSetId: String?, expanded: Boolean,
    onExpand: () -> Unit,
    onSelected: (Boolean) -> Unit, onSource: (AppSource) -> Unit, onAppSet: (CatalogAppSet) -> Unit,
    onChannel: (ReleaseChannel) -> Unit) {
    val enabled = source == AppSource.GITLAB || device.installed
    val catalogVersion = pkg.channels[channel]?.let(pkg.versions::get) ?: pkg.versions.values.firstOrNull()
    val deviceIsNewer = device.versionCode != null && catalogVersion != null && device.versionCode > catalogVersion.versionCode
    val availableChannels = ReleaseChannel.entries.filter { it.wireName in pkg.channels }
    val selectedChannel = availableChannels.firstOrNull { it.wireName == channel } ?: availableChannels.firstOrNull()
    val nextChannel = selectedChannel?.takeIf { availableChannels.size > 1 }
        ?.let { availableChannels[(availableChannels.indexOf(it) + 1) % availableChannels.size] }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onExpand), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(48.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Android, null) } }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(pkg.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(catalogVersion?.packageName ?: pkg.id, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Checkbox(selected, onSelected, enabled = enabled)
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                if (expanded) "Collapse ${pkg.name}" else "Configure ${pkg.name}")
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                catalogVersion?.let { version ->
                    val payloadSize = version.install?.payloadSize
                        ?: version.files.sumOf { it.size }
                    val fileSummary = version.files.groupingBy { it.type }.eachCount().entries
                        .sortedBy { it.key }.joinToString(" · ") { "${it.value} ${it.key}" }
                    Text("Package details", style = MaterialTheme.typography.labelLarge)
                    Text(buildString {
                        append("Version ${version.versionName} (${version.versionCode})")
                        append(" · API ${version.android.minApi ?: "any"}–${version.android.maxApi ?: "current"}")
                        append(" · ${payloadSize / 1_048_576.0f} MiB")
                    }, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (fileSummary.isNotBlank()) Text(fileSummary, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Installs to ${version.defaultPartition}; ${version.files.size} verified files",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Text("Package source", style = MaterialTheme.typography.labelLarge)
                Text("Select which version should be placed in the flashable ZIP.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SourceTile(title = "GitLab", version = catalogVersion?.versionName, icon = Icons.Default.CloudDownload,
                        selected = source == AppSource.GITLAB, enabled = true, modifier = Modifier.weight(1f),
                        supportingText = if (deviceIsNewer && source == AppSource.GITLAB) "Newer version on device" else null) {
                        onSource(AppSource.GITLAB)
                    }
                    SourceTile(title = "Device", version = device.versionName, icon = Icons.Default.PhoneAndroid,
                        selected = source == AppSource.DEVICE, enabled = device.installed, modifier = Modifier.weight(1f),
                        supportingText = if (device.installed) null else "Not installed") {
                        onSource(AppSource.DEVICE)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("AppSet", style = MaterialTheme.typography.labelLarge)
                        Text("Highlighted AppSet owns this package", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            memberAppSets.forEach { appSet ->
                                FilterChip(selected = appSet.id == selectedAppSetId,
                                    onClick = { onAppSet(appSet) }, label = { Text(appSet.name) },
                                    leadingIcon = if (appSet.id == selectedAppSetId) {{
                                        Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                                    }} else null)
                            }
                        }
                    }
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Release channel", style = MaterialTheme.typography.labelLarge)
                        FilledTonalButton(onClick = { nextChannel?.let(onChannel) }, enabled = nextChannel != null,
                            modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp)) {
                            Icon(Icons.Default.Sync, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(selectedChannel?.wireName?.replaceFirstChar { it.uppercase() } ?: "Unavailable",
                                maxLines = 1)
                        }
                        if (availableChannels.size > 1) Text("Tap to switch channel", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceTile(title: String, version: String?, icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean, enabled: Boolean, modifier: Modifier = Modifier, supportingText: String? = null,
    onClick: () -> Unit) {
    Card(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 112.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(20.dp))
                Spacer(Modifier.weight(1f))
                if (selected) Icon(Icons.Default.CheckCircle, "Selected source", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(version?.let { "v$it" } ?: "Version unavailable", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            supportingText?.let { Text(it, style = MaterialTheme.typography.labelSmall,
                color = if (enabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun ProjectPackageRow(pkg: CatalogPackage, source: AppSource, device: RegistryDeviceStatus,
    selected: Boolean, onOpen: () -> Unit, onSelected: (Boolean) -> Unit) {
    val version = pkg.versions.values.firstOrNull()
    Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(48.dp), shape = RoundedCornerShape(16.dp),
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Android, null) }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(pkg.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(version?.packageName ?: pkg.id, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${source.displayName}${if (device.installed) " · Installed" else ""}",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Checkbox(selected, onSelected, enabled = source == AppSource.GITLAB || device.installed)
        Icon(Icons.Default.ChevronRight, "Configure ${pkg.name}")
    }
}
